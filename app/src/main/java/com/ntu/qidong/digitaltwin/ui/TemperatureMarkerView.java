package com.ntu.qidong.digitaltwin.ui;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.ntu.qidong.digitaltwin.R;

public class TemperatureMarkerView extends MarkerView {

    private TextView tvTemperature;

    public TemperatureMarkerView(Context context) {
        super(context, R.layout.marker_temperature);

        tvTemperature = findViewById(R.id.tv_marker_temp);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        float temp = e.getY();
        tvTemperature.setText(String.format("%.1f°C", temp));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight() - 10);
    }
}