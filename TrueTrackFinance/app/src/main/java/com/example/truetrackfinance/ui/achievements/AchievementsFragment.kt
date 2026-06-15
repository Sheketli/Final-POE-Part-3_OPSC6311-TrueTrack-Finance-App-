package com.example.truetrackfinance.ui.achievements

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.FragmentAchievementsBinding
import com.example.truetrackfinance.ui.viewmodel.BadgeViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "AchievementsFragment"

/**
 * AchievementsFragment displays user badges and provides visual feedback for unlocked milestones.
 */
@AndroidEntryPoint
class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BadgeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var adapter: BadgeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing achievements screen")
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = BadgeAdapter()
        binding.rvBadges.adapter = adapter
        binding.rvBadges.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.badges.observe(viewLifecycleOwner) { badges ->
            Log.v(TAG, "Updating badge list: ${badges.size} slots")
            adapter.submitList(badges)
            
            val earnedCount = badges.count { it.earnedAt != null }
            binding.tvBadgesEarnedCount.text = getString(R.string.char_count_format, earnedCount, badges.size)
        }

        viewModel.newBadgeAwarded.observe(viewLifecycleOwner) { key ->
            key?.let {
                Log.i(TAG, "CELEBRATION: New badge unlocked - ${it.displayName}")
                triggerConfetti()
                viewModel.clearNewBadgeEvent()
            }
        }
    }

    private fun triggerConfetti() {
        binding.lottieConfetti.visibility = View.VISIBLE
        binding.lottieConfetti.playAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
