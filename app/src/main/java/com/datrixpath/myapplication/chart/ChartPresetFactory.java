package com.datrixpath.myapplication.chart;

import com.datrixpath.myapplication.model.ChartUIModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChartPresetFactory — 圖表預設清單（Android 端統一管理）
 *
 * ╔══════════════════════════════════════════════════════════════╗
 * ║ 工程師新增圖表的唯一入口： ║
 * ║ 1. 在此檔案下方新增一個 static 方法 build[Name]() ║
 * ║ 2. 在 buildAll() 的 list.add(...) 清單加入呼叫 ║
 * ║ 3. 若需要 JS 端自訂格式（formatter / 動畫等）， ║
 * ║ 在 echarts_factory.html 的「工程師擴充區」登記 Preset ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * JS 端對應的 Preset 系統說明：
 * - 內建類型（bar / line / pie / scatter）：直接填 .type() 即可
 * - 自訂類型（bar-normalized / gauge-ring）：
 * → 在 echarts_factory.html 的 PresetRegistry.register() 區塊登記
 * → Android 端同樣使用 .type("bar-normalized") 呼叫
 */
public class ChartPresetFactory {

    // ─────────────────────────────────────────────────────────────
    // 公開方法：回傳所有要顯示的圖表清單
    // ─────────────────────────────────────────────────────────────

