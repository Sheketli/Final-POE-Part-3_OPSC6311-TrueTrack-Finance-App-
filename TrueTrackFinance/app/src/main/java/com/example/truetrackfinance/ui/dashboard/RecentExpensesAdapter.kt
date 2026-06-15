package com.example.truetrackfinance.ui.dashboard

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.db.entity.Expense
import com.example.truetrackfinance.databinding.ItemExpenseRowBinding
import com.example.truetrackfinance.util.CurrencyUtil
import java.io.File

/**
 * RecentExpensesAdapter displays financial transactions in a list.
 * Includes support for receipt thumbnails and category indicators.
 */
class RecentExpensesAdapter(
    private val onItemClick: (Expense) -> Unit,
    private val onThumbnailClick: (String) -> Unit = {}
) : ListAdapter<Expense, RecentExpensesAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemExpenseRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(expense: Expense) {
            binding.tvExpenseDescription.text = expense.description
            binding.tvExpenseAmount.text = CurrencyUtil.format(expense.amount)
            
            // Thumbnail Logic
            if (!expense.receiptPhotoPath.isNullOrEmpty()) {
                val file = File(expense.receiptPhotoPath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    binding.ivReceiptThumb.setImageBitmap(bitmap)
                    binding.ivReceiptThumb.visibility = View.VISIBLE
                    binding.tvRowEmoji.visibility = View.GONE
                    
                    binding.cardCatIcon.setOnClickListener {
                        onThumbnailClick(expense.receiptPhotoPath)
                    }
                } else {
                    resetThumbnail()
                }
            } else {
                resetThumbnail()
            }

            binding.root.setOnClickListener { onItemClick(expense) }
        }

        private fun resetThumbnail() {
            binding.ivReceiptThumb.visibility = View.GONE
            binding.tvRowEmoji.visibility = View.VISIBLE
            binding.cardCatIcon.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Expense>() {
            override fun areItemsTheSame(a: Expense, b: Expense) = a.id == b.id
            override fun areContentsTheSame(a: Expense, b: Expense) = a == b
        }
    }
}
