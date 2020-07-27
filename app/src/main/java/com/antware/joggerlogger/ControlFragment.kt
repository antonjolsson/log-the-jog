package com.antware.joggerlogger

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.antware.joggerlogger.LogViewModel.ExerciseStatus.*
import com.antware.joggerlogger.databinding.FragmentControlBinding
import kotlinx.android.synthetic.main.fragment_control.*

class ControlFragment : Fragment(R.layout.fragment_control) {

    private val model: LogViewModel by activityViewModels()

    private var _binding: FragmentControlBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startButton.setOnClickListener(clickListener)
        binding.pauseButton.setOnClickListener(clickListener)
    }

    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.startButton -> startButtonPressed(view)
            R.id.pauseButton -> pauseButtonPressed(view)
        }
    }

    private fun pauseButtonPressed(view: View?) {
        when (model.exerciseStatus) {
            PAUSED -> {
                resetPauseButton(view)
            }
            else -> {
                if (view is Button) {
                    view.text = getText(R.string.resume)
                    view.setTextColor(ContextCompat.getColor(requireActivity(), R.color.colorBackground))
                }
                view?.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.colorPrimaryDark))
            }
        }
        model.pauseButtonPressed()
    }

    private fun resetPauseButton(view: View?) {
        if (view is Button) {
            view.text = getText(R.string.pause)
            view.setTextColor(ContextCompat.getColor(requireActivity(), if(model.exerciseStatus == STOPPED ||
                model.exerciseStatus == STOPPED_AFTER_PAUSED) R.color.colorDisabled
            else R.color.colorPrimaryDark))
        }
        view?.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.colorBackground))
    }

    private fun startButtonPressed(view: View?) {
        binding.startButton.setBackgroundColor(
            ContextCompat.getColor(requireActivity(), if (model.exerciseStatus == STOPPED ||
                model.exerciseStatus == STOPPED_AFTER_PAUSED) R.color.colorAccent
            else R.color.colorPrimaryDark)
        )
        if (view is Button) {
            val text: String = if (model.exerciseStatus == STOPPED || model.exerciseStatus == STOPPED_AFTER_PAUSED) getText(R.string.stop) as String
            else getText(R.string.start) as String
            view.text = text
        }
        model.startButtonPressed()
        resetPauseButton(binding.pauseButton)
        if (model.exerciseStatus == STOPPED || model.exerciseStatus == STOPPED_AFTER_PAUSED) {
            binding.pauseButton.isEnabled = false
            binding.pauseButton.elevation = 0F
        }
        else {
            binding.pauseButton.isEnabled = true
            binding.pauseButton.elevation = 4F
        }
        binding.pauseButton.isEnabled = model.exerciseStatus != STOPPED && model.exerciseStatus != STOPPED_AFTER_PAUSED

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}