package com.example.truetrackfinance.ui.expenses

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.truetrackfinance.databinding.FragmentExpensesBinding
import com.example.truetrackfinance.ui.viewmodel.ExpenseViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "RecurringTransactions"

/**
 * RecurringTransactionsFragment allows users to manage their active auto-logged expense series.
 */
@AndroidEntryPoint
class RecurringTransactionsFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExpenseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var adapter: RecurringSeriesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing Recurring Series Management")

        // Refine UI for series management mode
        binding.searchContainer.visibility = View.GONE
        binding.btnAddExpenseBottom.visibility = View.GONE
        
        // Potential logic: Update empty state text
        // binding.tvEmptyMessage?.text = "No recurring series active"

        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = RecurringSeriesAdapter(
            onDelete = { series -> 
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Stop Recurrence?")
                    .setMessage("This will stop future auto-logs for '${series.description}'. Past logs will remain.")
                    .setPositiveButton("Stop") { _, _ -> 
                        Log.i(TAG, "Action: Deleting master recurring series ID ${series.id}")
                        viewModel.deleteExpense(series) 
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding.rvExpenses.adapter = adapter
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeViewModel() {
        viewModel.expenses.observe(viewLifecycleOwner) { list ->
            // Filter only recurring transactions (series masters)
            val seriesList = list.filter { it.isRecurring }
            Log.i(TAG, "Database Sync: Found ${list.size} total expenses, ${seriesList.size} are recurring")
            
            adapter.submitList(seriesList)
            
            // UI Toggle: Show Empty State if no recurring items exist
            if (seriesList.isEmpty()) {
                Log.d(TAG, "UI Update: Displaying empty state message")
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.rvExpenses.visibility = View.GONE
            } else {
                Log.d(TAG, "UI Update: Rendering ${seriesList.size} items in list")
                binding.emptyStateContainer.visibility = View.GONE
                binding.rvExpenses.visibility = View.VISIBLE
            }

            // Update summary metrics
            val totalMonthlyRecurring = seriesList.sumOf { it.amount }
            binding.tvTotalSpentList.text = com.example.truetrackfinance.util.CurrencyUtil.format(totalMonthlyRecurring)
            binding.tvTotalCount.text = seriesList.size.toString()
            binding.tvAvgDay.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
