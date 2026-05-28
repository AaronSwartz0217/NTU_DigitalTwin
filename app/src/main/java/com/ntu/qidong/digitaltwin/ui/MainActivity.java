package com.ntu.qidong.digitaltwin.ui;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.ntu.qidong.digitaltwin.db.AppVisitDAO;
import com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO;
import com.ntu.qidong.digitaltwin.model.AppVisit;
import com.ntu.qidong.digitaltwin.model.WeatherHistory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.util.List;

import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.bridge.JavaScriptInjector;
import com.ntu.qidong.digitaltwin.model.WeatherData;
import com.ntu.qidong.digitaltwin.service.WeatherService;
import com.ntu.qidong.digitaltwin.utils.NetworkUtils;

/**
 * MainActivity - 主界面
 * 包含 WebView 区域、顶部菜单栏和底部天气状态栏
 * 负责 WebView 生命周期管理和天气数据展示
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // WebView 加载的 URL - GitHub Pages
    private static final String WEB_VIEW_URL = "https://aaronswartz0217.github.io/NTU_Building/";

    private WebView webView;
    private LinearLayout errorView;
    private TextView errorMessage;
    private TextView tvTemperature;
    private TextView tvWeatherCondition;
    private TextView tvHumidity;
    private TextView tvWindSpeed;
    private TextView tvFeelsLike;
    private TextView tvTotalVisits;
    private TextView tvTodayVisits;
    private TextView tvAvgTemp;
    private TextView tvUpdateTime;
    private LinearLayout historyContainer;
    private ProgressBar weatherLoading;
    private Button btnRefresh;
    private Button btnRetry;
    private com.github.mikephil.charting.charts.LineChart lineChart;
    
    // 侧边栏和标题栏控制
    private LinearLayout titleBar;
    private LinearLayout leftPanel;
    private LinearLayout rightPanel;
    private LinearLayout topTitleBox;
    private Button btnTogglePanels;
    private Button btnHideTitle;
    private boolean panelsVisible = true;
    
    // 网页加载状态
    private boolean webViewLoaded = false;
    
    // 折线图视图模式
    private boolean isTodayView = true; // true: 今日视图, false: 本周视图

    private WeatherService weatherService;
    private JavaScriptInjector jsInjector;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 强制横屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.activity_main);

        // 隐藏系统栏
        hideSystemBars();

        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化视图组件（原生UI）
        initViews();

        // 初始化 WeatherService
        weatherService = new WeatherService(this);
        weatherService.setRefreshCallback(weatherRefreshCallback);

        // 记录访问
        recordAppVisit();

        // 初始化 WebView（作为背景显示）
        initWebView();

        // 启动天气自动刷新
        weatherService.startAutoRefresh();
    }

    /**
     * 记录应用访问
     */
    private void recordAppVisit() {
        try {
            AppVisitDAO visitDAO = new AppVisitDAO(this);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            visitDAO.recordVisit(today);
            Log.d(TAG, "已记录今日访问: " + today);
        } catch (Exception e) {
            Log.e(TAG, "记录访问失败", e);
        }
    }

    /**
     * 初始化视图组件（原生UI）
     */
    private void initViews() {
        // 初始化视图组件引用
        webView = findViewById(R.id.webview);
        errorView = findViewById(R.id.error_view);
        errorMessage = findViewById(R.id.error_message);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvWeatherCondition = findViewById(R.id.tv_weather_condition);
        tvHumidity = findViewById(R.id.tv_humidity);
        tvWindSpeed = findViewById(R.id.tv_wind_speed);
        tvFeelsLike = findViewById(R.id.tv_feels_like);
        tvTotalVisits = findViewById(R.id.tv_total_visits);
        tvTodayVisits = findViewById(R.id.tv_today_visits);
        tvAvgTemp = findViewById(R.id.tv_avg_temp);
        tvUpdateTime = findViewById(R.id.tv_update_time);
        historyContainer = findViewById(R.id.history_container);
        weatherLoading = findViewById(R.id.weather_loading);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnRetry = findViewById(R.id.btn_retry);
        lineChart = findViewById(R.id.line_chart);
        
        // 论坛按钮 - 返回论坛首页
        Button btnForum = findViewById(R.id.btn_forum);
        
        // 温度趋势切换按钮
        Button btnToday = findViewById(R.id.btn_today);
        Button btnWeek = findViewById(R.id.btn_week);

        // 初始化折线图
        initLineChart();

        // 温度趋势切换按钮点击事件
        btnToday.setOnClickListener(v -> {
            isTodayView = true;
            updateLineChart();
        });

        btnWeek.setOnClickListener(v -> {
            isTodayView = false;
            updateLineChart();
        });

        // 刷新按钮点击事件
        btnRefresh.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(this)) {
                weatherService.refreshWeather();
                updateDataDisplay();
                // 只在网页未加载成功时刷新网页
                if (webView != null && !webViewLoaded) {
                    webView.reload();
                }
            } else {
                NetworkUtils.showNetworkError(this);
            }
        });

        // 论坛按钮点击事件 - 返回论坛首页
        btnForum.setOnClickListener(v -> {
            finish();
        });

        // 重试按钮点击事件
        btnRetry.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            reloadWebView();
        });
        
        // 侧边栏相关控件
        titleBar = findViewById(R.id.title_bar);
        leftPanel = findViewById(R.id.left_panel);
        rightPanel = findViewById(R.id.right_panel);
        topTitleBox = findViewById(R.id.top_title_box);
        btnTogglePanels = findViewById(R.id.btn_toggle_panels);
        btnHideTitle = findViewById(R.id.btn_hide_title);
        
        // 获取WebView的父容器FrameLayout
        FrameLayout webViewContainer = findViewById(R.id.webview_container);
        
        // 侧边栏切换按钮
        btnTogglePanels.setOnClickListener(v -> {
            togglePanels(webViewContainer);
        });
        
        // 隐藏标题栏按钮
        btnHideTitle.setOnClickListener(v -> {
            titleBar.setVisibility(View.GONE);
        });
        
        // 长按唤起标题栏
        btnTogglePanels.setOnLongClickListener(v -> {
            titleBar.setVisibility(View.VISIBLE);
            return true;
        });
        
        Log.d(TAG, "原生UI组件初始化完成");
    }
    
    /**
     * 切换侧边栏显示/隐藏
     */
    private void togglePanels(FrameLayout webViewContainer) {
        panelsVisible = !panelsVisible;
        
        if (panelsVisible) {
            // 显示侧边栏和顶部标题框
            leftPanel.setVisibility(View.VISIBLE);
            rightPanel.setVisibility(View.VISIBLE);
            topTitleBox.setVisibility(View.VISIBLE);
            btnTogglePanels.setText("隐藏");
            Log.d(TAG, "侧边栏和顶部标题已显示");
        } else {
            // 隐藏侧边栏和顶部标题框，WebView已经是全屏的
            leftPanel.setVisibility(View.GONE);
            rightPanel.setVisibility(View.GONE);
            topTitleBox.setVisibility(View.GONE);
            btnTogglePanels.setText("显示");
            Log.d(TAG, "侧边栏和顶部标题已隐藏，WebView全屏显示");
        }
    }

    /**
     * WebView 加载重试次数
     */
    private int webViewRetryCount = 0;
    private static final int MAX_WEBVIEW_RETRY = 3;
    private static final long WEBVIEW_RETRY_DELAY = 3000; // 3秒后重试

    /**
     * 初始化 WebView（预留用于未来 GLTF 模型展示）
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        if (webView == null) {
            Log.e(TAG, "WebView 为空，无法初始化");
            return;
        }
        
        WebView.setWebContentsDebuggingEnabled(true);
        Log.d(TAG, "WebView调试已启用");
        
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setDatabaseEnabled(true);
        
        // 性能优化配置
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setEnableSmoothTransition(true);
        
        // 添加更多兼容性配置
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        
        // 设置用户代理，模拟桌面浏览器
        String userAgent = webSettings.getUserAgentString();
        webSettings.setUserAgentString(userAgent + " NTU-DigitalTwin/1.0");
        
        // 启用硬件加速
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // 显示 WebView
        webView.setVisibility(View.VISIBLE);
        
        // 初始化 jsInjector
        jsInjector = new JavaScriptInjector(MainActivity.this, weatherService, webView);
        jsInjector.injectJavaScriptInterfaces();
        jsInjector.injectGlobalConfig();

        // 设置 WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "WebView 开始加载: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "WebView 加载完成: " + url);
                
                // 重置重试计数
                webViewRetryCount = 0;
                
                // 标记网页已加载成功
                webViewLoaded = true;

                // 注入天气数据到前端
                WeatherData currentWeather = weatherService.getCurrentWeatherData();
                if (currentWeather != null) {
                    mainHandler.postDelayed(() -> {
                        if (jsInjector != null) {
                            jsInjector.injectWeatherData(currentWeather);
                        }
                    }, 500); // 延迟 500ms 确保页面已完全加载
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                
                String url = request.getUrl().toString();
                final String errorMsg;
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    errorMsg = "错误码: " + error.getErrorCode() + ", 描述: " + error.getDescription();
                } else {
                    errorMsg = "加载失败";
                }
                
                Log.e(TAG, "WebView加载错误: " + url + ", " + errorMsg);
                
                if (request.isForMainFrame()) {
                    // 尝试自动重试
                    if (webViewRetryCount < MAX_WEBVIEW_RETRY) {
                        webViewRetryCount++;
                        Log.d(TAG, "WebView 自动重试第 " + webViewRetryCount + " 次...");
                        mainHandler.postDelayed(() -> {
                            if (webView != null && NetworkUtils.isNetworkAvailable(MainActivity.this)) {
                                webView.reload();
                            }
                        }, WEBVIEW_RETRY_DELAY);
                    } else {
                        // 重试次数用完，显示错误
                        final String detailedError = "页面加载失败\n" + errorMsg + "\nURL: " + WEB_VIEW_URL + "\n\n请检查网络连接后点击重试";
                        mainHandler.post(() -> {
                            webViewLoaded = false;
                            webViewRetryCount = 0;
                            showErrorView(detailedError);
                        });
                    }
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                
                String url = request.getUrl().toString();
                final int statusCode = errorResponse.getStatusCode();
                
                Log.e(TAG, "WebView HTTP错误: " + url + ", 状态码: " + statusCode);
                
                if (request.isForMainFrame() && statusCode >= 400) {
                    // 尝试自动重试
                    if (webViewRetryCount < MAX_WEBVIEW_RETRY) {
                        webViewRetryCount++;
                        Log.d(TAG, "WebView HTTP错误重试第 " + webViewRetryCount + " 次...");
                        mainHandler.postDelayed(() -> {
                            if (webView != null) {
                                webView.reload();
                            }
                        }, WEBVIEW_RETRY_DELAY);
                    } else {
                        final String httpError = "HTTP错误: " + statusCode + "\nURL: " + WEB_VIEW_URL + "\n\n请检查网络连接后点击重试";
                        mainHandler.post(() -> {
                            webViewLoaded = false;
                            webViewRetryCount = 0;
                            showErrorView(httpError);
                        });
                    }
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 允许加载所有 URL
                return false;
            }
        });

        // 设置 WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                // 可以在这里显示加载进度
                Log.d(TAG, "WebView 加载进度: " + newProgress + "%");
            }
        });

        // 加载 WebView URL
        Log.d(TAG, "开始加载 WebView URL: " + WEB_VIEW_URL);
        webView.loadUrl(WEB_VIEW_URL);
    }

    /**
     * 隐藏系统栏（状态栏和导航栏）
     * 实现沉浸式全屏模式
     */
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 推荐方案
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                // 隐藏状态栏和导航栏
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                // 设置沉浸式行为：滑动边缘临时显示系统栏，3秒后自动隐藏
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // 兼容 API 19-29 的旧方法
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }

    /**
     * 初始化温度趋势折线图（使用MPAndroidChart）
     */
    private void initLineChart() {
        if (lineChart == null) return;

        // 性能优化：禁用硬件加速可能导致卡顿，保持启用
        lineChart.setHardwareAccelerationEnabled(true);
        
        // 配置图表基本属性
        lineChart.setNoDataText("暂无历史数据");
        lineChart.setNoDataTextColor(0xffffffff);
        lineChart.setBackgroundColor(0x00000000);
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(false);

        // 启用点击交互以显示温度
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(false);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setHighlightPerTapEnabled(true);
        lineChart.setHighlightPerDragEnabled(false);

        // 配置描述
        lineChart.getDescription().setEnabled(false);

        // 配置图例
        lineChart.getLegend().setEnabled(false);

        // 配置X轴
        com.github.mikephil.charting.components.XAxis xAxis = lineChart.getXAxis();
        xAxis.setEnabled(true);
        xAxis.setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xffffffff);
        xAxis.setTextSize(8f);
        xAxis.setDrawGridLines(false);  // 性能优化：禁用网格线
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelCount(3, true);
        xAxis.setAvoidFirstLastClipping(true);

        // 配置Y轴（左侧）
        com.github.mikephil.charting.components.YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(0xffffffff);
        yAxisLeft.setTextSize(8f);
        yAxisLeft.setDrawGridLines(false);  // 性能优化：禁用网格线
        yAxisLeft.setDrawAxisLine(false);
        yAxisLeft.setLabelCount(3, true);

        // 配置Y轴（右侧）
        com.github.mikephil.charting.components.YAxis yAxisRight = lineChart.getAxisRight();
        yAxisRight.setEnabled(false);

        // 配置图表内边距 - 适配框子大小
        lineChart.setViewPortOffsets(5f, 5f, 5f, 15f);
        lineChart.setExtraOffsets(2f, 2f, 2f, 2f);
        
        // 性能优化：减少动画时长
        lineChart.animateY(300);
        
        // 启用MarkerView显示温度
        TemperatureMarkerView marker = new TemperatureMarkerView(this);
        lineChart.setMarker(marker);
    }

    /**
     * 更新折线图显示（使用MPAndroidChart）
     */
    private void updateLineChart() {
        if (lineChart == null) return;

        if (isTodayView) {
            // 今日视图：24小时温度变化
            updateTodayChart();
        } else {
            // 本周视图：7天平均温度
            updateWeekChart();
        }
    }

    /**
     * 更新今日温度变化图表（24小时）
     */
    private void updateTodayChart() {
        // 获取今日24小时温度数据
        java.util.List<com.ntu.qidong.digitaltwin.model.WeatherHistory> hourlyData = getTodayHourlyData();

        if (hourlyData == null || hourlyData.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("暂无今日数据");
            return;
        }

        // 构建数据点
        java.util.List<com.github.mikephil.charting.data.Entry> entries = new java.util.ArrayList<>();
        java.util.List<String> xLabels = new java.util.ArrayList<>();

        for (int i = 0; i < hourlyData.size(); i++) {
            com.ntu.qidong.digitaltwin.model.WeatherHistory data = hourlyData.get(i);
            float temp = (float) data.getTempAvg();
            entries.add(new com.github.mikephil.charting.data.Entry(i, temp));

            // 生成X轴标签（显示小时）
            if (i == 0) {
                xLabels.add("0时");
            } else if (i == 6) {
                xLabels.add("6时");
            } else if (i == 12) {
                xLabels.add("12时");
            } else if (i == 18) {
                xLabels.add("18时");
            } else if (i == hourlyData.size() - 1) {
                xLabels.add("24时");
            } else {
                xLabels.add("");
            }
        }

        // 创建并配置数据集
        com.github.mikephil.charting.data.LineDataSet dataSet = createDataSet(entries);
        
        // 设置X轴标签
        lineChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(xLabels));

        // 设置数据
        com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    /**
     * 更新本周温度变化图表（7天平均温度）
     */
    private void updateWeekChart() {
        // 使用 DAO 获取历史数据（7天）
        com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO dao =
            new com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO(this);
        java.util.List<com.ntu.qidong.digitaltwin.model.WeatherHistory> historyList = dao.findRecentDays(7);

        if (historyList == null || historyList.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("暂无本周数据");
            return;
        }

        // 构建数据点
        java.util.List<com.github.mikephil.charting.data.Entry> entries = new java.util.ArrayList<>();
        java.util.List<String> xLabels = new java.util.ArrayList<>();

        for (int i = 0; i < historyList.size(); i++) {
            com.ntu.qidong.digitaltwin.model.WeatherHistory history = historyList.get(i);
            float temp = (float) history.getTempAvg();
            entries.add(new com.github.mikephil.charting.data.Entry(i, temp));

            // 生成X轴标签
            if (i == 0) {
                xLabels.add("-3d");
            } else if (i == 3) {
                xLabels.add("今天");
            } else if (i == historyList.size() - 1) {
                xLabels.add("+3d");
            } else {
                xLabels.add("");
            }
        }

        // 创建并配置数据集
        com.github.mikephil.charting.data.LineDataSet dataSet = createDataSet(entries);
        
        // 设置X轴标签
        lineChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(xLabels));

        // 设置数据
        com.github.mikephil.charting.data.LineData lineData = new com.github.mikephil.charting.data.LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    /**
     * 创建数据集并配置样式
     */
    private com.github.mikephil.charting.data.LineDataSet createDataSet(java.util.List<com.github.mikephil.charting.data.Entry> entries) {
        com.github.mikephil.charting.data.LineDataSet dataSet = new com.github.mikephil.charting.data.LineDataSet(entries, "温度");
        
        // 配置线条样式
        dataSet.setColor(0xff00ffff);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(0xff00ffff);
        dataSet.setCircleRadius(3f);
        dataSet.setCircleHoleColor(0xffffffff);
        dataSet.setCircleHoleRadius(1.5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER);
        
        // 配置填充区域
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(0x2600ffff);
        dataSet.setFillAlpha(15);

        return dataSet;
    }

    /**
     * 获取今日24小时温度数据（模拟数据）
     */
    private java.util.List<com.ntu.qidong.digitaltwin.model.WeatherHistory> getTodayHourlyData() {
        // 从数据库获取今日数据，如果没有则返回模拟数据
        com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO dao =
            new com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO(this);
        
        // 尝试获取今日数据
        WeatherData currentWeather = weatherService.getCurrentWeatherData();
        if (currentWeather == null) {
            return null;
        }

        // 创建24小时模拟数据
        java.util.List<com.ntu.qidong.digitaltwin.model.WeatherHistory> hourlyData = new java.util.ArrayList<>();
        float baseTemp = Float.parseFloat(currentWeather.getTemperature().replace("°C", "").trim());
        
        // 生成24小时温度变化曲线
        for (int hour = 0; hour < 24; hour++) {
            com.ntu.qidong.digitaltwin.model.WeatherHistory hourlyRecord = new com.ntu.qidong.digitaltwin.model.WeatherHistory();
            
            // 模拟温度变化：凌晨低，中午高
            float tempVariation = (float) (Math.sin((hour - 6) * Math.PI / 12) * 5);
            float temp = baseTemp + tempVariation;
            
            hourlyRecord.setTempAvg(temp);
            hourlyRecord.setDate(String.format("%02d时", hour));
            hourlyData.add(hourlyRecord);
        }

        return hourlyData;
    }

    /**
     * 显示错误视图
     */
    private void showErrorView(String message) {
        mainHandler.post(() -> {
            webView.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
            errorMessage.setText(message);
        });
    }

    /**
     * 重新加载 WebView（预留方法）
     */
    private void reloadWebView() {
        if (webView == null) {
            Log.e(TAG, "WebView 为空，无法重新加载");
            return;
        }
        
        Log.d(TAG, "重新加载 WebView: " + WEB_VIEW_URL);
        
        mainHandler.post(() -> {
            webView.setVisibility(View.VISIBLE);
            errorView.setVisibility(View.GONE);
            webViewLoaded = false;
            webView.clearCache(true);
            webView.clearHistory();
            webView.loadUrl(WEB_VIEW_URL);
        });
    }

    /**
     * 更新天气数据显示
     */
    private void updateWeatherBar(WeatherData weatherData) {
        if (weatherData == null) {
            return;
        }

        mainHandler.post(() -> {
            tvTemperature.setText(weatherData.getTemperature() + "°C");
            tvWeatherCondition.setText(weatherData.getWeatherCondition());
            tvHumidity.setText(weatherData.getHumidity() + "%");
            tvWindSpeed.setText(weatherData.getWindSpeed());
            tvFeelsLike.setText(weatherData.getFeelsLike() + "°C");
            weatherLoading.setVisibility(View.GONE);
            
            // 更新时间
            String updateTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.CHINA)
                    .format(new java.util.Date(weatherData.getUpdateTime()));
            tvUpdateTime.setText(updateTime);
            
            // 更新历史数据
            updateHistoryDisplay();
        });
    }
    
    /**
     * 更新历史天气记录显示
     */
    private void updateHistoryDisplay() {
        if (historyContainer == null) return;
        
        historyContainer.removeAllViews();
        List<com.ntu.qidong.digitaltwin.model.WeatherHistory> historyList = 
                    weatherService.get14DayWeatherData();
        
        for (com.ntu.qidong.digitaltwin.model.WeatherHistory history : historyList) {
            LinearLayout historyItem = new LinearLayout(this);
            historyItem.setOrientation(LinearLayout.HORIZONTAL);
            historyItem.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            historyItem.setGravity(android.view.Gravity.CENTER_VERTICAL);
            historyItem.setPadding(8, 6, 8, 6);
            historyItem.setBackgroundResource(R.drawable.panel_background_transparent);
            
            TextView dateText = new TextView(this);
            dateText.setText(history.getDate());
            dateText.setTextColor(android.graphics.Color.parseColor("#00ffff"));
            dateText.setTextSize(10);
            dateText.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            TextView tempText = new TextView(this);
            tempText.setText(String.format("%.1f°C", history.getTempAvg()));
            tempText.setTextColor(android.graphics.Color.parseColor("#00ffff"));
            tempText.setTextSize(11);
            tempText.setTypeface(null, android.graphics.Typeface.BOLD);
            tempText.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tempText.setGravity(android.view.Gravity.CENTER);

            TextView weatherText = new TextView(this);
            weatherText.setText(history.getWeatherCondition());
            weatherText.setTextColor(android.graphics.Color.parseColor("#00ffff"));
            weatherText.setTextSize(10);
            weatherText.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            weatherText.setGravity(android.view.Gravity.CENTER);
            
            historyItem.addView(dateText);
            historyItem.addView(tempText);
            historyItem.addView(weatherText);
            
            historyContainer.addView(historyItem);
        }
    }
    
    /**
     * 更新所有数据显示
     */
    private void updateDataDisplay() {
        WeatherData weatherData = weatherService.getCurrentWeatherData();
        if (weatherData != null) {
            updateWeatherBar(weatherData);
            updateLineChart();
        }

        // 更新访问量统计
        updateVisitStats();
    }

    /**
     * 更新访问量统计显示
     */
    private void updateVisitStats() {
        try {
            AppVisitDAO visitDAO = new AppVisitDAO(this);

            // 获取累计访问次数
            int totalVisits = visitDAO.getTotalVisitCount();
            tvTotalVisits.setText(String.valueOf(totalVisits));

            // 获取今日访问次数
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int todayVisits = visitDAO.getTodayVisitCount(today);
            tvTodayVisits.setText(String.valueOf(todayVisits));

            // 获取平均温度
            WeatherHistoryDAO weatherDAO = new WeatherHistoryDAO(this);
            java.util.List<WeatherHistory> allHistory = weatherDAO.findAll();
            if (!allHistory.isEmpty()) {
                double sum = 0;
                for (WeatherHistory h : allHistory) {
                    sum += h.getTempAvg();
                }
                double avgTemp = sum / allHistory.size();
                tvAvgTemp.setText(String.format("%.1f°C", avgTemp));
            } else {
                tvAvgTemp.setText("--°C");
            }

            Log.d(TAG, "访问量更新 - 累计: " + totalVisits + ", 今日: " + todayVisits);
        } catch (Exception e) {
            Log.e(TAG, "更新访问量失败", e);
        }
    }
    
    /**
     * 天气刷新回调
     */
    private final WeatherService.WeatherRefreshCallback weatherRefreshCallback =
            new WeatherService.WeatherRefreshCallback() {
                @Override
                public void onWeatherRefreshed(WeatherData weatherData) {
                    updateWeatherBar(weatherData);

                    // 向前端注入最新的天气数据
                    if (jsInjector != null) {
                        jsInjector.injectWeatherData(weatherData);
                    }
                }

                @Override
                public void onRefreshError(String errorMessage) {
                    mainHandler.post(() -> {
                        weatherLoading.setVisibility(View.GONE);
                        // 显示错误但保持上次数据
                        if (tvTemperature.getText().toString().isEmpty() ||
                                tvTemperature.getText().toString().equals(getString(R.string.weather_loading))) {
                            tvTemperature.setText("--°C");
                            tvWeatherCondition.setText("加载失败");
                            tvHumidity.setText("");
                        }
                    });
                    Log.e(TAG, "天气刷新错误: " + errorMessage);
                }
            };

    @Override
    protected void onResume() {
        super.onResume();
        // 恢复 WebView
        if (webView != null) {
            webView.onResume();
        }
        // 切回应用时重新隐藏系统栏，防止系统栏自动弹出
        hideSystemBars();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停 WebView
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        // 停止天气刷新
        if (weatherService != null) {
            weatherService.stopAutoRefresh();
        }

        // 销毁 WebView
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.removeAllViews();
            webView.destroyDrawingCache();
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
        Log.d(TAG, "MainActivity 已销毁");
    }
}
