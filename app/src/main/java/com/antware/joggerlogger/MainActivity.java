package com.antware.joggerlogger;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
import static com.antware.joggerlogger.LogLocationManager.REQUEST_LOCATION;

public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_DURATION = 3000;
    private static final boolean SHOW_SPLASH_SCREEN = false;
    private static final String TAG = "MainActivity";
    private LogViewModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Created!");
        printFragmentBackStackCount(getSupportFragmentManager(), TAG);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        model = new ViewModelProvider(this).get(LogViewModel.class);

        FragmentManager fragmentManager = getSupportFragmentManager();
        ExerciseOngoingFragment exerciseOngoingFragment = new ExerciseOngoingFragment();

        if (!model.isReloaded()) {

            if (SHOW_SPLASH_SCREEN) showSplashScreen(fragmentManager);

            if (!locationPermitted()) {
                ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                 return;
            }

            if (SHOW_SPLASH_SCREEN){
                Handler handler = new Handler();
                Runnable r = () -> {
                    initFragments(fragmentManager, exerciseOngoingFragment);
                };
                handler.postDelayed(r, SPLASH_SCREEN_DURATION);
            }
            else initFragments(fragmentManager, exerciseOngoingFragment);
        }
        else initFragments(fragmentManager, exerciseOngoingFragment);
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

    private void initFragments(FragmentManager fragmentManager, ExerciseOngoingFragment exerciseOngoingFragment) {
        fragmentManager.addOnBackStackChangedListener(() -> {
            if (fragmentManager.getBackStackEntryCount() == 0)
                onBackPressed();
        });
        if (!model.isReloaded()) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.mainFrameLayout, exerciseOngoingFragment, ExerciseOngoingFragment.TAG)
                    .addToBackStack(null).commit();
        }
        model.getStatus().observe(this, status -> {
            if (status == LogViewModel.ExerciseStatus.STOPPED_AFTER_PAUSED &&
                    fragmentManager.getBackStackEntryCount() == 1)
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
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) initFragments(getSupportFragmentManager(),
                    new ExerciseOngoingFragment());
            else onBackPressed(); // TODO: make app usable without permission?
        }
    }

    private void onExerciseStopped(FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ExerciseCompleteFragment fragment = new ExerciseCompleteFragment();
        transaction.replace(R.id.mainFrameLayout, fragment, ExerciseCompleteFragment.TAG).
                addToBackStack(null).commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        model.saveState();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (getSupportFragmentManager().getBackStackEntryCount() == 1)
            model.reset();

    }

    static void printFragmentBackStackCount(FragmentManager fragmentManager,
                                                    String tag) {
        int backStackCount = fragmentManager.getBackStackEntryCount();
        Log.d("Fragment backstack", tag + ". Fragment count: " + backStackCount);
        for (int i = 0; i < backStackCount; i++) {
            Log.d("Fragment backstack", "Fragment " + i + ": " + fragmentManager
                    .getBackStackEntryAt(i).toString());
        }
    }
}
