package com.example.truetrackfinance.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.FragmentDashboardBinding
import com.example.truetrackfinance.ui.viewmodel.DashboardViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.util.CurrencyUtil
import dagger.hilt.android.AndroidEntryPoint

/**
 * DashboardFragment provides a high-level overview of the user's budget and spending.
 */
@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private val binding get() = _binding!!
    
    // AuthViewModel used to access session data (User details)
    private val authViewModel: AuthViewModel by viewModels()

    // DashboardViewModel used for the main financial metrics
    private val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var categoryProgressAdapter: CategoryProgressAdapter

    private lateinit var topSpendersAdapter: DashboardTopCategoriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DashboardFragment", "onViewCreated: Setting up dashboard components")

        setupRecyclerViews()
        observeViewModel()

        // UI Listeners for navigation based on new design
        binding.tvViewAllCats.setOnClickListener {
            Log.d("DashboardFragment", "Navigating to Categories screen")
            findNavController().navigate(R.id.categoriesFragment)
        }
        binding.btnAllocate.setOnClickListener {
            Log.d("DashboardFragment", "Navigating to Income Allocation for unallocated funds")
            findNavController().navigate(R.id.action_dashboard_to_incomeAllocation)
        }
    }

    /**
     * Initializes the category progress horizontal list.
     */
    private fun setupRecyclerViews() {
        categoryProgressAdapter = CategoryProgressAdapter()
        binding.rvCategoryProgress.apply {
            adapter = categoryProgressAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        topSpendersAdapter = DashboardTopCategoriesAdapter()
        binding.rvTopSpenders.apply {
            adapter = topSpendersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * Connects UI elements to the ViewModel data streams.
     */
    private fun observeViewModel() {
        // Fetch current user ID from session to initialize dashboard data
        val userId = authViewModel.getActiveUserId()
        Log.i("DashboardFragment", "Initializing dashboard for active user: $userId")
        viewModel.initialise(userId)
        
        // Observe and update the personalized greeting using real session data
        authViewModel.getActiveUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                Log.v("DashboardFragment", "Updating greeting for user: ${it.fullName}")
                // Using the Full Name for the personalized greeting
                binding.tvUsernameGreeting.text = getString(R.string.greeting_with_emoji, it.fullName)
            }
        }
        
        // Observe full dashboard state (budget, spent, streak, etc.)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            Log.v("DashboardFragment", "UI State updated: totalSpent=${state.totalSpent}")
            
            // 1. Central Circular Gauge
            binding.circularProgressBudget.progress = (state.progressFraction * 100).toInt()
            binding.tvTotalSpent.text = CurrencyUtil.format(state.totalSpent)
            binding.tvRemainingBottom.text = getString(R.string.amount_left_format, CurrencyUtil.format(state.remaining))
            
            // 2. Quick Stats Row
            binding.tvTotalBudget.text = CurrencyUtil.format(state.totalBudget)
            
            // --- Daily Allowance Indicator ---
            binding.tvDailyAllowance.text = CurrencyUtil.format(state.dailyAllowance)
            if (state.dailyAllowance < 0) {
                // Semantic coloring: Red for overspent allowance
                binding.tvDailyAllowance.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.danger))
            } else {
                // White for healthy allowance to match the Forest Green header contrast
                binding.tvDailyAllowance.setTextColor(android.graphics.Color.WHITE)
            }

            // Plurals for correct grammar (e.g., "1-day streak" vs "12-day streak")
            binding.tvStreakCount.text = resources.getQuantityString(R.plurals.streak_days, state.currentStreak, state.currentStreak)

            // 3. Unallocated Funds Warning Card
            if (state.hasUnallocatedFunds) {
                Log.w("DashboardFragment", "User has unallocated funds: ${state.unallocatedAmount}")
                binding.cardUnallocated.visibility = View.VISIBLE
                binding.tvUnallocatedMsg.text = getString(R.string.unallocated_msg_format, CurrencyUtil.format(state.unallocatedAmount))
            } else {
                binding.cardUnallocated.visibility = View.GONE
            }

            // 4. Horizontal Category Progress List
            categoryProgressAdapter.submitList(state.categoryProgress)

            // 5. Top 3 Spenders Vertical List
            val topSpenders = state.categoryProgress.take(3)
            topSpendersAdapter.submitList(topSpenders)
            binding.cardTopSpenders.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
