package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.antware.joggerlogger.databinding.FragmentStatsBinding

@SuppressLint("SetTextI18n")
class StatsFragment : Fragment() {

    private val model: LogViewModel by activityViewModels()
    private var _binding: FragmentStatsBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
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
        //binding.durationView.text = getDurationText(LogViewModel.Duration(0, 0, 0))
        model.duration.observe(viewLifecycleOwner, androidx.lifecycle.Observer { duration ->
            binding.durationView.text = getDurationText(duration)
        })
        //model.getStatus().observe(viewLifecycleOwner, onStatusChanged())
        model.distance.observe(viewLifecycleOwner, androidx.lifecycle.Observer { distance ->
            binding.distanceView.text = distance.toString().take(4)
        })
        model.currSpeed.observe(viewLifecycleOwner, androidx.lifecycle.Observer { currSpeed ->
            binding.currSpeedView.text = (currSpeed.toString() + "0").take(4)
        })
        model.avgSpeed.observe(viewLifecycleOwner, androidx.lifecycle.Observer { avgSpeed ->
            binding.avgSpeedView.text = (avgSpeed.toString() + "0").take(4)
        })
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