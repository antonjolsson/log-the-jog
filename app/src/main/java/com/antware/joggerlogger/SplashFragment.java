package com.antware.joggerlogger;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;
import androidx.transition.TransitionListenerAdapter;

import com.antware.joggerlogger.databinding.FragmentSplashBinding;

/**
 * Fragment responsible for displaying the splash screen.
 * @author Anton J Olsson
 */
public class SplashFragment extends Fragment {

    private static final long ANIM_DURATION = 2000;
    private FragmentSplashBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSplashBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Fades in the logo.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ValueAnimator fadeAnim = ObjectAnimator.ofFloat(binding.logoView, "alpha",
                0f, 1f);
        fadeAnim.setDuration(ANIM_DURATION);
        fadeAnim.start();
    }

    /**
     * Sets transitions and hides system bars.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity mainActivity = (MainActivity) requireActivity();
        mainActivity.setBarVisibility(false);
        TransitionInflater inflater = TransitionInflater.from(requireContext());
        setEnterTransition(inflater.inflateTransition(R.transition.slide_left));
        setExitTransition(inflater.inflateTransition(R.transition.slide_left));
        Transition transition = (Transition) getExitTransition();
        assert transition != null;
        transition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                mainActivity.setBarVisibility(true);
            }
        });
    }
}
