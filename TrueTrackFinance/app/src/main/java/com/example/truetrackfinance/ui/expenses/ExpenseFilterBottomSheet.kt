package com.example.truetrackfinance.ui.expenses

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.BottomSheetExpenseFiltersBinding
import com.example.truetrackfinance.ui.viewmodel.ExpenseViewModel
import com.example.truetrackfinance.ui.viewmodel.ExpenseFilter
import com.example.truetrackfinance.util.DateUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "ExpenseFilterSheet"

/**
 * ExpenseFilterBottomSheet provides advanced filtering options for the transaction list.
 */
@AndroidEntryPoint
class ExpenseFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetExpenseFiltersBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExpenseViewModel by viewModels()
    private var currentFilter = ExpenseFilter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetExpenseFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing advanced filters")

        viewModel.filter.observe(viewLifecycleOwner) { 
            currentFilter = it
            updateUiFromFilter()
        }

        setupCategories()
        setupListeners()
    }

    private fun setupCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding.chipGroupFilterCats.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(requireContext()).apply {
                    text = category.name
                    isCheckable = true
                    isChecked = currentFilter.categoryIds.contains(category.id)
                    setOnCheckedChangeListener { _, isChecked ->
                        val newIds = currentFilter.categoryIds.toMutableSet()
                        if (isChecked) newIds.add(category.id) else newIds.remove(category.id)
                        currentFilter = currentFilter.copy(categoryIds = newIds)
                    }
                }
                binding.chipGroupFilterCats.addView(chip)
            }
        }
    }

    private fun setupListeners() {
        binding.btnFilterDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build()
            
            picker.show(parentFragmentManager, "DateRangePicker")
            picker.addOnPositiveButtonClickListener { range ->
                currentFilter = currentFilter.copy(fromDate = range.first, toDate = range.second)
                binding.btnFilterDate.text = "${DateUtil.formatDisplay(range.first)} - ${DateUtil.formatDisplay(range.second)}"
            }
        }

        binding.btnApplyFilters.setOnClickListener {
            val min = binding.etMinAmount.text.toString().toDoubleOrNull()
            val max = binding.etMaxAmount.text.toString().toDoubleOrNull()
            
            val finalFilter = currentFilter.copy(minAmount = min, maxAmount = max)
            Log.i(TAG, "Action: Applying filters -> $finalFilter")
            viewModel.updateFilter(finalFilter)
            dismiss()
        }

        binding.btnResetFilters.setOnClickListener {
            Log.d(TAG, "Action: Resetting all filters")
            viewModel.clearFilter()
            dismiss()
        }
    }

    private fun updateUiFromFilter() {
        if (currentFilter.fromDate != null && currentFilter.toDate != null) {
            binding.btnFilterDate.text = "${DateUtil.formatDisplay(currentFilter.fromDate!!)} - ${DateUtil.formatDisplay(currentFilter.toDate!!)}"
        } else {
            binding.btnFilterDate.text = "All Time"
        }
        
        binding.etMinAmount.setText(currentFilter.minAmount?.toString() ?: "")
        binding.etMaxAmount.setText(currentFilter.maxAmount?.toString() ?: "")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ExpenseFilterBottomSheet"
    }
}
