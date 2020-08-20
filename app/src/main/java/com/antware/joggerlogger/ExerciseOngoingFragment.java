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
import androidx.transition.TransitionInflater;

import com.antware.joggerlogger.databinding.FragmentExerciseOngoingBinding;

public class ExerciseOngoingFragment extends Fragment {

    private boolean isViewCreated;
    private MapsFragment mapsFragment;
    private StatsFragment statsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_left));
        setExitTransition(inflater.inflateTransition(R.transition.fade));
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
        if (isViewCreated) {
            resetStats();
            return;
        }
        FragmentManager fragmentManager = getChildFragmentManager();
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
