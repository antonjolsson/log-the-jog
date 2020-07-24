package com.antware.joggerlogger

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.antware.joggerlogger.databinding.FragmentControlBinding
import com.antware.joggerlogger.databinding.FragmentStatsBinding
import java.util.*

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
        // Inflate the layout for this fragment
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.duration.observe(viewLifecycleOwner, androidx.lifecycle.Observer { duration ->
            setDurationView(duration)
        })
        model.distance.observe(viewLifecycleOwner, androidx.lifecycle.Observer { distance ->
            binding.distanceView.text = (distance / 1000).toString() + "." + ((distance % 1000).toString() + 0).take(2) + " km"
        })
        model.speed.observe(viewLifecycleOwner, androidx.lifecycle.Observer { speed ->
            binding.speedView.text = (speed.toString() + "0").take(4) + " km/h"
        })
    }

    private fun setDurationView(duration: LogViewModel.Duration) {
        val hours = "0" + duration.hours.toString()
        val minutes = "0" + duration.minutes.toString()
        val seconds = "0" + duration.seconds.toString()
        binding.durationView.text = """${hours.takeLast(2)}:${minutes.takeLast(2)}:${seconds.takeLast(2)}"""
    }

}