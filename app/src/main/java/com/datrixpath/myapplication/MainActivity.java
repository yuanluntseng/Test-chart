package com.datrixpath.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.datrixpath.myapplication.bridge.ChartWebViewManager;
import com.datrixpath.myapplication.viewmodel.ChartViewModel;

/**
 * MainActivity — MVVM View 層
 *
 * 職責：
 * 1. 初始化 WebView （透過 ChartWebViewManager）
 * 2. 觀察 ChartViewModel 的 LiveData
 * 3. 在頁面與數據都就緒後，透過 Bridge 注入圖表
 *
 * 流程：
 * Activity onCreate
 * → ChartWebViewManager.init() ← 載入 echarts_factory.html
 * → ChartViewModel.fetchAllCharts()
 *
 * onPageReady() ← WebViewClient.onPageFinished 觸發
 * chartList observer ← LiveData 資料就緒觸發
 * → 兩者都就緒 → renderCharts()
 */
public class MainActivity extends AppCompatActivity implements ChartWebViewManager.Callback {

        private static final String TAG = "MainActivity";

        private ChartWebViewManager webViewManager;
        private ChartViewModel viewModel;
        private ProgressBar progressBar;
        private WebView chartWebView;

        // 雙重就緒旗標（頁面 + 數據都要 ready 才渲染）
        private boolean pageReady = false;
        private boolean dataReady = false;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);

                progressBar = findViewById(R.id.progressBar);
                chartWebView = findViewById(R.id.chartWebView);

                // ① 初始化 JS Bridge & WebView
                webViewManager = new ChartWebViewManager(chartWebView, this);
                webViewManager.init();

                // ② 初始化 ViewModel
                viewModel = new ViewModelProvider(this).get(ChartViewModel.class);

                // ③ 觀察圖表數據
                viewModel.chartList.observe(this, charts -> {
                        if (charts != null && !charts.isEmpty()) {
                                dataReady = true;
                                tryRender();
                        }
                });

                // ④ 觀察載入狀態
                viewModel.loadState.observe(this, state -> {
                        if (state == ChartViewModel.LoadState.ERROR) {
                                progressBar.setVisibility(View.GONE);
                        }
                });

                // ⑤ 觀察錯誤訊息
                viewModel.errorMsg.observe(this, msg -> {
                        if (msg != null) {
                                Log.e(TAG, "ViewModel error: " + msg);
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                });

                // ⑥ 觸發數據載入
                viewModel.fetchAllCharts();
        }

        // ─────────────────────────────────────────────────────────────
        // ChartWebViewManager.Callback 實作
        // ─────────────────────────────────────────────────────────────

        @Override
        public void onPageReady() {
                pageReady = true;
                tryRender();
        }

        @Override
        public void onError(String message) {
                Log.e(TAG, "WebView error: " + message);
                Toast.makeText(this, "圖表錯誤：" + message, Toast.LENGTH_SHORT).show();
        }

        // ─────────────────────────────────────────────────────────────
        // 核心：雙重 Ready 後才渲染
        // ─────────────────────────────────────────────────────────────

        /**
         * 只有當頁面與數據都就緒時才執行渲染。
         * 由於 onPageReady / LiveData.observe 各自獨立觸發，
         * 這個方法確保兩者都到位後執行一次。
         */
        private void tryRender() {
                if (!pageReady || !dataReady)
                        return;

                // 隱藏 Loading，顯示 WebView
                progressBar.setVisibility(View.GONE);
                chartWebView.setVisibility(View.VISIBLE);

                // 注入所有圖表到 JS 工廠
                webViewManager.renderCharts(viewModel.chartList.getValue());

                Log.d(TAG, "All charts rendered successfully.");
        }

        // ─────────────────────────────────────────────────────────────
        // 生命週期
        // ─────────────────────────────────────────────────────────────

        @Override
        protected void onDestroy() {
                super.onDestroy();
                if (chartWebView != null) {
                        chartWebView.destroy();
                }
        }
}