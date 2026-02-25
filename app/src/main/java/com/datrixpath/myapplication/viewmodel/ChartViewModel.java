package com.datrixpath.myapplication.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.datrixpath.myapplication.chart.ChartPresetFactory;
import com.datrixpath.myapplication.model.ChartUIModel;

import java.util.List;

/**
 * ChartViewModel — MVVM 邏輯層（精簡版）
 *
 * 職責：
 * 1. 觸發資料載入，管理 LoadState
 * 2. 透過 LiveData 將圖表清單通知 View
 *
 * ★ 圖表的建構邏輯全部移至 ChartPresetFactory。
 * 新增圖表請直接編輯 ChartPresetFactory.java。
 */
public class ChartViewModel extends ViewModel {

    /** View 觀察此 LiveData 更新所有圖表 */
    private final MutableLiveData<List<ChartUIModel>> _chartList = new MutableLiveData<>();
    public LiveData<List<ChartUIModel>> chartList = _chartList;

    /** 載入狀態 */
    public enum LoadState {
        IDLE, LOADING, SUCCESS, ERROR
    }

    private final MutableLiveData<LoadState> _loadState = new MutableLiveData<>(LoadState.IDLE);
    public LiveData<LoadState> loadState = _loadState;

    private final MutableLiveData<String> _errorMsg = new MutableLiveData<>();
    public LiveData<String> errorMsg = _errorMsg;

    // ─────────────────────────────────────────────────────────────
    // 公開方法：觸發資料載入
    // ─────────────────────────────────────────────────────────────

    /**
     * 載入所有圖表。
     *
     * 真實場景：
     * - 將此方法內部改為非同步（RxJava / Coroutine / AsyncTask）
     * - 在 ChartPresetFactory 的各 build 方法中替換 mock 資料為 API 呼叫
     */
    public void fetchAllCharts() {
        _loadState.setValue(LoadState.LOADING);
        try {
            // ★ 所有圖表建構邏輯集中於 ChartPresetFactory
            _chartList.setValue(ChartPresetFactory.buildAll());
            _loadState.setValue(LoadState.SUCCESS);
        } catch (Exception e) {
            _errorMsg.setValue("資料載入失敗：" + e.getMessage());
            _loadState.setValue(LoadState.ERROR);
        }
    }
}
