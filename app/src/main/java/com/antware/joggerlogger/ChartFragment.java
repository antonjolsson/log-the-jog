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
        HorizontalData horizData = (HorizontalData) bundle.get("horizData");
        VerticalData verticalData = (VerticalData) bundle.get("verticalData");
        LogViewModel model = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
        double maxValue = getStatMaxValue(model, verticalData);
        setVertTickLabels(maxValue, binding.vertLabelsLayout);
        setDurTickLabels(binding.horizLabelsLayout, model);
        binding.chartView.init(model, maxValue, verticalData);
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

    private void setVertTickLabels(double maxValue, ConstraintLayout labelsLayout) {
        for (int i = 0; i < labelsLayout.getChildCount(); i++) {
            double value = (double) i / (labelsLayout.getChildCount() - 1) * maxValue;
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
}
