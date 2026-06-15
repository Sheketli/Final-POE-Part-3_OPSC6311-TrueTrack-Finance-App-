package com.example.truetrackfinance.ui.expenses

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.FragmentExpensesBinding
import com.example.truetrackfinance.ui.viewmodel.ExpenseViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.ui.dashboard.RecentExpensesAdapter
import com.example.truetrackfinance.util.CurrencyUtil
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "ExpensesFragment"

/**
 * ExpensesFragment displays a filtered list of financial transactions.
 * Supports Search, Multi-select Categories, Date Range, and Amount filters.
 */
@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private var _binding: FragmentExpensesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExpenseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var adapter: RecentExpensesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing detailed History view")
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = RecentExpensesAdapter(
            onItemClick = { expense ->
                Log.d(TAG, "Navigate: Edit expense ID ${expense.id}")
                val action = ExpensesFragmentDirections.actionExpensesToAddExpense(expense.id)
                findNavController().navigate(action)
            },
            onThumbnailClick = { path ->
                Log.d(TAG, "Navigate: Full-screen receipt for $path")
                val action = ExpensesFragmentDirections.actionExpensesToImageViewer(path)
                findNavController().navigate(action)
            }
        )
        binding.rvExpenses.adapter = adapter
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        
        // --- Swipe-to-Delete with Undo Feature ---
        val swipeHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                val expense = adapter.currentList[position]
                
                Log.w(TAG, "Action: Deleting expense '${expense.description}' via swipe")
                viewModel.deleteExpense(expense)
                
                Snackbar.make(binding.root, "Expense deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { 
                        Log.i(TAG, "Action: Restoring deleted expense")
                        viewModel.saveExpense(expense) 
                    }.show()
            }
        })
        swipeHelper.attachToRecyclerView(binding.rvExpenses)
    }

    private fun setupListeners() {
        binding.btnAddExpenseBottom.setOnClickListener {
            findNavController().navigate(R.id.addExpenseFragment)
        }

        binding.btnFilter.setOnClickListener {
            Log.d(TAG, "Action: Launching advanced filter sheet")
            ExpenseFilterBottomSheet().show(parentFragmentManager, ExpenseFilterBottomSheet.TAG)
        }

        // Live Search logic (updates ViewModel state)
        binding.etSearchExpenses.doOnTextChanged { text, _, _, _ ->
            val query = text.toString()
            Log.v(TAG, "Filtering by search: $query")
            val currentFilter = viewModel.filter.value ?: com.example.truetrackfinance.ui.viewmodel.ExpenseFilter()
            viewModel.updateFilter(currentFilter.copy(search = query))
        }

        // Horizontal Category Quick-Filter logic
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            binding.chipGroupExpenseFilters.removeAllViews()
            
            // Add "All" chip
            val allChip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = "All"
                isCheckable = true
                isChecked = (viewModel.filter.value?.categoryIds?.isEmpty() ?: true)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) viewModel.updateFilter(viewModel.filter.value?.copy(categoryIds = emptySet()) ?: com.example.truetrackfinance.ui.viewmodel.ExpenseFilter())
                }
            }
            binding.chipGroupExpenseFilters.addView(allChip)

            categories.forEach { category ->
                val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                    text = category.name
                    isCheckable = true
                    isChecked = (viewModel.filter.value?.categoryIds?.contains(category.id) ?: false)
                    setOnCheckedChangeListener { _, isChecked ->
                        val current = viewModel.filter.value ?: com.example.truetrackfinance.ui.viewmodel.ExpenseFilter()
                        val newIds = current.categoryIds.toMutableSet()
                        if (isChecked) newIds.add(category.id) else newIds.remove(category.id)
                        viewModel.updateFilter(current.copy(categoryIds = newIds))
                    }
                }
                binding.chipGroupExpenseFilters.addView(chip)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.expenses.observe(viewLifecycleOwner) { list ->
            Log.v(TAG, "UI Update: Displaying ${list.size} filtered transactions")
            adapter.submitList(list.map { it.toExpense() })
            
            // Real-time Summary Card update
            val total = list.sumOf { it.amount }
            binding.tvTotalSpentList.text = CurrencyUtil.format(total)
            binding.tvTotalCount.text = list.size.toString()
            
            // Requirement: reverse-chronological order is handled by the DAO query
        }
    }

    // --- Entity Mapping for Adapter Compatibility ---
    private fun com.example.truetrackfinance.data.model.ExpenseWithCategory.toExpense() = com.example.truetrackfinance.data.db.entity.Expense(
        id = this.id, 
        userId = this.userId, 
        categoryId = this.categoryId, 
        amount = this.amount, 
        description = this.description, 
        date = this.date, 
        receiptPhotoPath = this.receiptPhotoPath, 
        isRecurring = this.isRecurring, 
        recurringFrequency = this.recurringFrequency, 
        currency = this.currency
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
