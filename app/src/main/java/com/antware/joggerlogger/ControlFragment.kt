package com.antware.joggerlogger

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.transition.TransitionInflater
import com.antware.joggerlogger.LogViewModel.ExerciseStatus.*
import com.antware.joggerlogger.databinding.FragmentControlBinding

class ControlFragment : Fragment(R.layout.fragment_control) {

    private val model: LogViewModel by activityViewModels()

    private var _binding: FragmentControlBinding? = null
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
        binding.stopButton.setOnClickListener(clickListener)
        binding.stopButton.visibility = View.GONE
    }

    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.startButton -> startButtonPressed()
            R.id.stopButton -> stopButtonPressed()
        }
    }

    private fun stopButtonPressed() {
        model.stopButtonPressed();
        binding.stopButton.visibility = View.GONE
        binding.startButton.text = getText(R.string.start)
    }

    private fun startButtonPressed() {
        if (model.exerciseStatus == STARTED || model.exerciseStatus == RESUMED){
            binding.stopButton.visibility = View.VISIBLE
            binding.startButton.text = getText(R.string.resume)
        }
        else {
            binding.stopButton.visibility = View.GONE
            binding.startButton.text = getText(R.string.pause)
        }
        model.startButtonPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}