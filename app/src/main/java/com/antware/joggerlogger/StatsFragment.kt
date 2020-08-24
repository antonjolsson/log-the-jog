package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.antware.joggerlogger.ExerciseCompleteFragment.*
import com.antware.joggerlogger.databinding.FragmentStatsBinding
import java.util.*

@SuppressLint("SetTextI18n")
class StatsFragment : Fragment() {

    var reset: Boolean = false
    private val model: LogViewModel by activityViewModels()
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.duration.observe(viewLifecycleOwner, { duration ->
            setDuration(duration)
        })
        model.distance.observe(viewLifecycleOwner, { distance ->
            TWO_DECIMALS_FORMAT.setFigure(binding.distanceView, distance)
        })
        model.currSpeed.observe(viewLifecycleOwner, { currSpeed ->
            TWO_DECIMALS_FORMAT.setFigure(binding.currSpeedView, currSpeed)
        })
        model.avgSpeed.observe(viewLifecycleOwner, { avgSpeed ->
            TWO_DECIMALS_FORMAT.setFigure(binding.avgSpeedView, avgSpeed)
        })
    }

    override fun onResume() {
        super.onResume()
        if (reset) {
            model.reset()
            reset = false
        }
    }

    private fun String.setFigure(textView: TextView, value: Double) {
        textView.text = String.format(Locale.ENGLISH, this, value)
    }

    private fun setDuration(duration: LogViewModel.Duration) {
        binding.durationView.text = getDurationText(duration)
    }

    companion object {
        fun getDurationText(duration: LogViewModel.Duration): String {
            val hours = "0" + duration.hours.toString()
            val minutes = "0" + duration.minutes.toString()
            val seconds = "0" + duration.seconds.toString()
            return """${hours.takeLast(2)}:${minutes.takeLast(2)}:${seconds.takeLast(2)}"""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}