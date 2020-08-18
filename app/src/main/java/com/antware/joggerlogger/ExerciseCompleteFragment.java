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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.antware.joggerlogger.databinding.FragmentExerciseCompleteBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

import static com.antware.joggerlogger.ExerciseCompleteFragment.HorizontalData.DURATION;
import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.*;

public class ExerciseCompleteFragment extends Fragment {

    private final static int NUM_DECIMALS_IN_DISTANCES = 2;

    enum HorizontalData {DURATION, DISTANCE}
    enum VerticalData {SPEED, ELEVATION}
    HorizontalData horizData = DURATION;

    private FragmentExerciseCompleteBinding mainBinding;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainBinding = FragmentExerciseCompleteBinding.inflate(inflater, container, false);

        return mainBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LogViewModel model = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
        showNumericStats(model);

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment speedFragment = getChartFragment(SPEED, DURATION);
        Fragment elevFragment = getChartFragment(ELEVATION, DURATION);
        transaction.add(R.id.speedLayout, speedFragment).add(R.id.elevationLayout, elevFragment);
        transaction.commit();
    }

    private ChartFragment getChartFragment(VerticalData vertData, HorizontalData horizData) {
        ChartFragment fragment = new ChartFragment();

        Bundle args = new Bundle();
        args.putSerializable("verticalData", vertData);
        args.putSerializable("horizData", horizData);
        fragment.setArguments(args);

        return fragment;
    }

    private void showNumericStats(LogViewModel model) {
        String durationText = StatsFragment.
                Companion.getDurationText(Objects.requireNonNull(model.getDuration().getValue()));
        mainBinding.durationLabelView.setText(durationText);
        String distanceText = String.valueOf(model.getDistance().getValue());
        mainBinding.distanceView.setText(truncateDecimals(distanceText));
        String avgSpeedText = String.valueOf(model.getAvgSpeed().getValue());
        mainBinding.completeAvgSpdView.setText(truncateDecimals(avgSpeedText));
        LogViewModel.Duration pace = model.getPace().getValue();
        assert pace != null;
        String paceText = getResources().getString(R.string.paceData, pace.minutes, pace.seconds);
        mainBinding.completePaceView.setText(paceText);
    }

    // TODO: Round decimals
    @NotNull
    private String truncateDecimals(String distanceText) {
        return distanceText.substring(0, distanceText.length() >= NUM_DECIMALS_IN_DISTANCES + 3 ?
                NUM_DECIMALS_IN_DISTANCES + 2 : distanceText.length());
    }
}
