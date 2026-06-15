package com.example.truetrackfinance.ui.categories

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.db.entity.Category
import com.example.truetrackfinance.databinding.BottomSheetAddCategoryBinding
import com.example.truetrackfinance.databinding.ItemColorPresetBinding
import com.example.truetrackfinance.ui.viewmodel.CategoryViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_CATEGORY_ID = "arg_category_id"
private const val ARG_CATEGORY_NAME = "arg_category_name"
private const val ARG_CATEGORY_COLOR = "arg_category_color"
private const val ARG_CATEGORY_EMOJI = "arg_category_emoji"

/**
 * Bottom sheet for creating or editing an expense category.
 */
@AndroidEntryPoint
class AddCategoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddCategoryBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: CategoryViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var categoryId: Long? = null
    private var selectedColorHex: String = "#006D5B"
    private val colorPresets = listOf(
        "#006D5B", "#0D9488", "#0891B2", "#0284C7", 
        "#2563EB", "#4F46E5", "#7C3AED", "#9333EA",
        "#C026D3", "#DB2777", "#E11D48", "#DC2626",
        "#EA580C", "#D97706", "#CA8A04", "#65A30D"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_CATEGORY_ID)) {
                categoryId = it.getLong(ARG_CATEGORY_ID)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupColorGrid()
        
        // Handle Edit Mode pre-fill
        arguments?.let { args ->
            if (categoryId != null) {
                binding.tvTitle.text = "Edit Category"
                binding.etCategoryName.setText(args.getString(ARG_CATEGORY_NAME))
                binding.etEmoji.setText(args.getString(ARG_CATEGORY_EMOJI))
                selectedColorHex = args.getString(ARG_CATEGORY_COLOR) ?: "#006D5B"
            }
        }
        
        setupSaveButton()
    }

    private fun setupColorGrid() {
        val adapter = ColorPresetAdapter(colorPresets) { hex ->
            selectedColorHex = hex
        }
        
        // Find current color index for selection
        val initialIndex = colorPresets.indexOf(selectedColorHex).coerceAtLeast(0)
        adapter.setInitialSelection(initialIndex)

        binding.rvColorPresets.apply {
            this.adapter = adapter
            layoutManager = GridLayoutManager(requireContext(), 8)
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            val emoji = binding.etEmoji.text.toString().trim()
            
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = Category(
                id = categoryId ?: 0,
                userId = authViewModel.getActiveUserId(),
                name = name,
                colorHex = selectedColorHex,
                emoji = if (emoji.isNotEmpty()) emoji else null,
                sortOrder = 0 // Repository preserves existing or sets next on insert
            )

            if (categoryId == null) {
                Log.i("AddCategorySheet", "Action: Creating new category '$name'")
                viewModel.addCategory(category)
            } else {
                Log.i("AddCategorySheet", "Action: Updating existing category ID $categoryId to '$name'")
                viewModel.updateCategory(category)
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ColorPresetAdapter(
        private val colors: List<String>,
        private val onSelected: (String) -> Unit
    ) : RecyclerView.Adapter<ColorPresetAdapter.ViewHolder>() {

        private var selectedIndex = 0

        fun setInitialSelection(index: Int) {
            selectedIndex = index
        }

        inner class ViewHolder(val binding: ItemColorPresetBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemColorPresetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val colorHex = colors[position]
            val drawable = holder.binding.colorCircle.background as GradientDrawable
            drawable.setColor(Color.parseColor(colorHex))

            holder.binding.ivSelected.visibility = if (selectedIndex == position) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                val oldIndex = selectedIndex
                selectedIndex = holder.bindingAdapterPosition
                notifyItemChanged(oldIndex)
                notifyItemChanged(selectedIndex)
                onSelected(colorHex)
            }
        }

        override fun getItemCount() = colors.size
    }

    companion object {
        const val TAG = "AddCategoryBottomSheet"

        /** Factory method for Create mode. */
        fun newInstance() = AddCategoryBottomSheet()

        /** Factory method for Edit mode. */
        fun newInstance(id: Long, name: String, color: String, emoji: String?) = AddCategoryBottomSheet().apply {
            arguments = Bundle().apply {
                putLong(ARG_CATEGORY_ID, id)
                putString(ARG_CATEGORY_NAME, name)
                putString(ARG_CATEGORY_COLOR, color)
                putString(ARG_CATEGORY_EMOJI, emoji)
            }
        }
    }
}
