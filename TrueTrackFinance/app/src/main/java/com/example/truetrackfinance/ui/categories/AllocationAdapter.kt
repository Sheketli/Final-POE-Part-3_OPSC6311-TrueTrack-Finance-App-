package com.example.truetrackfinance.ui.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.db.entity.Category
import com.example.truetrackfinance.databinding.ItemAllocationRowBinding
import com.example.truetrackfinance.util.CurrencyUtil
import java.util.*

/**
 * AllocationAdapter handles the input fields for per-category budget limits.
 */
class AllocationAdapter(
    private val onLimitChanged: (Long, Double) -> Unit
) : ListAdapter<Category, AllocationAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val limitsMap = mutableMapOf<Long, Double>()

    fun setInitialLimits(limits: Map<Long, Double>) {
        limitsMap.clear()
        limitsMap.putAll(limits)
        notifyDataSetChanged()
    }

    fun getLimits(): Map<Long, Double> = limitsMap

    inner class ViewHolder(private val binding: ItemAllocationRowBinding) : RecyclerView.ViewHolder(binding.root) {
        private var currentWatcher: android.text.TextWatcher? = null

        fun bind(category: Category) {
            // Remove previous watcher to prevent multiple listeners on recycled view
            currentWatcher?.let { binding.etLimit.removeTextChangedListener(it) }
            
            binding.tvCatName.text = "${category.emoji ?: ""} ${category.name}"
            
            val currentLimit = limitsMap[category.id] ?: 0.0
            val formatted = if (currentLimit > 0) String.format(Locale.US, "%.2f", currentLimit) else ""
            
            if (binding.etLimit.text.toString() != formatted && !binding.etLimit.hasFocus()) {
                binding.etLimit.setText(formatted)
            }

            currentWatcher = binding.etLimit.doOnTextChanged { text, _, _, _ ->
                val limit = CurrencyUtil.parseAmount(text.toString()) ?: 0.0
                if (limitsMap[category.id] != limit) {
                    limitsMap[category.id] = limit
                    onLimitChanged(category.id, limit)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAllocationRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(a: Category, b: Category) = a.id == b.id
            override fun areContentsTheSame(a: Category, b: Category) = a == b
        }
    }
}
