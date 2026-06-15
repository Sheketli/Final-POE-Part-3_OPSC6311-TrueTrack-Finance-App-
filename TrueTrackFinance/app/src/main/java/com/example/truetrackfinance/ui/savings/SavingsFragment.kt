package com.example.truetrackfinance.ui.savings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.truetrackfinance.databinding.FragmentSavingsBinding
import com.example.truetrackfinance.ui.viewmodel.SavingsViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.util.CurrencyUtil
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "SavingsFragment"

/**
 * SavingsFragment manages Savings Goals and Annual Envelopes.
 */
@AndroidEntryPoint
class SavingsFragment : Fragment() {

    private var _binding: FragmentSavingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SavingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var goalAdapter: SavingsGoalAdapter
    private lateinit var envelopeAdapter: AnnualEnvelopeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing detailed Savings screen")
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupRecyclerViews()
        setupTabs()
        observeViewModel()
        
        binding.btnNewGoalFab.setOnClickListener {
            val currentTab = binding.tabLayoutSavings.selectedTabPosition
            Log.d(TAG, "Action: Creating new savings entry on tab index $currentTab")
            
            if (currentTab == 0) {
                AddGoalBottomSheet().show(parentFragmentManager, AddGoalBottomSheet.TAG)
            } else {
                AddEnvelopeBottomSheet().show(parentFragmentManager, AddEnvelopeBottomSheet.TAG)
            }
        }
    }

    private fun setupRecyclerViews() {
        goalAdapter = SavingsGoalAdapter(
            onContribute = { goal -> 
                ContributionBottomSheet(goal.id, goal.name).show(parentFragmentManager, ContributionBottomSheet.TAG)
            },
            onDelete = { goal -> viewModel.deleteGoal(goal) }
        )
        
        envelopeAdapter = AnnualEnvelopeAdapter(
            onDelete = { envelope -> viewModel.deleteEnvelope(envelope) }
        )
        
        binding.rvGoals.apply {
            adapter = goalAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupTabs() {
        binding.tabLayoutSavings.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.rvGoals.adapter = goalAdapter
                        binding.btnNewGoalFab.text = "New Goal"
                    }
                    1 -> {
                        binding.rvGoals.adapter = envelopeAdapter
                        binding.btnNewGoalFab.text = "New Envelope"
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel() {
        viewModel.savingsGoals.observe(viewLifecycleOwner) { goals ->
            Log.v(TAG, "Updating goal list: ${goals.size} active goals")
            goalAdapter.submitList(goals)
            
            val totalSaved = goals.sumOf { it.currentAmount }
            val totalTarget = goals.sumOf { it.targetAmount }
            
            binding.tvTotalSavedAmount.text = CurrencyUtil.format(totalSaved)
            binding.tvActiveGoals.text = goals.size.toString()
            binding.tvCompletedGoals.text = goals.count { it.currentAmount >= it.targetAmount }.toString()
            binding.tvRemainingSaved.text = CurrencyUtil.format((totalTarget - totalSaved).coerceAtLeast(0.0))
        }

        viewModel.annualEnvelopes.observe(viewLifecycleOwner) { envelopes ->
            envelopeAdapter.submitList(envelopes)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
