package com.antware.joggerlogger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.antware.joggerlogger.LogViewModel.Duration;

import java.util.Objects;

public class ChartView extends View {

    private static final int VERT_GRID_LINES = 9;
    private static final int CHART_AXIS_WIDTH = 10;
    private static final float PATH_WIDTH = 10;
    private static final float PATH_TOP_PADDING = 50;
    private static final int CHART_AXIS_COLOR = Color.BLACK;
    private static final int CHART_DURATION_LABELS = 2;
    private static final int CHART_DISTANCE_LABELS = 4;
    private static final float LEGEND_TEXT_SIZE = 35;
    private static final int CHART_BOTTOM_PADDING = 35;
    private static final int CHART_HORIZ_PADDING = 5;
    private static final boolean HORIZ_DURATION_LEGEND = true;

    Paint gridPaint = new Paint();
    Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    LogViewModel model = null;

    /*public ChartView(Context context) {
        super(context);
    }*/

    public ChartView(Context context, AttributeSet attrs) {
        super(context);
        gridPaint.setColor(CHART_AXIS_COLOR);
        gridPaint.setStrokeWidth(CHART_AXIS_WIDTH);

        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        pathPaint.setStrokeWidth(PATH_WIDTH);

        textPaint.setColor(CHART_AXIS_COLOR);
        textPaint.setTextSize(LEGEND_TEXT_SIZE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(ResourcesCompat.getFont(getContext(), R.font.rubik_medium));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (model == null) return;
        drawPath(canvas);
        drawGrid(canvas);
        if (HORIZ_DURATION_LEGEND) drawDurLegend(canvas);
        else drawDistanceLegend(canvas);
    }

    private void drawDistanceLegend(Canvas canvas) {
        double totalDistance = Objects.requireNonNull(model.getDistance().getValue());
        // int legDistance =
        for (int i = 1; i <= CHART_DISTANCE_LABELS; i++) {

        }

    }

    private void drawDurLegend(Canvas canvas) {
        Duration duration = model.getDuration().getValue();
        assert duration != null;
        int seconds = duration.hours * 3600 + duration.minutes * 60 + duration.seconds;
        for (int i = 1; i <= CHART_DURATION_LABELS; i++) {
            Duration interval = model.getDurationFromMs((long) (1000 * seconds *
                    (float) i / (CHART_DURATION_LABELS + 1)));
            String intervalText = StatsFragment.Companion.getDurationText(interval);
            drawHorizLegendText(canvas, i, intervalText, CHART_DURATION_LABELS);
        }
    }

    private void drawHorizLegendText(Canvas canvas, float index, String text, int numLabels) {
        canvas.drawText(text, index * getChartWidth()  / (numLabels + 1)
                + CHART_HORIZ_PADDING, getHeight(), textPaint);
    }

    private void drawPath(Canvas canvas) {
        double maxSpeed = getMaxSpeed();
        int numWaypoints = model.getWaypoints().size();
        for (int i = 1; i < model.getWaypoints().size(); i++) {
            float startX = getPathX(numWaypoints, i - 1);
            float startY = getPathY(maxSpeed, i - 1);
            float stopX = getPathX(numWaypoints, i);
            float stopY = getPathY(maxSpeed, i);
            canvas.drawLine(startX, startY, stopX, stopY, pathPaint);
        }
    }

    private float getPathY(double maxSpeed, int i) {
        return (float) (((maxSpeed - model.getWaypoints().get(i).getCurrentSpeed()) / maxSpeed) *
                getChartHeight()) + PATH_TOP_PADDING - CHART_BOTTOM_PADDING;
    }

    private float getChartHeight() {
        return getHeight() - PATH_TOP_PADDING - CHART_BOTTOM_PADDING;
    }

    private float getPathX(int waypoints, float i) {
        return i / --waypoints * getChartWidth() + CHART_HORIZ_PADDING;
    }

    private double getMaxSpeed() {
        double max = 0;
        for (Waypoint point : model.getWaypoints()) {
            if (point.getCurrentSpeed() > max)
                max = point.getCurrentSpeed();
        }
        return max;
    }



    private float getChartWidth() {
        return getWidth();
    }

    private void drawGrid(Canvas canvas) {

        /*for (int i = 0; i < VERT_GRID_LINES; i++) {
            float startX = (float) i / VERT_GRID_LINES * getWidth();
            canvas.drawLine(startX, 0, startX, getHeight(), gridPaint);
        }*/
        canvas.drawLine(CHART_HORIZ_PADDING, 0, CHART_HORIZ_PADDING,
                getHeight() - CHART_BOTTOM_PADDING, gridPaint);
        canvas.drawLine(CHART_HORIZ_PADDING, getHeight() - CHART_BOTTOM_PADDING,
                getWidth() - CHART_HORIZ_PADDING, getHeight() - CHART_BOTTOM_PADDING,
                gridPaint);
    }

    public void initModel(LogViewModel model) {
        this.model = model;
    }
}
