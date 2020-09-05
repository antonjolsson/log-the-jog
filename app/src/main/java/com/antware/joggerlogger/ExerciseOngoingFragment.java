package com.antware.joggerlogger;

import android.os.Bundle;
import android.util.Log;
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

import static android.view.View.GONE;

public class ExerciseOngoingFragment extends Fragment {

    public final static String TAG = "ExerciseOngoingFragment";

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
        FragmentManager fragmentManager = getChildFragmentManager();
        ((MainActivity) requireActivity()).setBarVisibility(true);

        ControlFragment controlFragment = (ControlFragment) fragmentManager.findFragmentByTag(ControlFragment.TAG);
        if (controlFragment == null) controlFragment = new ControlFragment();
        StatsFragment statsFragment = (StatsFragment) fragmentManager.findFragmentByTag(StatsFragment.TAG);
        if (statsFragment == null) statsFragment = new StatsFragment();
        MapsFragment mapsFragment = (MapsFragment) fragmentManager.findFragmentByTag(MapsFragment.TAG);
        if (mapsFragment == null) mapsFragment = new MapsFragment();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.exerciseOngoingBottom, controlFragment, ControlFragment.TAG);
        transaction.replace(R.id.exerciseOngoingTop, mapsFragment, MapsFragment.TAG);
        transaction.replace(R.id.exerciseOngoingCenter, statsFragment, StatsFragment.TAG);
        transaction.addToBackStack(null).commit();
    }

}
