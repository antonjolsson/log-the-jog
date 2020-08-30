package com.antware.joggerlogger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionInflater;

import com.antware.joggerlogger.databinding.FragmentExerciseOngoingBinding;

public class ExerciseOngoingFragment extends Fragment {

    public final static String TAG = "exerciseOngoingFragment";
    private boolean isViewCreated;
    private MapsFragment mapsFragment;
    private StatsFragment statsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setExitTransition(inflater.inflateTransition(R.transition.slide_left));
        setReenterTransition(inflater.inflateTransition(R.transition.slide_left));
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentExerciseOngoingBinding binding = FragmentExerciseOngoingBinding.
                inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LogViewModel model = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
        FragmentManager fragmentManager = getChildFragmentManager();
        ((MainActivity) requireActivity()).setBarVisibility(true);
        if (isViewCreated) {
            resetStats();
            return;
        }
        if (model.isReloaded()) return;

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ControlFragment controlFragment = new ControlFragment();
        statsFragment = new StatsFragment();
        mapsFragment = new MapsFragment();
        transaction.add(R.id.exerciseOngoingBottom, controlFragment).add(R.id.exerciseOngoingTop, mapsFragment)
                .add(R.id.exerciseOngoingCenter, statsFragment);
        isViewCreated = true;
        transaction.commit();
    }

    private void resetStats() {
        mapsFragment.reset();
        statsFragment.setReset(true);
    }

}
