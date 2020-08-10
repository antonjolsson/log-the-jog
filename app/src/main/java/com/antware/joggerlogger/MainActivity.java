package com.antware.joggerlogger;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.antware.joggerlogger.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ControlFragment controlFragment = new ControlFragment();
        MapsFragment mapsFragment = new MapsFragment();
        StatsFragment statsFragment = new StatsFragment();
        transaction.add(R.id.exerciseOngoingBottom, controlFragment).add(R.id.exerciseOngoingTop, mapsFragment)
                .add(R.id.exerciseOngoingCenter, statsFragment).commit();

        LogViewModel model = new ViewModelProvider(this).get(LogViewModel.class);
        model.getStatus().observe(this, status -> {
            if (status == LogViewModel.ExerciseStatus.STOPPED_AFTER_PAUSED)
                onExerciseStopped(fragmentManager, controlFragment, mapsFragment, statsFragment);
        });
    }

    private void onExerciseStopped(FragmentManager fragmentManager, ControlFragment controlFragment,
                                   MapsFragment mapsFragment, StatsFragment statsFragment) {

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ExerciseCompleteFragment fragment = new ExerciseCompleteFragment();
        transaction.replace(R.id.mainFrameLayout, fragment);
        transaction.hide(controlFragment).hide(statsFragment);
        transaction.remove(mapsFragment).commit();
        fragmentManager.executePendingTransactions();
        transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.mapFrame, mapsFragment);
        transaction.addToBackStack(null);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        transaction.commit();

        binding.exerciseOngoingLayout.divider.setVisibility(View.GONE);
    }

}