    /**
     * 建立所有圖表清單。
     * 新增圖表時，在此方法的 list.add() 清單加入對應的 build 方法呼叫。
     */
    public static List<ChartUIModel> buildAll() {
        List<ChartUIModel> list = new ArrayList<>();

        // ── 格式 A: 氣溫折線圖 ────────────────────────
        list.add(buildWeatherLine());

        // ── 格式 B: 月營收長條圖 ──────────────────────
        list.add(buildRevenueBar());

        // ── 格式 C: 裝置市場佔比圓餅圖 ───────────────
        list.add(buildMarketSharePie());

        // ── 格式 D: 散點分佈圖 ────────────────────────
        list.add(buildScatter());

        // ── 格式 E: 多系列折線（iOS vs Android） ─────
        list.add(buildMultiSeriesLine());

        // ── 格式 F: 水平長條圖（GDP 排行） ───────────
        list.add(buildHorizontalBar());

        // ── 格式 G: 堆疊長條圖（三通路月營收） ───────
        list.add(buildStackedBar());

        // ── 格式 H: 堆疊面積折線圖（三區域銷售） ─────
        list.add(buildStackedLine());

        // ── 格式 I: 100% 堆疊（Normalized）長條圖 ────
        // 類型 "bar-normalized" 由 JS Preset 處理，Android 端零額外邏輯
        list.add(buildNormalizedBar());

        // ── 格式 J: 環形儀表盤（達成率） ─────────────
        // 類型 "gauge-ring" 由 JS Preset 處理
        list.add(buildGaugeRing());

        // ╔══════════════════════════════════════════════╗
        // ║ >>> 在此加入新圖表 <<< ║
        // ║ list.add(buildYourNewChart()); ║
        // ╚══════════════════════════════════════════════╝

        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 A — 氣溫折線圖（type: line）
    // 資料格式：{ "time": "08:00", "temp": 22.5 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildWeatherLine() {
        List<Map<String, Object>> data = Arrays.asList(
                row("time", "08:00", "temp", 18.2),
                row("time", "10:00", "temp", 22.5),
                row("time", "12:00", "temp", 26.1),
                row("time", "14:00", "temp", 28.4),
                row("time", "16:00", "temp", 25.7),
                row("time", "18:00", "temp", 21.3),
                row("time", "20:00", "temp", 17.9));
        Map<String, String> encode = encode("x", "time", "y", "temp");
        return new ChartUIModel.Builder("weather_chart", data)
                .title("氣溫趨勢 (格式 A)")
                .type("line")
                .encode(encode)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 B — 月營收長條圖（type: bar）
    // 資料格式：{ "date": "Jan", "revenue": 8500 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildRevenueBar() {
        List<Map<String, Object>> data = Arrays.asList(
                row("date", "Jan", "revenue", 8500),
                row("date", "Feb", "revenue", 9200),
                row("date", "Mar", "revenue", 11400),
                row("date", "Apr", "revenue", 10100),
                row("date", "May", "revenue", 13300),
                row("date", "Jun", "revenue", 15600));
        Map<String, String> encode = encode("x", "date", "y", "revenue");
        Map<String, Object> opts = options("color", Arrays.asList("#22d3ee", "#6366f1"));
        return new ChartUIModel.Builder("revenue_chart", data)
                .title("月營收報告 (格式 B)")
                .type("bar")
                .encode(encode)
                .options(opts)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 C — 裝置市場佔比（type: pie）
    // 資料格式：{ "category": "Mobile", "share": 43 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildMarketSharePie() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] names = { "Mobile", "Desktop", "Tablet", "Smart TV", "Other" };
        int[] shares = { 43, 31, 12, 9, 5 };
        for (int i = 0; i < names.length; i++)
            data.add(row("category", names[i], "share", shares[i]));

        // 圓餅圖的 encode 欄位名稱固定為 itemName / value
        Map<String, String> encode = encode("itemName", "category", "value", "share");
        return new ChartUIModel.Builder("market_pie_chart", data)
                .title("裝置市場佔比 (格式 C)")
                .type("pie")
                .encode(encode)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 D — 散點分佈（type: scatter）
    // 資料格式：{ "x_val": 3.2, "y_val": 7.8 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildScatter() {
        List<Map<String, Object>> data = new ArrayList<>();
        double[][] points = { { 1.2, 4.5 }, { 2.3, 6.1 }, { 3.1, 3.2 }, { 4.8, 7.4 }, { 2.9, 5.5 }, { 5.3, 8.2 },
                { 1.8, 2.9 } };
        for (double[] p : points)
            data.add(row("x_val", p[0], "y_val", p[1]));

        Map<String, String> encode = encode("x", "x_val", "y", "y_val");
        return new ChartUIModel.Builder("scatter_chart", data)
                .title("散點分布 (格式 D)")
                .type("scatter")
                .encode(encode)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 E — iOS vs Android 多系列折線
    // 資料格式：{ "month": "Jan", "ios": 120, "android": 95 }
    // （多系列需透過 options.series 明確指定各系列的 encode）
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildMultiSeriesLine() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun" };
        int[] ios = { 120, 135, 110, 148, 162, 175 };
        int[] android = { 95, 118, 99, 130, 155, 168 };
        for (int i = 0; i < months.length; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("month", months[i]);
            row.put("ios", ios[i]);
            row.put("android", android[i]);
            data.add(row);
        }
        Map<String, String> encode = encode("x", "month");

        Map<String, Object> opts = new HashMap<>();
        opts.put("series", Arrays.asList(
                seriesCfg("line", "iOS", encode("x", "month", "y", "ios")),
                seriesCfg("line", "Android", encode("x", "month", "y", "android"))));

        return new ChartUIModel.Builder("app_download_chart", data)
                .title("App 下載量 iOS vs Android (格式 E)")
                .type("line")
                .dimensions(Arrays.asList("month", "ios", "android"))
                .encode(encode)
                .options(opts)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 F — 水平長條圖（encode x/y 對調 + options 翻轉軸）
    // 資料格式：{ "country": "TW", "gdp": 790 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildHorizontalBar() {
        List<Map<String, Object>> data = Arrays.asList(
                row("country", "Taiwan", "gdp", 790),
                row("country", "Singapore", "gdp", 465),
                row("country", "HongKong", "gdp", 359),
                row("country", "Japan", "gdp", 4230),
                row("country", "Korea", "gdp", 1710));
        Map<String, String> encode = encode("x", "gdp", "y", "country");

        Map<String, Object> opts = new HashMap<>();
        opts.put("xAxis", options("type", "value"));
        opts.put("yAxis", options("type", "category"));

        return new ChartUIModel.Builder("gdp_bar_chart", data)
                .title("GDP 排行 (格式 F - 水平長條)")
                .type("bar")
                .encode(encode)
                .options(opts)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 G — 堆疊長條圖（扁平資料 + stackField）
    // 資料格式：{ "date": "Jan", "channel": "Online", "revenue": 5000 }
    // JS 工廠自動 pivot，無需 Android 端預處理
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildStackedBar() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun" };
        String[] channels = { "Online", "Offline", "App" };
        int[][] revenues = {
                { 5000, 6200, 7100, 6800, 8300, 9400 },
                { 3500, 3000, 3800, 4100, 3700, 4500 },
                { 1800, 2100, 2500, 2900, 3200, 3800 }
        };
        for (int c = 0; c < channels.length; c++)
            for (int m = 0; m < months.length; m++)
                data.add(row3("date", months[m], "channel", channels[c], "revenue", revenues[c][m]));

        Map<String, String> encode = encode("x", "date", "y", "revenue");
        return new ChartUIModel.Builder("stacked_bar_chart", data)
                .title("各通路月營收 (格式 G - 堆疊長條)")
                .type("bar")
                .encode(encode)
                .stackField("channel")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 H — 堆疊面積折線圖
    // 資料格式：{ "date": "Jan", "region": "North", "sales": 820 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildStackedLine() {
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun" };
        String[] regions = { "North", "Central", "South" };
        int[][] sales = {
                { 820, 932, 1100, 934, 1290, 1330 },
                { 620, 710, 890, 770, 1010, 1150 },
                { 440, 530, 680, 590, 780, 910 }
        };
        for (int r = 0; r < regions.length; r++)
            for (int m = 0; m < months.length; m++)
                data.add(row3("date", months[m], "region", regions[r], "sales", sales[r][m]));

        Map<String, String> encode = encode("x", "date", "y", "sales");
        return new ChartUIModel.Builder("stacked_line_chart", data)
                .title("各區域銷售 (格式 H - 堆疊面積線)")
                .type("line")
                .encode(encode)
                .stackField("region")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 I — 100% 堆疊長條圖（Normalized）
    // 類型名稱 "bar-normalized" → 由 JS echarts_factory.html 的
    // PresetRegistry 處理；Android 端不需要計算百分比
    // 資料格式：{ "date": "Jan", "channel": "Online", "revenue": 5000 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildNormalizedBar() {
        // 使用與格式 G 完全相同的原始數據（JS 端自動換算百分比）
        List<Map<String, Object>> data = new ArrayList<>();
        String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun" };
        String[] channels = { "Online", "Offline", "App" };
        int[][] revenues = {
                { 5000, 6200, 7100, 6800, 8300, 9400 },
                { 3500, 3000, 3800, 4100, 3700, 4500 },
                { 1800, 2100, 2500, 2900, 3200, 3800 }
        };
        for (int c = 0; c < channels.length; c++)
            for (int m = 0; m < months.length; m++)
                data.add(row3("date", months[m], "channel", channels[c], "revenue", revenues[c][m]));

        Map<String, String> encode = encode("x", "date", "y", "revenue");
        return new ChartUIModel.Builder("normalized_bar_chart", data)
                .title("各通路佔比 (格式 I - 100% 堆疊)")
                .type("bar-normalized") // ← JS Preset 負責渲染
                .encode(encode)
                .stackField("channel")
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // 格式 J — 環形儀表盤（gauge-ring）
    // 類型名稱 "gauge-ring" → 由 JS echarts_factory.html 的 Preset 處理
    // 資料格式：{ "name": "目標達成率", "value": 85 }
    // ─────────────────────────────────────────────────────────────

    private static ChartUIModel buildGaugeRing() {
        List<Map<String, Object>> data = Arrays.asList(
                row("name", "月度目標", "value", 85),
                row("name", "季度目標", "value", 63),
                row("name", "年度目標", "value", 42));
        Map<String, String> encode = encode("itemName", "name", "value", "value");
        return new ChartUIModel.Builder("gauge_ring_chart", data)
                .title("KPI 達成率 (格式 J - 環形儀表)")
                .type("gauge-ring") // ← JS Preset 負責渲染
                .encode(encode)
                .build();
    }

    // ╔══════════════════════════════════════════════════════════════╗
    // ║ >>> 在此加入新的 build 方法 <<< ║
    // ║ ║
    // ║ private static ChartUIModel buildYourChart() { ║
    // ║ List<Map<String, Object>> data = new ArrayList<>(); ║
    // ║ // ... 準備資料 ... ║
    // ║ return new ChartUIModel.Builder("your_id", data) ║
    // ║ .title("你的圖表標題") ║
    // ║ .type("bar") // 或其他類型 ║
    // ║ .encode(encode("x", "xField", "y", "yField")) ║
    // ║ .build(); ║
    // ║ } ║
    // ╚══════════════════════════════════════════════════════════════╝

    // ─────────────────────────────────────────────────────────────
    // 工具方法（私有，僅供本工廠使用）
    // ─────────────────────────────────────────────────────────────

    /** 建立 2 個欄位的 row Map */
    private static Map<String, Object> row(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    /** 建立 3 個欄位的 row Map（堆疊圖使用） */
    private static Map<String, Object> row3(String k1, Object v1,
            String k2, Object v2,
            String k3, Object v3) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        m.put(k3, v3);
        return m;
    }

    /** 快速建立 encode Map（支援 2 或 4 個參數） */
    private static Map<String, String> encode(String... pairs) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2)
            m.put(pairs[i], pairs[i + 1]);
        return m;
    }

    /** 快速建立 options Map（支援 2 或 4 個參數） */
    private static Map<String, Object> options(String k1, Object v1) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        return m;
    }

    /** 多系列 series 配置輔助 */
    private static Map<String, Object> seriesCfg(String type, String name, Map<String, String> encode) {
        Map<String, Object> s = new HashMap<>();
        s.put("type", type);
        s.put("name", name);
        s.put("encode", encode);
        return s;
    }
}
