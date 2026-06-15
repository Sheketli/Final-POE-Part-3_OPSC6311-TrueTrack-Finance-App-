package com.example.truetrackfinance.ui.categories

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.FragmentIncomeAllocationBinding
import com.example.truetrackfinance.ui.viewmodel.CategoryViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.util.CurrencyUtil
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

private const val TAG = "IncomeAllocation"

/**
 * IncomeAllocationFragment implements the Zero-Based Budgeting framework.
 */
@AndroidEntryPoint
class IncomeAllocationFragment : Fragment() {

    private var _binding: FragmentIncomeAllocationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategoryViewModel by viewModels()

    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var adapter: AllocationAdapter
    private var totalIncome: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIncomeAllocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing detailed budgeting view")
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupRecyclerView()
        observeViewModel()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = AllocationAdapter { _, _ ->
            calculateRemaining()
        }
        binding.rvAllocations.adapter = adapter
        binding.rvAllocations.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.submitList(categories)
        }

        viewModel.currentBudget.observe(viewLifecycleOwner) { budget ->
            budget?.let {
                totalIncome = it.totalIncome
                
                // Pre-fill fields if not already focused
                if (!binding.etIncome.hasFocus()) {
                    binding.etIncome.setText(String.format(Locale.US, "%.2f", it.totalIncome))
                }
                if (!binding.etMinSpent.hasFocus()) {
                    binding.etMinSpent.setText(String.format(Locale.US, "%.2f", it.minSpentGoal))
                }
                if (!binding.etMaxSpent.hasFocus()) {
                    binding.etMaxSpent.setText(String.format(Locale.US, "%.2f", it.maxSpentGoal))
                }
                
                calculateRemaining()
            }
        }

        viewModel.categoryLimits.observe(viewLifecycleOwner) { limits ->
            val limitMap = limits.associate { it.categoryId to it.limitAmount }
            adapter.setInitialLimits(limitMap)
            calculateRemaining()
        }
    }

    private fun setupListeners() {
        binding.etIncome.doOnTextChanged { text, _, _, _ ->
            totalIncome = CurrencyUtil.parseAmount(text.toString()) ?: 0.0
            calculateRemaining()
        }

        binding.btnSaveAllocation.setOnClickListener {
            val remaining = calculateRemainingValue()
            val incomeText = binding.etIncome.text.toString()
            val minText = binding.etMinSpent.text.toString()
            val maxText = binding.etMaxSpent.text.toString()

            val income = CurrencyUtil.parseAmount(incomeText) ?: 0.0
            val minSpent = CurrencyUtil.parseAmount(minText) ?: 0.0
            val maxSpent = CurrencyUtil.parseAmount(maxText) ?: 0.0

            // Logic Fix: Only block if over-allocated. Allow positive remaining (unallocated).
            if (remaining < -0.01) {
                Toast.makeText(requireContext(), "You have over-allocated your income by ${CurrencyUtil.format(-remaining)}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (income <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid monthly income", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (minSpent > maxSpent && maxSpent > 0) {
                Toast.makeText(requireContext(), "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.i(TAG, "Action: Saving budget plan [Income: $income, Min: $minSpent, Max: $maxSpent]")
            
            // Ensure we use the latest parsed values from the UI
            viewModel.setMonthlyGoals(income, minSpent, maxSpent)
            viewModel.saveCategoryLimits(adapter.getLimits())
            
            Toast.makeText(requireContext(), "Budget plan saved successfully!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun calculateRemaining() {
        val remaining = calculateRemainingValue()
        
        when {
            Math.abs(remaining) < 0.01 -> {
                binding.tvRemainingToAllocate.text = "Perfectly Allocated! (Zero-Based)"
                binding.tvRemainingToAllocate.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            }
            remaining < 0 -> {
                binding.tvRemainingToAllocate.text = "Over-allocated: ${CurrencyUtil.format(Math.abs(remaining))}"
                binding.tvRemainingToAllocate.setTextColor(android.graphics.Color.RED)
            }
            else -> {
                binding.tvRemainingToAllocate.text = "Remaining to allocate: ${CurrencyUtil.format(remaining)}"
                binding.tvRemainingToAllocate.setTextColor(android.graphics.Color.GRAY)
            }
        }
    }

    private fun calculateRemainingValue(): Double {
        val allocated = adapter.getLimits().values.sum()
        return totalIncome - allocated
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
