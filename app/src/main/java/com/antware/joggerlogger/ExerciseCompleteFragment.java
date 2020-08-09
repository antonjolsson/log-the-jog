package com.antware.joggerlogger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.antware.joggerlogger.databinding.FragmentExerciseCompleteBinding;

import org.jetbrains.annotations.NotNull;
import java.util.Objects;

public class ExerciseCompleteFragment extends Fragment {

    private FragmentExerciseCompleteBinding binding;

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
        setStatistics(model);
    }

    private void setStatistics(LogViewModel model) {
        String durationText = StatsFragment.
                Companion.getDurationText(Objects.requireNonNull(model.getDuration().getValue()));
        binding.durationLabelView.setText(durationText);
        binding.distanceView.setText(String.valueOf(model.getDistance().getValue()).substring(0, 4));
        binding.completeAvgSpdView.setText(String.valueOf(model.getAvgSpeed().getValue()).substring(0, 4));
        LogViewModel.Duration pace = model.getPace().getValue();
        assert pace != null;
        String paceText = getResources().getString(R.string.paceData, pace.minutes, pace.seconds);
        binding.completePaceView.setText(paceText);
    }
}
