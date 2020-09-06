package com.antware.joggerlogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.antware.joggerlogger.LogViewModel.ExerciseStatus.*
import com.antware.joggerlogger.databinding.FragmentControlBinding

/**
 * Fragment for controls (buttons) during ongoing exercise.
 * @author Anton J Olsson
 */
class ControlFragment : Fragment(R.layout.fragment_control) {

    companion object {
        const val TAG: String = "ControlFragment"
    }

    private val model: LogViewModel by activityViewModels()

    private var _binding: FragmentControlBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.startButton.setOnClickListener(clickListener)
        binding.stopButton.setOnClickListener(clickListener)
        binding.stopButton.visibility = View.GONE

        model.isReloadedLiveData.observe(viewLifecycleOwner, { reloaded : Boolean ->
            if (reloaded && model.exerciseStatus != STOPPED && model.exerciseStatus != STOPPED_AFTER_PAUSED) {
                setControlState(model.exerciseStatus)
            }
        })
        setControlState(model.exerciseStatus)
    }

    private val clickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.startButton -> startButtonPressed()
            R.id.stopButton -> stopButtonPressed()
        }
    }

    private fun stopButtonPressed() {
        setControlState(STOPPED)
        model.stopButtonPressed();
    }

    private fun startButtonPressed() {
        when (model.exerciseStatus) {
            STARTED, RESUMED -> setControlState(PAUSED)
            PAUSED           -> setControlState(RESUMED)
            else             -> setControlState(STARTED)
        }
        model.startButtonPressed()
    }

    /**
     * Sets visibility and text on buttons given exercise state.
     */
    private fun setControlState(status: LogViewModel.ExerciseStatus) {
        when (status) {
            STARTED, RESUMED -> {binding.stopButton.visibility = View.GONE
                                 binding.startButton.text = getText(R.string.pause) }
            PAUSED           -> {binding.stopButton.visibility = View.VISIBLE
                                 binding.startButton.text = getText(R.string.resume) }
            else             -> {binding.stopButton.visibility = View.GONE
                                 binding.startButton.text = getText(R.string.start) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}