package com.antware.joggerlogger;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.antware.joggerlogger.databinding.ActivityMainBinding;

import org.jetbrains.annotations.NotNull;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.antware.joggerlogger.MyLocation2.REQUEST_LOCATION;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        if (!locationPermitted()) {
            ActivityCompat.requestPermissions(this,
                new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
             return;
        }

        initFragments();
    }

    private void initFragments() {
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

    public boolean locationPermitted() {
       return ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) ==
               PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) ==
                       PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) initFragments();
            else onBackPressed();
        }
    }

    private void onExerciseStopped(FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ExerciseCompleteFragment fragment = new ExerciseCompleteFragment();
        transaction.replace(R.id.mainFrameLayout, fragment).addToBackStack(null).commit();
    }

}
