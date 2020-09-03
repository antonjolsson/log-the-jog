package com.antware.joggerlogger;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.antware.joggerlogger.databinding.ActivityMainBinding;

import org.jetbrains.annotations.NotNull;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.antware.joggerlogger.LocationService.REQUEST_LOCATION;

public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_SCREEN_DURATION = 3000;
    private static final boolean SHOW_SPLASH_SCREEN = true;
    private static final String TAG = MainActivity.class.getSimpleName();
    private LogViewModel model;
    private Context locationContext;
    private LocationService.BestLocationResult locationResult;
    private MainActivity mainActivity = this;
    private Intent locationIntent;

    private LocationService locationService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            locationService = ((LocationService.ServiceBinder) iBinder).getService();
            locationService.initService(mainActivity, getApplicationContext());
            locationService.setSaveLocations(false);
            if (locationContext != null && locationResult != null) {
                locationService.getLocation(locationContext, locationResult);
                locationContext = null;
                locationResult = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            locationService = null;
        }

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        model = new ViewModelProvider(this).get(LogViewModel.class);

        FragmentManager fragmentManager = getSupportFragmentManager();
        ExerciseOngoingFragment ongoingFragment = (ExerciseOngoingFragment)
                fragmentManager.findFragmentByTag(ExerciseOngoingFragment.TAG);
        if (ongoingFragment == null) ongoingFragment = new ExerciseOngoingFragment();

        if (!model.isReloaded()) {
            if (SHOW_SPLASH_SCREEN) showSplashScreen(fragmentManager);
            if (!locationPermitted()) {
                ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
                 return;
            }
            if (SHOW_SPLASH_SCREEN){
                Handler handler = new Handler();
                ExerciseOngoingFragment finalOngoingFragment = ongoingFragment;
                Runnable r = () -> {
                    initFragments(fragmentManager, finalOngoingFragment);
                };
                handler.postDelayed(r, SPLASH_SCREEN_DURATION);
            }
            else initFragments(fragmentManager, ongoingFragment);
        }
        else initFragments(fragmentManager, ongoingFragment);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onResume() {
        super.onResume();
        initLocationService();
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initLocationService() {
        if (!isLocationServiceRunning()) {
            locationIntent = new Intent(this, LocationService.class);
            startForegroundService(locationIntent);
        }
        bindService(new Intent(this, LocationService.class), serviceConnection,
                Context.BIND_ABOVE_CLIENT);
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
        if (!model.isReloaded()) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.mainFrameLayout, exerciseOngoingFragment, ExerciseOngoingFragment.TAG)
                    .commit();
        }
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
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) initFragments(getSupportFragmentManager(),
                    new ExerciseOngoingFragment());
            else onBackPressed();
        }
    }

    private void onExerciseStopped(FragmentManager fragmentManager) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        ExerciseCompleteFragment fragment = (ExerciseCompleteFragment)
                fragmentManager.findFragmentByTag(ExerciseCompleteFragment.TAG);
        if (fragment == null) fragment = new ExerciseCompleteFragment();
        transaction.replace(R.id.mainFrameLayout, fragment, ExerciseCompleteFragment.TAG);
        if (fragmentManager.getBackStackEntryCount() == 0)
                transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationService != null) locationService.setSaveLocations(true);
        model.saveTimerVars();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) model.reset();
        else stopService(locationIntent);
        super.onBackPressed();
    }

    public void addLocationResultListener(Context context, LocationService.BestLocationResult locationResult) {
        if (locationService != null)
            locationService.getLocation(context, locationResult);
        else {
            locationContext = context;
            this.locationResult = locationResult;
        }
    }
}
