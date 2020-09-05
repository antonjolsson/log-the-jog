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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.antware.joggerlogger.ExerciseCompleteFragment.HorizontalData.DISTANCE;
import static com.antware.joggerlogger.ExerciseCompleteFragment.VerticalData.*;

public class ExerciseCompleteFragment extends Fragment implements Toolbar.OnMenuItemClickListener{

    public final static String TAG = "exerciseCompleteFrag";
    public final static String TWO_DECIMALS_FORMAT = "%.2f";
    public static final String VERT_DATA_KEY = "verticalData";
    public static final String HORIZ_DATA_KEY = "horizData";
    private static final int NUM_OF_CHART_FRAGMENTS = 2;
    private LogViewModel model;
    private ChartFragment speedFragment;
    private List<Integer> vertLayoutWidths = new ArrayList<>();
    private ChartFragment elevFragment;

    public void addVertLayoutWidth(int vertLayoutWidth) {
        vertLayoutWidths.add(vertLayoutWidth);
        if (vertLayoutWidths.size() == NUM_OF_CHART_FRAGMENTS) {
            int maxWidth = 0;
            for (int width : vertLayoutWidths) {
                if (width > maxWidth) maxWidth = width;
            }
            if (maxWidth > 0) {
                speedFragment.setVertLayoutWidth(maxWidth);
                elevFragment.setVertLayoutWidth(maxWidth);
            }
        }
    }

    enum HorizontalData {DURATION, DISTANCE}
    enum VerticalData {SPEED, ELEVATION}
    HorizontalData horizData = DISTANCE;

    private FragmentExerciseCompleteBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_right));
        setReturnTransition(inflater.inflateTransition(R.transition.slide_right));
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
        speedFragment = getChartFragment(SPEED, horizData);
        elevFragment = getChartFragment(ELEVATION, horizData);
        Fragment mapFragment = fragmentManager.findFragmentByTag(MapsFragment.TAG);
        if (mapFragment == null) mapFragment = new MapsFragment();
        transaction.replace(R.id.speedLayout, speedFragment).replace(R.id.elevationLayout, elevFragment)
        .replace(R.id.mapFrame, mapFragment, MapsFragment.TAG);
        transaction.commit();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        FileManager fileManager = FileManager.getInstance();
        fileManager.save(model, requireActivity());
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
        String distanceText = String.format(Locale.ENGLISH, TWO_DECIMALS_FORMAT, model.getDistance().getValue());
        binding.distanceView.setText(distanceText);
        String avgSpeedText =  String.format(Locale.ENGLISH, TWO_DECIMALS_FORMAT, model.getAvgSpeed().getValue());
        binding.completeAvgSpdView.setText(avgSpeedText);
        Duration pace = model.getPace().getValue();
        assert pace != null;
        String paceText = String.format(Locale.ENGLISH, "%1$d:%2$02d", pace.minutes, pace.seconds);
        binding.completePaceView.setText(paceText);

        binding.caloriesLayout.setVisibility(View.GONE);
        binding.activityLayout.setVisibility(View.GONE);
    }

}
