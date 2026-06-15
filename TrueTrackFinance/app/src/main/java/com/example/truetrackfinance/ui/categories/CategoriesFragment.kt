package com.example.truetrackfinance.ui.categories

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.R
import com.example.truetrackfinance.databinding.FragmentCategoriesBinding
import com.example.truetrackfinance.ui.viewmodel.CategoryViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.util.CurrencyUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "CategoriesFragment"

/**
 * CategoriesFragment manages budget categories with a high-fidelity list view.
 */
@AndroidEntryPoint
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategoryViewModel by viewModels()

    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing Category Management UI")
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onEdit = { category -> 
                Log.i(TAG, "Navigate: Launching edit mode for ${category.categoryName}")
                AddCategoryBottomSheet.newInstance(
                    category.categoryId, 
                    category.categoryName, 
                    category.colorHex, 
                    category.emoji
                ).show(parentFragmentManager, AddCategoryBottomSheet.TAG)
            },
            onDelete = { category -> viewModel.prepareDelete(category) }
        )
        binding.rvCategories.adapter = adapter
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        
        // --- Drag-and-Drop Implementation ---
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = vh.bindingAdapterPosition
                val toPos = target.bindingAdapterPosition
                
                val list = adapter.currentList.toMutableList()
                java.util.Collections.swap(list, fromPos, toPos)
                adapter.submitList(list)
                return true
            }
            
            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                Log.d(TAG, "Action: Categories reordered. Saving new sort orders.")
                viewModel.updateSortOrder(adapter.currentList.map { it.categoryId })
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        })
        touchHelper.attachToRecyclerView(binding.rvCategories)
    }

    private fun setupListeners() {
        binding.btnAddCategory.setOnClickListener {
            AddCategoryBottomSheet().show(parentFragmentManager, AddCategoryBottomSheet.TAG)
        }

        binding.btnSetIncome.setOnClickListener {
            findNavController().navigate(R.id.action_categories_to_incomeAllocation)
        }
    }

    private fun observeViewModel() {
        // Observe combined categories + progress data
        viewModel.categoriesWithProgress.observe(viewLifecycleOwner) { list ->
            Log.v(TAG, "UI Update: Rendering ${list.size} categories with progress metrics")
            adapter.submitList(list)
        }
        
        viewModel.currentBudget.observe(viewLifecycleOwner) { budget ->
            binding.tvTotalBudget.text = CurrencyUtil.format(budget?.totalIncome ?: 0.0)
        }

        viewModel.deletePreview.observe(viewLifecycleOwner) { preview ->
            preview?.let { (category, count) ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Category?")
                    .setMessage("This will reassign $count expenses to Uncategorised. Continue?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.confirmDelete(category) }
                    .setNegativeButton("Cancel") { _, _ -> viewModel.cancelDelete() }
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
