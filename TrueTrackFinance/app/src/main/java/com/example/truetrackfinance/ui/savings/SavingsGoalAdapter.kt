package com.example.truetrackfinance.ui.savings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.db.entity.SavingsGoal
import com.example.truetrackfinance.databinding.ItemSavingsGoalBinding
import com.example.truetrackfinance.util.CurrencyUtil
import com.example.truetrackfinance.util.DateUtil
import java.util.Locale

/**
 * SavingsGoalAdapter displays a list of savings targets with progress bars and 
 * monthly contribution requirements.
 */
class SavingsGoalAdapter(
    private val onContribute: (SavingsGoal) -> Unit,
    private val onDelete: (SavingsGoal) -> Unit
) : ListAdapter<SavingsGoal, SavingsGoalAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemSavingsGoalBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(goal: SavingsGoal) {
            binding.tvGoalName.text = goal.name
            binding.tvGoalSaved.text = CurrencyUtil.format(goal.currentAmount)
            binding.tvGoalTarget.text = CurrencyUtil.format(goal.targetAmount)
            
            // Percentage calculation for progress bar
            val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100).toInt() else 0
            binding.progressBarGoal.progress = progress
            binding.tvGoalProgress.text = String.format(Locale.getDefault(), "%d%%", progress)

            binding.tvGoalDeadline.text = DateUtil.formatDisplay(goal.deadline)

            // --- Requirement: Calculate and display required monthly contribution ---
            val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
            val monthsRemaining = DateUtil.monthsBetween(System.currentTimeMillis(), goal.deadline).coerceAtLeast(1)
            
            if (remainingAmount <= 0) {
                binding.tvMonthlyContribution.text = "Completed"
                binding.tvMonthlyContribution.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // Success Green
            } else {
                val monthly = remainingAmount / monthsRemaining
                binding.tvMonthlyContribution.text = CurrencyUtil.format(monthly)
                binding.tvMonthlyContribution.setTextColor(android.graphics.Color.BLACK)
            }

            binding.btnContribute.setOnClickListener { onContribute(goal) }
            binding.btnDeleteGoal.setOnClickListener { onDelete(goal) }
            
            // Visual tweak: ensure contribute button is visible if not complete
            binding.btnContribute.visibility = if (remainingAmount > 0) View.VISIBLE else View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavingsGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SavingsGoal>() {
            override fun areItemsTheSame(a: SavingsGoal, b: SavingsGoal) = a.id == b.id
            override fun areContentsTheSame(a: SavingsGoal, b: SavingsGoal) = a == b
        }
    }
}
