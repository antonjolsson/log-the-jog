package com.antware.joggerlogger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.antware.joggerlogger.databinding.FragmentExerciseCompleteBinding;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ExerciseCompleteFragment extends Fragment {

    private FragmentExerciseCompleteBinding binding;
    private final static int NUM_DECIMALS_IN_DISTANCES = 2;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExerciseCompleteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LogViewModel model = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
        showNumericStats(model);
        drawSpeedCurve(model);
        drawElevationCurve(model);
    }

    private void drawElevationCurve(LogViewModel model) {
        binding.elevationChart.elevationChart.initModel(model);
    }

    private void drawSpeedCurve(LogViewModel model) {
        binding.speedChart.speedChart.initModel(model);
    }

    private void showNumericStats(LogViewModel model) {
        String durationText = StatsFragment.
                Companion.getDurationText(Objects.requireNonNull(model.getDuration().getValue()));
        binding.durationLabelView.setText(durationText);
        String distanceText = String.valueOf(model.getDistance().getValue());
        binding.distanceView.setText(truncateDecimals(distanceText));
        String avgSpeedText = String.valueOf(model.getAvgSpeed().getValue());
        binding.completeAvgSpdView.setText(truncateDecimals(avgSpeedText));
        LogViewModel.Duration pace = model.getPace().getValue();
        assert pace != null;
        String paceText = getResources().getString(R.string.paceData, pace.minutes, pace.seconds);
        binding.completePaceView.setText(paceText);
    }

    // TODO: Round decimals
    @NotNull
    private String truncateDecimals(String distanceText) {
        return distanceText.substring(0, distanceText.length() >= NUM_DECIMALS_IN_DISTANCES + 3 ?
                NUM_DECIMALS_IN_DISTANCES + 2 : distanceText.length());
    }
}
