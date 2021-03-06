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

/**
 * Class for drawing a line chart in a ChartFragment.
 * @author Anton J Olsson
 */
public class ChartView extends View {

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
    Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    LogViewModel model = null;
    private boolean filledPath;

    public ChartView(Context context) {
        super(context);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Initializes the different paints used in the chart.
     */
    private void initPaints() {
        gridPaint.setColor(ContextCompat.getColor(getContext(), CHART_AXIS_COLOR));
        gridPaint.setStrokeWidth(CHART_AXIS_WIDTH);

        pathPaint.setColor(ContextCompat.getColor(getContext(), PATH_COLOR));
        pathPaint.setStyle(Paint.Style.FILL);
        pathPaint.setAlpha(PATH_FILL_ALPHA);

        linePaint.setColor(ContextCompat.getColor(getContext(), PATH_COLOR));
        linePaint.setStrokeWidth(PATH_WIDTH);
        linePaint.setStrokeJoin(Paint.Join.MITER);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * The callback draw method.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (model == null) return;
        int numWaypoints = model.getWaypoints().size();
        if (filledPath) drawPath(canvas, dataRange, numWaypoints);
        drawLine(canvas, dataRange, numWaypoints);
        drawGrid(canvas);
    }

    /**
     * Draws a (filled) path.
     * @param canvas the canvas to draw on
     * @param dataRange the DataRange to use
     * @param numWaypoints the number of points in the graph
     */
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

    /**
     * Draws a line.
     * @param canvas the canvas to draw on
     * @param dataRange the DataRange to use
     * @param numWaypoints the number of points in the graph
     */
    private void drawLine(Canvas canvas, DataRange dataRange, int numWaypoints) {
        for (int i = 1; i < numWaypoints; i++) {
            Point start = getDataPoint(numWaypoints, dataRange, i - 1);
            Point end = getDataPoint(numWaypoints, dataRange, i);
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint);
        }
    }

    /**
     * Returns a (2D) Point given the index of a waypoint.
     */
    private Point getDataPoint(int numWaypoints, DataRange dataRange, int index) {
        return new Point((int) getPathX(numWaypoints, index), (int) getPathY(dataRange, index));
    }

    /**
     * Gets the y-coordinate of a Point given index of a Waypoint.
     * @param i the index of the Waypoint
     * @return a new Point
     */
    private float getPathY(DataRange dataRange, int i) {
        Waypoint waypoint = model.getWaypoints().get(i);
        double currentValue = vertData == SPEED ? waypoint.getCurrentSpeed() : waypoint.getAltitude();
        return getYPosition(dataRange, currentValue);
    }

    /**
     * Gets the y-coordinate of a Point given a data value.
     * @param currentValue the data value
     * @return a new Point
     */
    private float getYPosition(DataRange dataRange, double currentValue) {
        double range = dataRange.getMaxValue() - dataRange.getMinValue();
        return (float) ((dataRange.getMaxValue() - currentValue) / range) * getPathMaxVertExtent()
                + AXIS_PADDING;
    }

    /**
     * Returns the max drawable vertical extent.
     * @return the max drawable vertical extent
     */
    private float getPathMaxVertExtent() {
        return getGridHeight() - PATH_PADDING;
    }

    /**
     * Returns the grid's height.
     * @return the grid's height
     */
    private float getGridHeight() {
        return getHeight() - AXIS_PADDING;
    }

    /**
     * Gets the x-coordinate of a Point given the index of a Waypoint.
     * @param waypoints
     * @param i the index
     */
    private float getPathX(int waypoints, float i) {
        return i / --waypoints * (getWidth() - PATH_PADDING) + PATH_PADDING;
    }

    /**
     * Draws the chart's grid (i.e. the axes). Could be extended with e.g. more horizontal lines.
     */
    private void drawGrid(Canvas canvas) {
        canvas.drawLine(AXIS_PADDING, 0, AXIS_PADDING, getHeight(), gridPaint);
        canvas.drawLine(0, getGridHeight(), getWidth(), getGridHeight(), gridPaint);
    }

    /**
     * Initializes the ChartView.
     * @param filledPath draw a filled path or not?
     */
    public void init(LogViewModel model, DataRange dataRange, VerticalData verticalData,
                     boolean filledPath) {
        this.dataRange = dataRange;
        this.model = model;
        this.vertData = verticalData;
        this.filledPath = filledPath;
        initPaints();
    }
}
