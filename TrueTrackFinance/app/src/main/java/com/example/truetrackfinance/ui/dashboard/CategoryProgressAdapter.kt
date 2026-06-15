package com.example.truetrackfinance.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.model.CategoryProgress
import com.example.truetrackfinance.databinding.ItemCategoryProgressBinding
import com.example.truetrackfinance.util.CurrencyUtil

/**
 * Adapter for the horizontal category progress cards on the Dashboard.
 */
class CategoryProgressAdapter : ListAdapter<CategoryProgress, CategoryProgressAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemCategoryProgressBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryProgress) {
            binding.tvCategoryName.text = item.categoryName
            binding.tvCategoryEmoji.text = item.emoji ?: ""
            binding.tvSpent.text = CurrencyUtil.format(item.spentAmount)
            binding.tvLimit.text = CurrencyUtil.format(item.limitAmount)
            
            // Sync linear progress bar
            binding.progressBarCategory.progress = item.percentage.toInt()
            
            // Color dot background
            val dot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(item.colorHex))
            }
            binding.ivCategoryDot.background = dot

            // Accessibility: Show warning if limit is exceeded
            if (item.isOverLimit) {
                binding.ivWarning.visibility = View.VISIBLE
                binding.progressBarCategory.setIndicatorColor(Color.RED)
            } else if (item.isNearLimit) {
                binding.ivWarning.visibility = View.INVISIBLE
                binding.progressBarCategory.setIndicatorColor(Color.YELLOW)
            } else {
                binding.ivWarning.visibility = View.INVISIBLE
                // Forest Green for healthy categories
                binding.progressBarCategory.setIndicatorColor(Color.parseColor("#006D5B"))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
