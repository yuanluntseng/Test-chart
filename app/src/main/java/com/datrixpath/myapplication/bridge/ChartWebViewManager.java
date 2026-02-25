package com.datrixpath.myapplication.bridge;

import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.datrixpath.myapplication.model.ChartUIModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * ChartWebViewManager — WebView 封裝管理器 (JS Bridge)
 *
 * 職責：
 * 1. 初始化 WebView 並設定所有必要的 WebSettings
 * 2. 載入 assets/echarts_factory.html
 * 3. 在頁面就緒後，將 ChartUIModel 序列化並透過 evaluateJavascript 傳入 JS
 * 4. 提供 Android Interface（@JavascriptInterface）供 JS 主動回呼 Android
 *
 * 使用方式（在 Activity / Fragment 中）：
 * 
 * <pre>
 * ChartWebViewManager manager = new ChartWebViewManager(webView, this);
 * manager.init();
 * // 等待 LiveData 有資料後：
 * manager.renderCharts(chartList);
 * </pre>
 */
public class ChartWebViewManager {

    private final WebView webView;
    private final Callback callback;
    private boolean pageReady = false;

    /** 與 View 層的通訊介面 */
    public interface Callback {
        /** HTML 頁面完全載入完成，可以開始注入圖表數據 */
        void onPageReady();

        /** JS 端回報錯誤 */
        void onError(String message);
    }

    public ChartWebViewManager(WebView webView, Callback callback) {
        this.webView = webView;
        this.callback = callback;
    }

    // ─────────────────────────────────────────────────────────────
    // 初始化 WebView
    // ─────────────────────────────────────────────────────────────

    @SuppressLint({ "SetJavaScriptEnabled", "AddJavascriptInterface" })
    public void init() {
        WebSettings settings = webView.getSettings();

        // 核心設定
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // 允許從 file:// 頁面存取網路資源（載入 ECharts CDN）
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 效能優化
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // 注入 Android 接口（JS 可透過 window.Android.xxx() 呼叫）
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        // 監聽頁面載入完成
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageReady = true;
                if (callback != null)
                    callback.onPageReady();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 攔截站外跳轉，一律由 WebView 內部處理
                return false;
            }
        });

        // 載入工廠 HTML
        webView.loadUrl("file:///android_asset/echarts_factory.html");
    }

    // ─────────────────────────────────────────────────────────────
    // 渲染圖表（對每個 ChartUIModel 呼叫一次 JS renderChart）
    // ─────────────────────────────────────────────────────────────

    public void renderCharts(List<ChartUIModel> charts) {
        if (!pageReady)
            return;
        for (ChartUIModel model : charts) {
            renderSingleChart(model);
        }
    }

    public void renderSingleChart(ChartUIModel model) {
        if (!pageReady)
            return;

        try {
            String dataJson = sourceToJson(model.getSource());
            String configJson = configToJson(model);
            String js = "renderChart("
                    + "'" + escapeForJs(model.getId()) + "',"
                    + "'" + escapeForJs(dataJson) + "',"
                    + "'" + escapeForJs(configJson) + "'"
                    + ");";
            webView.evaluateJavascript(js, null);
        } catch (JSONException e) {
            if (callback != null)
                callback.onError("序列化失敗: " + e.getMessage());
        }
    }

    /** 移除指定圖表 */
    public void removeChart(String chartId) {
        if (!pageReady)
            return;
        webView.evaluateJavascript("removeChart('" + chartId + "');", null);
    }

    /** 清除所有圖表 */
    public void clearAll() {
        if (!pageReady)
            return;
        webView.evaluateJavascript("clearAllCharts();", null);
    }

    // ─────────────────────────────────────────────────────────────
    // 序列化工具
    // ─────────────────────────────────────────────────────────────

    /**
     * 將 List<Map> 轉為 JSON 陣列字串（ECharts dataset.source 格式）
     */
    private String sourceToJson(List<Map<String, Object>> source) throws JSONException {
        JSONArray arr = new JSONArray();
        for (Map<String, Object> row : source) {
            arr.put(new JSONObject(row));
        }
        return arr.toString();
    }

    /**
     * 將 ChartUIModel 的配置部分轉為 configJson
     * 結構：{ type, title, encode, dimensions, stackField, options }
     */
    private String configToJson(ChartUIModel model) throws JSONException {
        JSONObject cfg = new JSONObject();
        cfg.put("type", model.getType());
        cfg.put("title", model.getTitle() != null ? model.getTitle() : "");

        // encode
        JSONObject encodeObj = new JSONObject();
        if (model.getEncode() != null) {
            for (Map.Entry<String, String> entry : model.getEncode().entrySet()) {
                encodeObj.put(entry.getKey(), entry.getValue());
            }
        }
        cfg.put("encode", encodeObj);

        // dimensions（可選）
        if (model.getDimensions() != null && !model.getDimensions().isEmpty()) {
            JSONArray dims = new JSONArray();
            for (String d : model.getDimensions())
                dims.put(d);
            cfg.put("dimensions", dims);
        }

        // stackField（★ 堆疊分組欄位，可選）
        if (model.getStackField() != null && !model.getStackField().isEmpty()) {
            cfg.put("stackField", model.getStackField());
        }

        // options（可選的個性化覆寫）
        if (model.getOptions() != null) {
            cfg.put("options", new JSONObject(model.getOptions()));
        }

        return cfg.toString();
    }

    /**
     * 轉義 JSON 字串中可能破壞 JS 呼叫的字元
     */
    private String escapeForJs(String json) {
        return json
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    // ─────────────────────────────────────────────────────────────
    // Android Bridge（JS → Android 回呼）
    // ─────────────────────────────────────────────────────────────

    private class AndroidBridge {
        /**
         * JS 端在 ECharts 就緒後呼叫此方法
         * 呼叫方式：window.Android.onPageReady('echarts_factory')
         */
        @JavascriptInterface
        public void onPageReady(String pageName) {
            if (callback != null && webView != null) {
                webView.post(() -> callback.onPageReady());
            }
        }

        /**
         * JS 端發生錯誤時回報
         * 呼叫方式：window.Android.onError('message')
         */
        @JavascriptInterface
        public void onError(String message) {
            if (callback != null && webView != null) {
                webView.post(() -> callback.onError(message));
            }
        }
    }
}
