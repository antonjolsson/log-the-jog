package com.antware.joggerlogger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.antware.joggerlogger.ExerciseCompleteFragment.HorizontalData;
import com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData;
import com.antware.joggerlogger.databinding.FragmentChartBinding;

import java.util.Locale;
import java.util.Objects;

import static com.antware.joggerlogger.ExerciseCompleteFragment.HORIZ_DATA_KEY;
import static com.antware.joggerlogger.ExerciseCompleteFragment.HorizontalData.DURATION;
import static com.antware.joggerlogger.ExerciseCompleteFragment.VERT_DATA_KEY;
import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.ELEVATION;
import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.SPEED;

/**
 * Class responsible for displaying a chart.
 * @author Anton J Olsson
 */
public class ChartFragment extends Fragment {

    public final static String TAG = ChartFragment.class.getSimpleName();
    private FragmentChartBinding binding;
    private final static double ELEVATION_PATH_RANGE = 0.9;
    private int vertLayoutWidth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Determines the type of data to display and the vertical axis range. Sets the tick and
     * axis labels.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = this.getArguments();
        assert bundle != null;
        HorizontalData horizData = (HorizontalData) bundle.get(HORIZ_DATA_KEY);
        VerticalData verticalData = (VerticalData) bundle.get(VERT_DATA_KEY);
        LogViewModel model = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
        DataRange dataRange = DataRange.getDataRange(model,
                verticalData, true,true);
        if (verticalData == ELEVATION) {
            dataRange.setRangeExtent(1 / ELEVATION_PATH_RANGE);
        }
        setTickLabels(dataRange, binding.vertLabelsLayout);
        if (horizData == DURATION) {
            setDurTickLabels(binding.horizLabelsLayout, model);
            binding.horizLabel.setText(R.string.time);
        }
        else {
            double distance = Objects.requireNonNull(model.getDistance().getValue());
            setTickLabels(new DataRange(distance, 0), binding.horizLabelsLayout);
            binding.horizLabel.setText(R.string.km);
        }
        binding.chartView.init(model, dataRange, verticalData, verticalData == ELEVATION);

        addVertLayoutObserver();
    }

    /**
     * Computes the width of the vertical axis (including tick marks), so that both charts on the results
     * screen will have the same axis width.
     */
    private void addVertLayoutObserver() {
        ViewTreeObserver vto = binding.vertLabelsLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.vertLabelsLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                vertLayoutWidth  = binding.vertLabelsLayout.getMeasuredWidth();
                assert getParentFragment() != null;
                ((ExerciseCompleteFragment) getParentFragment()).addVertLayoutWidth(vertLayoutWidth);
            }
        });
    }

    /**
     * Sets the tick labels on an axis, depending on the max and min values and the index.
     * @param maxMinValues the max and min values of the data
     * @param labelsLayout the axis' layout
     */
    private void setTickLabels(DataRange maxMinValues, ConstraintLayout labelsLayout) {
        for (int i = 0; i < labelsLayout.getChildCount(); i++) {
            double range = maxMinValues.maxValue - maxMinValues.minValue;
            double value = (double) i / (labelsLayout.getChildCount() - 1) * range + maxMinValues.minValue;
            TextView tickLabel = (TextView) labelsLayout.getChildAt(i);
            tickLabel.setText(String.format(Locale.ENGLISH, "%.1f", value));
        }
    }

    /**
     * Sets the tick labels on an axis representing data.
     * @param layout the axis' layout
     * @param model the viewmodel containing the data
     */
    private void setDurTickLabels(ConstraintLayout layout, LogViewModel model) {
        Duration duration = model.getDuration().getValue();
        assert duration != null;
        int seconds = duration.hours * 3600 + duration.minutes * 60 + duration.seconds;
        for (int i = 0; i < layout.getChildCount(); i++) {
            Duration interval = Duration.getDurationFromMs((long) (1000 * seconds *
                    (float) i / (layout.getChildCount() + 1)));
            TextView tickLabel = (TextView) layout.getChildAt(i);
            tickLabel.setText(StatsFragment.Companion.getDurationText(interval));
        }
    }

    /**
     * Sets the width of the vertical axis to align it with other charts.
     * @param width the new width of the axis
     */
    public void setVertLayoutWidth(int width) {
        ViewGroup.LayoutParams params = binding.vertLabelsLayout.getLayoutParams();
        params.width = width;
        binding.vertLabelsLayout.setLayoutParams(params);
    }

    /**
     * Class representing a data range an axis will display.
     */
    public static class DataRange {

        public DataRange(double maxValue, double minValue) {
            this.maxValue = maxValue;
            this.minValue = minValue;
        }

        public DataRange() {}

        /**
         * Gets the max value of the range.
         * @return the max value
         */
        public double getMaxValue() {
            return maxValue;
        }

        /**
         * Gets the min value of the range.
         * @return the min value
         */
        public double getMinValue() {
            return minValue;
        }

        private double maxValue = Double.MIN_VALUE, minValue = Double.MAX_VALUE;

        /**
         * Returns a new DataRange.
         * @param model the viewmodel
         * @param dataType the type of data
         * @param getMaxValue should the set's max value or 0 be used as the range's max value?
         * @param getMinValue should the set's min value or 0 be used as the range's min value?
         * @return a new DataRange
         */
        public static DataRange getDataRange(LogViewModel model, VerticalData dataType,
                                             boolean getMaxValue, boolean getMinValue) {
            DataRange dataRange = new DataRange();
            if (!getMaxValue) dataRange.maxValue = 0;
            if (!getMinValue) dataRange.minValue = 0;
            for (Waypoint point : model.getWaypoints()) {
                double value = dataType == SPEED ? point.getCurrentSpeed() : point.getAltitude();
                if (getMaxValue && value > dataRange.maxValue)
                    dataRange.maxValue = value;
                if (getMinValue && value < dataRange.minValue)
                    dataRange.minValue = value;
            }
            return dataRange;
        }

        /**
         * Multiplies the min value with some coefficient. Might be useful later/in other contexts.
         * @param coefficient the coefficient
         */
        public void setMinValue(double coefficient) {
            minValue *= coefficient;
        }

        /**
         * Multiplies the max value with some coefficient. Might be useful later/in other contexts.
         * @param coefficient the coefficient
         */
        public void setMaxValue(double coefficient) {
            maxValue *= coefficient;
        }

        /**
         * Increases or decreases the extent of the range by multiplying the extreme values with some
         * coefficient.
         * @param coefficient the coefficient.
         */
        public void setRangeExtent(double coefficient) {
            double average = (maxValue + minValue) / 2;
            maxValue = average + (maxValue - average) * coefficient;
            minValue = average - (average - minValue) * coefficient;
        }
    }
}
