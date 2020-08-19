package com.antware.joggerlogger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.antware.joggerlogger.ChartFragment.StatMaxMinValues;
import com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData;

import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.SPEED;


public class ChartView extends View {

    private static final int VERT_GRID_LINES = 9;
    private static final float CHART_AXIS_WIDTH = 10;
    private static final float PATH_WIDTH = CHART_AXIS_WIDTH;
    private static final float GRID_PADDING = PATH_WIDTH / 2;
    private static final float PATH_PADDING = PATH_WIDTH;
    private static final int CHART_AXIS_COLOR = R.color.colorBlack;
    private static final int STROKE_COLOR = R.color.colorPrimaryDark;
    private StatMaxMinValues maxMinValues;

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
        gridPaint.setColor(ContextCompat.getColor(getContext(), CHART_AXIS_COLOR));
        gridPaint.setStrokeWidth(CHART_AXIS_WIDTH);

        pathPaint.setColor(ContextCompat.getColor(getContext(), STROKE_COLOR));
        pathPaint.setStrokeWidth(PATH_WIDTH);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (model == null) return;
        drawPath(canvas, maxMinValues);
        drawGrid(canvas);
    }

    private void drawPath(Canvas canvas, StatMaxMinValues maxMinValues) {
        int numWaypoints = model.getWaypoints().size();
        for (int i = 1; i < model.getWaypoints().size(); i++) {
            float startX = getPathX(numWaypoints, i - 1);
            float startY = getPathY(maxMinValues, i - 1);
            float stopX = getPathX(numWaypoints, i);
            float stopY = getPathY(maxMinValues, i);
            canvas.drawLine(startX, startY, stopX, stopY, pathPaint);
        }
    }

    private float getPathY(StatMaxMinValues maxMinValues, int i) {
        Waypoint waypoint = model.getWaypoints().get(i);
        double currentValue = vertData == SPEED ? waypoint.getCurrentSpeed() : waypoint.getLocation()
                .getAltitude();
        return getYPosition(maxMinValues, currentValue);
    }

    private float getYPosition(StatMaxMinValues maxMinValues, double currentValue) {
        double range = maxMinValues.getMaxValue() - maxMinValues.getMinValue();
        return (float) ((maxMinValues.getMaxValue() - currentValue) / range) * getPathMaxVertExtent()
                + GRID_PADDING;
    }

    private float getPathMaxVertExtent() {
        return getGridHeight() - PATH_PADDING;
    }

    private float getGridHeight() {
        return getHeight() - GRID_PADDING;
    }

    private float getPathX(int waypoints, float i) {
        return i / --waypoints * (getWidth() - PATH_PADDING) + PATH_PADDING;
    }

    private void drawGrid(Canvas canvas) {

        /*for (int i = 0; i < VERT_GRID_LINES; i++) {
            float startX = (float) i / VERT_GRID_LINES * getWidth();
            canvas.drawLine(startX, 0, startX, getHeight(), gridPaint);
        }*/
        canvas.drawLine(GRID_PADDING, 0, GRID_PADDING, getHeight(), gridPaint);
        canvas.drawLine(0, getGridHeight(), getWidth(), getGridHeight(), gridPaint);
    }

    public void init(LogViewModel model, StatMaxMinValues maxMinValues, VerticalData verticalData) {
        this.maxMinValues = maxMinValues;
        this.model = model;
        this.vertData = verticalData;
    }
}
