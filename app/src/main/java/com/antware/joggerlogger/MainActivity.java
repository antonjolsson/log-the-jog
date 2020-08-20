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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        ExerciseOngoingFragment exerciseOngoingFragment = new ExerciseOngoingFragment();
        transaction.add(R.id.mainFrameLayout, exerciseOngoingFragment).addToBackStack(null).commit();
        LogViewModel model = new ViewModelProvider(this).get(LogViewModel.class);
        model.getStatus().observe(this, status -> {
            if (status == LogViewModel.ExerciseStatus.STOPPED_AFTER_PAUSED)
                onExerciseStopped(fragmentManager);
        });
    }

    private void onExerciseStopped(FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ExerciseCompleteFragment fragment = new ExerciseCompleteFragment();
        transaction.replace(R.id.mainFrameLayout, fragment).addToBackStack(null).commit();
    }

}
