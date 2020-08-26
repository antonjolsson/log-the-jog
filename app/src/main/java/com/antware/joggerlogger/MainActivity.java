package com.antware.joggerlogger;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import static com.antware.joggerlogger.LocationManager.REQUEST_LOCATION;

public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_DURATION = 3000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        FragmentManager fragmentManager = getSupportFragmentManager();

        showSplashScreen(fragmentManager);

        if (!locationPermitted()) {
            ActivityCompat.requestPermissions(this,
                new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
             return;
        }

        Handler handler = new Handler();
        Runnable r = () -> {
            initFragments(fragmentManager);
        };
        handler.postDelayed(r, SPLASH_SCREEN_DURATION);
    }

    private void showSplashScreen(FragmentManager fragmentManager) {
        SplashFragment splashFragment = new SplashFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.mainFrameLayout, splashFragment).commit();
    }

    public void setBarVisibility(boolean showBars) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int visibility = showBars ? View.SYSTEM_UI_FLAG_VISIBLE : View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(visibility);
        }
        else {
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(showBars ? View.VISIBLE : View.GONE);
        }
    }

    private void initFragments(FragmentManager fragmentManager) {
        fragmentManager.addOnBackStackChangedListener(() -> {
            if (fragmentManager.getBackStackEntryCount() == 0)
                onBackPressed();
        });
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        ExerciseOngoingFragment exerciseOngoingFragment = new ExerciseOngoingFragment();
        transaction.replace(R.id.mainFrameLayout, exerciseOngoingFragment).addToBackStack(null).commit();
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
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) initFragments(getSupportFragmentManager());
            else onBackPressed(); // TODO: make app usable without permission?
        }
    }

    private void onExerciseStopped(FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ExerciseCompleteFragment fragment = new ExerciseCompleteFragment();
        transaction.replace(R.id.mainFrameLayout, fragment).addToBackStack(null).commit();
    }

}
