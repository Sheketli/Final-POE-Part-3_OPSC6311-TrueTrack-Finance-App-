package com.example.truetrackfinance.ui.reports

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.R
import com.example.truetrackfinance.data.model.CategoryProgress
import com.example.truetrackfinance.databinding.ItemCategoryVarianceBinding
import com.example.truetrackfinance.util.CurrencyUtil

/**
 * VarianceAdapter renders the Category Summary table on the Reports screen.
 * Shows Variance (Budget - Spent) with dynamic coloring.
 */
class VarianceAdapter : ListAdapter<CategoryProgress, VarianceAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(private val binding: ItemCategoryVarianceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryProgress) {
            binding.tvCatName.text = item.categoryName
            binding.tvCatSpent.text = CurrencyUtil.format(item.spentAmount)
            binding.tvCatLimit.text = CurrencyUtil.format(item.limitAmount)
            
            val variance = item.limitAmount - item.spentAmount
            // Format variance with + or - sign
            val sign = if (variance >= 0) "+" else ""
            binding.tvVariance.text = getString(binding.root.context, R.string.variance_format, sign, CurrencyUtil.format(java.lang.Math.abs(variance)))
            
            // Apply semantic coloring based on financial performance
            val color = if (variance < 0) R.color.danger else R.color.success
            binding.tvVariance.setTextColor(ContextCompat.getColor(binding.root.context, color))
        }

        private fun getString(context: android.content.Context, resId: Int, vararg args: Any): String {
            return context.getString(resId, *args)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryVarianceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
