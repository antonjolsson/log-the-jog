package com.antware.joggerlogger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

public class ChartView extends View {

    private static final int VERT_GRID_LINES = 9;
    private static final int CHART_AXIS_WIDTH = 20;
    private static final float PATH_WIDTH = 10;
    private static final float PATH_TOP_PADDING = 30;
    Paint gridPaint = new Paint();
    Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    LogViewModel model = null;

    public ChartView(Context context) {
        super(context);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context);
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorDisabled));
        gridPaint.setStrokeWidth(CHART_AXIS_WIDTH);
        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        pathPaint.setStrokeWidth(PATH_WIDTH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (model == null) return;
        drawPath(canvas);
        drawGrid(canvas);
        drawLegend(canvas);
    }

    private void drawPath(Canvas canvas) {
        double maxSpeed = getMaxSpeed();
        int waypoints = model.getWaypoints().size();
        for (int i = 1; i < model.getWaypoints().size(); i++) {
            float startX = getPathX(waypoints, i - 1);
            float startY = getPathY(maxSpeed, i - 1);
            float stopX = getPathX(waypoints, i);
            float stopY = getPathY(maxSpeed, i);
            canvas.drawLine(startX, startY, stopX, stopY, pathPaint);
        }
    }

    private float getPathY(double maxSpeed, int i) {
        return (float) ((maxSpeed - model.getWaypoints().get(i).getCurrentSpeed())
                / maxSpeed * (getHeight() - PATH_TOP_PADDING)) + PATH_TOP_PADDING;
    }

    private float getPathX(int waypoints, float i) {
        return i / waypoints * getWidth();
    }

    private double getMaxSpeed() {
        double max = 0;
        for (Waypoint point : model.getWaypoints()) {
            if (point.getCurrentSpeed() > max)
                max = point.getCurrentSpeed();
        }
        return max;
    }

    private void drawLegend(Canvas canvas) {
        LogViewModel.Duration duration = model.getDuration().getValue();
        assert duration != null;
        int seconds = duration.hours * 3600 + duration.minutes * 60 + duration.seconds;
    }

    private void drawGrid(Canvas canvas) {

        /*for (int i = 0; i < VERT_GRID_LINES; i++) {
            float startX = (float) i / VERT_GRID_LINES * getWidth();
            canvas.drawLine(startX, 0, startX, getHeight(), gridPaint);
        }*/
        canvas.drawLine(0, 0, 0, getHeight(), gridPaint);
        canvas.drawLine(0, getHeight(), getWidth(), getHeight(), gridPaint);
    }

    public void initModel(LogViewModel model) {
        this.model = model;
    }
}
