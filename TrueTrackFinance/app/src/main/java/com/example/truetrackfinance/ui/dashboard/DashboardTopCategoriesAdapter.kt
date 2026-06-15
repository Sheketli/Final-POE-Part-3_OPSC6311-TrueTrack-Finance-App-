package com.example.truetrackfinance.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.model.CategoryProgress
import com.example.truetrackfinance.databinding.ItemTopSpenderRowBinding
import com.example.truetrackfinance.util.CurrencyUtil

/**
 * Adapter for the Top 3 Spenders vertical list on the Dashboard.
 */
class DashboardTopCategoriesAdapter : ListAdapter<CategoryProgress, DashboardTopCategoriesAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemTopSpenderRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryProgress) {
            binding.tvCatName.text = item.categoryName
            binding.tvCatEmoji.text = item.emoji ?: ""
            binding.tvCatSpent.text = CurrencyUtil.format(item.spentAmount)
            
            // Percentage of total budget (simplified)
            binding.tvPercentage.text = "${item.percentage.toInt()}%"
            
            val dot = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(item.colorHex))
            }
            binding.ivColorDot.background = dot
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopSpenderRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
