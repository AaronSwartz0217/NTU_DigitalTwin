package com.ntu.qidong.digitaltwin.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.ntu.qidong.digitaltwin.R;
import com.ntu.qidong.digitaltwin.db.AppVisitDAO;
import com.ntu.qidong.digitaltwin.db.WeatherHistoryDAO;
import com.ntu.qidong.digitaltwin.model.WeatherHistory;
import com.ntu.qidong.digitaltwin.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * StatisticsActivity - 统计页面
 * 展示最近 7 天日平均气温趋势折线图和累计访问次数
 * 使用 MPAndroidChart v3.1.0 绘制图表
 */
public class StatisticsActivity extends AppCompatActivity {

    private static final String TAG = "StatisticsActivity";
    private static final int RECENT_DAYS = 7; // 显示最近 7 天数据

    private LineChart temperatureChart;
    private TextView tvTotalVisits;
    private ImageButton btnBack;

    private WeatherHistoryDAO weatherHistoryDAO;
    private AppVisitDAO appVisitDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // 初始化 DAO
        weatherHistoryDAO = new WeatherHistoryDAO(this);
        appVisitDAO = new AppVisitDAO(this);

        // 初始化视图
        initViews();

        // 加载数据并绘制图表
        loadData();
    }

    /**
     * 初始化视图组件
     */
    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        temperatureChart = findViewById(R.id.temperature_chart);
        tvTotalVisits = findViewById(R.id.tv_total_visits);

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 配置图表
        configureChart();
    }

    /**
     * 配置 MPAndroidChart
     */
    private void configureChart() {
        // 禁用描述
        temperatureChart.getDescription().setEnabled(false);

        // 启用触摸交互
        temperatureChart.setTouchEnabled(true);
        temperatureChart.setDragEnabled(true);
        temperatureChart.setScaleEnabled(false);
        temperatureChart.setPinchZoom(false);

        // 设置背景色
        temperatureChart.setBackgroundColor(Color.TRANSPARENT);

        // 设置图例
        temperatureChart.getLegend().setEnabled(false);

        // 配置 X 轴
        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);

        // 配置 Y 轴
        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setTextColor(Color.GRAY);

        // 禁用右侧 Y 轴
        temperatureChart.getAxisRight().setEnabled(false);

        // 设置空数据时显示的文字
        temperatureChart.setNoDataText("暂无气温数据");
        temperatureChart.setNoDataTextColor(Color.GRAY);
    }

    /**
     * 加载数据并绘制图表
     */
    private void loadData() {
        long startTime = System.currentTimeMillis();

        // 加载天气历史数据
        loadWeatherHistory();

        // 加载访问统计数据
        loadVisitStatistics();

        long endTime = System.currentTimeMillis();
        Log.d(TAG, "图表加载时间: " + (endTime - startTime) + "ms");
    }

    /**
     * 加载天气历史数据并绘制折线图
     */
    private void loadWeatherHistory() {
        List<WeatherHistory> historyList = weatherHistoryDAO.findRecentDays(RECENT_DAYS);

        if (historyList == null || historyList.isEmpty()) {
            // 没有数据时显示空状态
            temperatureChart.clear();
            temperatureChart.invalidate();
            return;
        }

        // 准备数据点
        List<Entry> entries = new ArrayList<>();
        List<String> dateLabels = new ArrayList<>();

        // 获取最近 7 天的日期标签
        String[] recentDates = DateUtils.getRecentDaysArray(RECENT_DAYS, "MM-dd");

        // 填充数据 (按日期顺序)
        for (int i = 0; i < RECENT_DAYS; i++) {
            String targetDate = recentDates[i];

            // 查找对应的历史数据
            WeatherHistory found = null;
            for (WeatherHistory history : historyList) {
                if (history.getDate().endsWith(targetDate.substring(5))) { // 比较 MM-dd 部分
                    found = history;
                    break;
                }
            }

            if (found != null) {
                entries.add(new Entry(i, (float) found.getTempAvg()));
            } else {
                // 没有数据的日子用 0 或者跳过
                entries.add(new Entry(i, 0));
            }
            dateLabels.add(targetDate);
        }

        // 设置 X 轴标签
        temperatureChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dateLabels.size()) {
                    return dateLabels.get(index);
                }
                return "";
            }
        });

        // 创建数据集
        LineDataSet dataSet = new LineDataSet(entries, "日平均气温");

        // 配置数据集样式
        dataSet.setColor(getResources().getColor(R.color.chart_line, getTheme()));
        dataSet.setCircleColor(getResources().getColor(R.color.chart_line, getTheme()));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(2f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.chart_fill, getTheme()));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // 平滑曲线

        // 设置数据点到显示的值
        dataSet.setDrawValues(true);

        // 创建线数据
        LineData lineData = new LineData(dataSet);
        temperatureChart.setData(lineData);
        temperatureChart.animateX(500); // 动画效果
        temperatureChart.invalidate();
    }

    /**
     * 加载访问统计数据
     */
    private void loadVisitStatistics() {
        int totalVisits = appVisitDAO.getTotalVisitCount();

        if (totalVisits > 0) {
            tvTotalVisits.setText(totalVisits + " 次");
        } else {
            tvTotalVisits.setText(getString(R.string.no_data));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 刷新数据
        loadData();
    }
}
