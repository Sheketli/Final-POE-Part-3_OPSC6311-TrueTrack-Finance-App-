package com.example.truetrackfinance.ui.expenses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.db.entity.Expense
import com.example.truetrackfinance.data.model.ExpenseWithCategory
import com.example.truetrackfinance.databinding.ItemRecurringSeriesBinding
import com.example.truetrackfinance.util.CurrencyUtil
import com.example.truetrackfinance.util.DateUtil
import java.util.Locale

/**
 * Adapter for managing active recurring expense series.
 */
class RecurringSeriesAdapter(
    private val onDelete: (Expense) -> Unit
) : ListAdapter<ExpenseWithCategory, RecurringSeriesAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemRecurringSeriesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExpenseWithCategory) {
            binding.tvSeriesDescription.text = item.description
            binding.tvSeriesAmount.text = CurrencyUtil.format(item.amount)
            binding.tvSeriesEmoji.text = item.categoryEmoji ?: "🔄"
            
            // Frequency display
            binding.tvSeriesFrequency.text = String.format(Locale.getDefault(), "Every %s", item.recurringFrequency ?: "Month")
            
            // Next scheduled log date
            item.nextRecurrenceDate?.let {
                binding.tvNextLogDate.text = DateUtil.formatDisplay(it)
            } ?: run {
                binding.tvNextLogDate.text = "Not scheduled"
            }

            binding.btnStopRecurrence.setOnClickListener { 
                onDelete(item.toExpense()) 
            }
        }
    }

    private fun ExpenseWithCategory.toExpense() = Expense(
        id = this.id, userId = this.userId, categoryId = this.categoryId, 
        amount = this.amount, description = this.description, date = this.date, 
        receiptPhotoPath = this.receiptPhotoPath, isRecurring = this.isRecurring, 
        recurringFrequency = this.recurringFrequency, 
        nextRecurrenceDate = this.nextRecurrenceDate,
        currency = this.currency
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecurringSeriesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ExpenseWithCategory>() {
            override fun areItemsTheSame(a: ExpenseWithCategory, b: ExpenseWithCategory) = a.id == b.id
            override fun areContentsTheSame(a: ExpenseWithCategory, b: ExpenseWithCategory) = a == b
        }
    }
}
