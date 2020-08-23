package com.antware.joggerlogger;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionInflater;

import com.antware.joggerlogger.databinding.FragmentExerciseCompleteBinding;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

import static com.antware.joggerlogger.ExerciseCompleteFragment.HorizontalData.DISTANCE;
import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.*;

public class ExerciseCompleteFragment extends Fragment implements Toolbar.OnMenuItemClickListener{

    private final static String DISTANCE_FORMAT = "%.2f";
    private final static String AVG_SPEED_FORMAT = "%.1f";
    public static final String VERT_DATA_KEY = "verticalData";
    public static final String HORIZ_DATA_KEY = "horizData";
    private LogViewModel model;

    enum HorizontalData {DURATION, DISTANCE}
    enum VerticalData {SPEED, ELEVATION}
    HorizontalData horizData = DISTANCE;

    private FragmentExerciseCompleteBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_right));
        setExitTransition(inflater.inflateTransition(R.transition.fade));
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentExerciseCompleteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = new ViewModelProvider(requireActivity()).get(LogViewModel.class);

        binding.completeToolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        binding.completeToolbar.setOnMenuItemClickListener(this);
        showNumericStats(model);

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment speedFragment = getChartFragment(SPEED, horizData);
        Fragment elevFragment = getChartFragment(ELEVATION, horizData);
        Fragment mapFragment = new MapsFragment();
        transaction.add(R.id.speedLayout, speedFragment).add(R.id.elevationLayout, elevFragment)
        .add(R.id.mapFrame, mapFragment);
        transaction.commit();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        model.save();
        Log.d("CompleteFragment", "Exercise saved!");
        return true;
    }

    private ChartFragment getChartFragment(VerticalData vertData, HorizontalData horizData) {
        ChartFragment fragment = new ChartFragment();

        Bundle args = new Bundle();
        args.putSerializable(VERT_DATA_KEY, vertData);
        args.putSerializable(HORIZ_DATA_KEY, horizData);
        fragment.setArguments(args);

        return fragment;
    }

    private void showNumericStats(LogViewModel model) {
        String durationText = StatsFragment.
                Companion.getDurationText(Objects.requireNonNull(model.getDuration().getValue()));
        binding.durationLabelView.setText(durationText);
        String distanceText = String.format(Locale.ENGLISH, DISTANCE_FORMAT, model.getDistance().getValue());
        binding.distanceView.setText(distanceText);
        String avgSpeedText =  String.format(Locale.ENGLISH, AVG_SPEED_FORMAT, model.getAvgSpeed().getValue());
        binding.completeAvgSpdView.setText(avgSpeedText);
        LogViewModel.Duration pace = model.getPace().getValue();
        assert pace != null;
        String paceText = String.format(Locale.ENGLISH, "%1$d:%2$02d", pace.minutes, pace.seconds);
        binding.completePaceView.setText(paceText);
    }

}
