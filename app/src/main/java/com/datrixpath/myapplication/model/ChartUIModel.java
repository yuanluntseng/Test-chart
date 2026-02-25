package com.datrixpath.myapplication.model;

import java.util.List;
import java.util.Map;

/**
 * ChartUIModel — 圖表指令包 (Data Contract)
 *
 * 這是 ViewModel → View → JS Bridge 的標準數據格式。
 * 每一個 ChartUIModel 對應 JS 端的一個 renderChart() 呼叫。
 *
 * 欄位說明：
 * id -> 圖表唯一識別碼，對應 JS 的 containerId
 * title -> 圖表標題（顯示在卡片頂部）
 * type -> ECharts series 類型：line / bar / pie / scatter / ...
 * source -> API 原始數組（ECharts dataset.source）
 * dimensions -> 顯式維度定義（可選，不設則由 ECharts 自動推斷）
 * encode -> 欄位映射規則（解決 10 種不同 API 格式的核心）
 * 例：{"x": "date", "y": "sales"}
 * stackField -> ★ 堆疊分組欄位（可選）。設定後 JS 端自動 pivot 並堆疊系列。
 * 例：stackField="channel" → 自動依 channel 值拆成多系列
 * options -> 個性化覆寫配置（顏色、標籤等），可為 null
 */
public class ChartUIModel {

    private final String id;
    private final String title;
    private final String type;
    private final List<Map<String, Object>> source;
    private final List<String> dimensions;
    private final Map<String, String> encode;
    private final Map<String, Object> options;
    private final String stackField; // ★ nullable — null 表示不堆疊

    private ChartUIModel(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.type = builder.type;
        this.source = builder.source;
        this.dimensions = builder.dimensions;
        this.encode = builder.encode;
        this.options = builder.options;
        this.stackField = builder.stackField;
    }

    // ── Getters ──────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public List<Map<String, Object>> getSource() {
        return source;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public Map<String, String> getEncode() {
        return encode;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public String getStackField() {
        return stackField;
    }

    // ── Builder ──────────────────────────────────────────────────

    public static class Builder {
        private String id;
        private String title = "";
        private String type = "bar";
        private List<Map<String, Object>> source;
        private List<String> dimensions = null;
        private Map<String, String> encode;
        private Map<String, Object> options = null;
        private String stackField = null; // ★ 新增

        public Builder(String id, List<Map<String, Object>> source) {
            this.id = id;
            this.source = source;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder dimensions(List<String> dims) {
            this.dimensions = dims;
            return this;
        }

        public Builder encode(Map<String, String> encode) {
            this.encode = encode;
            return this;
        }

        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        /**
         * 設定堆疊分組欄位。
         * 例：encode={x:"date", y:"revenue"}, stackField="channel"
         * → JS 自動將 channel 的每個唯一值變成獨立系列，並以 date 為 X 軸堆疊。
         */
        public Builder stackField(String stackField) {
            this.stackField = stackField;
            return this;
        }

        public ChartUIModel build() {
            if (id == null || id.isEmpty())
                throw new IllegalArgumentException("chartId cannot be empty");
            if (source == null)
                throw new IllegalArgumentException("source cannot be null");
            if (encode == null)
                throw new IllegalArgumentException("encode cannot be null");
            return new ChartUIModel(this);
        }
    }
}
