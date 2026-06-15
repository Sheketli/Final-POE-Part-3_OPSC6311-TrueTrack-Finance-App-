package com.example.truetrackfinance.ui.categories

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.model.CategoryProgress
import com.example.truetrackfinance.databinding.ItemCategoryRowBinding
import com.example.truetrackfinance.util.CurrencyUtil
import java.util.Locale

/**
 * CategoryAdapter handles the redesigned category management list with high-fidelity progress indicators.
 */
class CategoryAdapter(
    private val onEdit: (CategoryProgress) -> Unit,
    private val onDelete: (CategoryProgress) -> Unit
) : ListAdapter<CategoryProgress, CategoryAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemCategoryRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryProgress) {
            binding.tvCategoryName.text = item.categoryName
            binding.tvCategoryEmoji.text = item.emoji ?: "📁"
            
            // Sync spending and limit labels matching the reference image
            binding.tvSpent.text = CurrencyUtil.format(item.spentAmount)
            binding.tvLimit.text = CurrencyUtil.format(item.limitAmount)
            
            // Progress Bar and Percentage logic
            val percentInt = item.percentage.toInt().coerceIn(0, 100)
            binding.tvPercentage.text = String.format(Locale.getDefault(), "%d%%", percentInt)
            binding.progressBarCategory.progress = percentInt
            
            // Semantic coloring based on budget status
            val indicatorColor = when {
                item.isOverLimit -> Color.RED
                item.isNearLimit -> Color.parseColor("#FBC02D") // Amber
                else -> Color.parseColor("#006D5B") // Forest Green
            }
            binding.progressBarCategory.setIndicatorColor(indicatorColor)

            // Interactive Icons: Pencil (Edit) and Bin (Delete)
            binding.btnRenameCategory.setOnClickListener { onEdit(item) }
            binding.btnDeleteCategory.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CategoryProgress>() {
            override fun areItemsTheSame(a: CategoryProgress, b: CategoryProgress) = a.categoryId == b.categoryId
            override fun areContentsTheSame(a: CategoryProgress, b: CategoryProgress) = a == b
        }
    }
}
