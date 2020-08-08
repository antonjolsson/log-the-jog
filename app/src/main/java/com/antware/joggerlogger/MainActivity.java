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
        transaction.add(R.id.controlFragment, controlFragment).add(R.id.mapFragment, mapsFragment)
                .add(R.id.fragment3, statsFragment).commit();


        LogViewModel model = new ViewModelProvider(this).get(LogViewModel.class);
        model.getStatus().observe(this, status -> {
            if (status == LogViewModel.ExerciseStatus.STOPPED_AFTER_PAUSED)
                onExerciseStopped(transaction);
        });

    }

    private void onExerciseStopped(FragmentTransaction fragmentTransaction) {

        ExerciseCompleteFragment fragment = new ExerciseCompleteFragment();
        fragmentTransaction.replace(R.id.controlLayout, fragment);
        fragmentTransaction.commit();

    }
}
