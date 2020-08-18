package com.antware.joggerlogger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.antware.joggerlogger.ExerciseCompleteFragment.HorizontalData;
import com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData;

import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.SPEED;


public class ChartView extends View {

    private static final int VERT_GRID_LINES = 9;
    private static final int CHART_AXIS_WIDTH = 10;
    private static final float PATH_WIDTH = CHART_AXIS_WIDTH;
    private static final int CHART_AXIS_COLOR = Color.BLACK;
    private double maxVertValue;

    HorizontalData horizData;
    VerticalData vertData;

    Paint gridPaint = new Paint();
    Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    LogViewModel model = null;

    public ChartView(Context context) {
        super(context);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        gridPaint.setColor(CHART_AXIS_COLOR);
        gridPaint.setStrokeWidth(CHART_AXIS_WIDTH);

        pathPaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        pathPaint.setStrokeWidth(PATH_WIDTH);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (model == null) return;
        drawPath(canvas, maxVertValue);
        drawGrid(canvas);
    }

    private void drawPath(Canvas canvas, double maxValue) {
        int numWaypoints = model.getWaypoints().size();
        for (int i = 1; i < model.getWaypoints().size(); i++) {
            float startX = getPathX(numWaypoints, i - 1);
            float startY = getPathY(maxValue, i - 1);
            float stopX = getPathX(numWaypoints, i);
            float stopY = getPathY(maxValue, i);
            canvas.drawLine(startX, startY, stopX, stopY, pathPaint);
        }
    }

    private float getPathY(double maxValue, int i) {
        Waypoint waypoint = model.getWaypoints().get(i);
        double currentValue = vertData == SPEED ? waypoint.getCurrentSpeed() : waypoint.getLocation()
                .getAltitude();
        return getYPosition(maxValue, currentValue);
    }

    private float getYPosition(double maxValue, double currentValue) {
        return (float) ((maxValue - currentValue) / maxValue) * getHeight();
    }

    private float getPathX(int waypoints, float i) {
        return i / --waypoints * getWidth();
    }

    private void drawGrid(Canvas canvas) {

        /*for (int i = 0; i < VERT_GRID_LINES; i++) {
            float startX = (float) i / VERT_GRID_LINES * getWidth();
            canvas.drawLine(startX, 0, startX, getHeight(), gridPaint);
        }*/
        canvas.drawLine(0, 0, 0, getHeight(), gridPaint);
        canvas.drawLine(0, getHeight(), getWidth(), getHeight(), gridPaint);
    }

    public void init(LogViewModel model, double maxVertValue, VerticalData verticalData) {
        this.maxVertValue = maxVertValue;
        this.model = model;
        this.vertData = verticalData;
    }
}
