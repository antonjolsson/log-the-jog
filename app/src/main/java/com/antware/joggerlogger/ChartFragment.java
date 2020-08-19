package com.antware.joggerlogger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ChartFragment extends Fragment {

    private FragmentChartBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = this.getArguments();
        assert bundle != null;
        HorizontalData horizData = (HorizontalData) bundle.get(HORIZ_DATA_KEY);
        VerticalData verticalData = (VerticalData) bundle.get(VERT_DATA_KEY);
        LogViewModel model = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
        /*double maxValue = getStatMaxValue(model, verticalData);*/
        StatMaxMinValues maxMinValues = StatMaxMinValues.getStatMaxMinValues(model,
                verticalData, true, verticalData == ELEVATION);
        setTickLabels(maxMinValues, binding.vertLabelsLayout);
        if (horizData == DURATION) {
            setDurTickLabels(binding.horizLabelsLayout, model);
            binding.horizLabel.setText(R.string.time);
        }
        else {
            double distance = Objects.requireNonNull(model.getDistance().getValue());
            setTickLabels(new StatMaxMinValues(distance, 0), binding.horizLabelsLayout);
            binding.horizLabel.setText(R.string.km);
        }
        binding.chartView.init(model, maxMinValues, verticalData);
    }

    private double getStatMaxValue(LogViewModel model, VerticalData vertData) {
        double max = 0;
        for (Waypoint point : model.getWaypoints()) {
            double value = vertData == SPEED ? point.getCurrentSpeed() : point.getLocation().getAltitude();
            if (value > max)
                max = value;
        }
        return max;
    }

    private void setTickLabels(StatMaxMinValues maxMinValues, ConstraintLayout labelsLayout) {
        for (int i = 0; i < labelsLayout.getChildCount(); i++) {
            double range = maxMinValues.maxValue - maxMinValues.minValue;
            double value = (double) i / (labelsLayout.getChildCount() - 1) * range + maxMinValues.minValue;
            TextView tickLabel = (TextView) labelsLayout.getChildAt(i);
            tickLabel.setText(String.format(Locale.ENGLISH, "%.1f", value));
        }
    }

    private void setDurTickLabels(ConstraintLayout layout, LogViewModel model) {
        LogViewModel.Duration duration = model.getDuration().getValue();
        assert duration != null;
        int seconds = duration.hours * 3600 + duration.minutes * 60 + duration.seconds;
        for (int i = 0; i < layout.getChildCount(); i++) {
            LogViewModel.Duration interval = model.getDurationFromMs((long) (1000 * seconds *
                    (float) i / (layout.getChildCount() + 1)));
            TextView tickLabel = (TextView) layout.getChildAt(i);
            tickLabel.setText(StatsFragment.Companion.getDurationText(interval));
        }
    }

    public static class StatMaxMinValues {

        public StatMaxMinValues(double maxValue, double minValue) {
            this.maxValue = maxValue;
            this.minValue = minValue;
        }

        public StatMaxMinValues() {}

        public double getMaxValue() {
            return maxValue;
        }

        public double getMinValue() {
            return minValue;
        }

        private double maxValue = Double.MIN_VALUE, minValue = Double.MAX_VALUE;

        public static StatMaxMinValues getStatMaxMinValues(LogViewModel model, VerticalData dataType,
                                                           boolean getMaxValue, boolean getMinValue) {
            StatMaxMinValues maxMinValues = new StatMaxMinValues();
            if (!getMaxValue) maxMinValues.maxValue = 0;
            if (!getMinValue) maxMinValues.minValue = 0;
            for (Waypoint point : model.getWaypoints()) {
                double value = dataType == SPEED ? point.getCurrentSpeed() : point.getLocation().getAltitude();
                if (getMaxValue && value > maxMinValues.maxValue)
                    maxMinValues.maxValue = value;
                if (getMinValue && value < maxMinValues.minValue)
                    maxMinValues.minValue = value;
            }
            return maxMinValues;
        }
    }
}
