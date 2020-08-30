package com.antware.joggerlogger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.antware.joggerlogger.ChartFragment.DataRange;
import com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData;

import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.SPEED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.PAUSED;
import static com.antware.joggerlogger.LogViewModel.ExerciseStatus.RESUMED;


public class ChartView extends View {

    private static final int VERT_GRID_LINES = 9;
    private static final float CHART_AXIS_WIDTH = 10;
    private static final float PATH_WIDTH = CHART_AXIS_WIDTH;
    private static final float AXIS_PADDING = PATH_WIDTH / 2;
    private static final float PATH_PADDING = PATH_WIDTH;
    private static final int CHART_AXIS_COLOR = R.color.colorBlack;
    private static final int PATH_COLOR = R.color.colorPrimaryDark;
    private static final int PATH_FILL_ALPHA = 128;
    private DataRange dataRange;

    VerticalData vertData;

    Paint gridPaint = new Paint();
    Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    LogViewModel model = null;
    private boolean filledPath;

    public ChartView(Context context) {
        super(context);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void initPaints() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), CHART_AXIS_COLOR));
        gridPaint.setStrokeWidth(CHART_AXIS_WIDTH);

        pathPaint.setColor(ContextCompat.getColor(getContext(), PATH_COLOR));
        if (filledPath) {
            pathPaint.setStyle(Paint.Style.FILL);
            pathPaint.setAlpha(PATH_FILL_ALPHA);
        }
        else {
            pathPaint.setStrokeWidth(PATH_WIDTH);
            pathPaint.setStrokeJoin(Paint.Join.MITER);
            pathPaint.setStrokeCap(Paint.Cap.ROUND);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (model == null) return;
        int numWaypoints = model.getWaypoints().size();
        if (filledPath) drawPath(canvas, dataRange, numWaypoints);
        else drawLine(canvas, dataRange, numWaypoints);
        drawGrid(canvas);
    }

    private void drawPath(Canvas canvas, DataRange dataRange, int numWaypoints) {
        Path path = new Path();
        for (int i = 0; i < numWaypoints; i++) {
            Point point = getDataPoint(numWaypoints, dataRange, i);
            if (i == 0) path.moveTo(point.x, point.y);
            else path.lineTo(point.x, point.y);
        }
        path.lineTo(getWidth(), (int) getGridHeight());
        path.lineTo(0, (int) getGridHeight());
        path.close();
        canvas.drawPath(path, pathPaint);
    }

    private void drawLine(Canvas canvas, DataRange dataRange, int numWaypoints) {
        for (int i = 1; i < numWaypoints; i++) {
            Point start = getDataPoint(numWaypoints, dataRange, i - 1);
            Point end = getDataPoint(numWaypoints, dataRange, i);
            canvas.drawLine(start.x, start.y, end.x, end.y, pathPaint);
        }
    }

    private Point getDataPoint(int numWaypoints, DataRange dataRange, int index) {
        return new Point((int) getPathX(numWaypoints, index), (int) getPathY(dataRange, index));
    }

    private float getPathY(DataRange dataRange, int i) {
        Waypoint waypoint = model.getWaypoints().get(i);
        /*double currentValue;
        if (vertData == SPEED) currentValue = waypoint.getCurrentSpeed();
        else {
            if (waypoint.getStatus() == PAUSED)
                currentValue = model.getWaypoints().get(i - 1).getCurrentSpeed();
            else currentValue = waypoint.getCurrentSpeed();
        }*/
        double currentValue = vertData == SPEED ? waypoint.getCurrentSpeed() : waypoint.getAltitude();
        return getYPosition(dataRange, currentValue);
    }

    private float getYPosition(DataRange dataRange, double currentValue) {
        double range = dataRange.getMaxValue() - dataRange.getMinValue();
        return (float) ((dataRange.getMaxValue() - currentValue) / range) * getPathMaxVertExtent()
                + AXIS_PADDING;
    }

    private float getPathMaxVertExtent() {
        return getGridHeight() - PATH_PADDING;
    }

    private float getGridHeight() {
        return getHeight() - AXIS_PADDING;
    }

    private float getPathX(int waypoints, float i) {
        return i / --waypoints * (getWidth() - PATH_PADDING) + PATH_PADDING;
    }

    private void drawGrid(Canvas canvas) {

        /*for (int i = 0; i < VERT_GRID_LINES; i++) {
            float startX = (float) i / VERT_GRID_LINES * getWidth();
            canvas.drawLine(startX, 0, startX, getHeight(), gridPaint);
        }*/
        canvas.drawLine(AXIS_PADDING, 0, AXIS_PADDING, getHeight(), gridPaint);
        canvas.drawLine(0, getGridHeight(), getWidth(), getGridHeight(), gridPaint);
    }

    public void init(LogViewModel model, DataRange dataRange, VerticalData verticalData,
                     boolean filledPath) {
        this.dataRange = dataRange;
        this.model = model;
        this.vertData = verticalData;
        this.filledPath = filledPath;
        initPaints();
    }
}
