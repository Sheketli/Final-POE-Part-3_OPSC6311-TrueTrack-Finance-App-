package com.example.truetrackfinance.ui.savings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.data.db.entity.AnnualEnvelope
import com.example.truetrackfinance.databinding.ItemAnnualEnvelopeBinding
import com.example.truetrackfinance.util.CurrencyUtil
import java.util.Calendar

/**
 * AnnualEnvelopeAdapter displays irregular annual expenses with calculated monthly set-asides.
 */
class AnnualEnvelopeAdapter(
    private val onDelete: (AnnualEnvelope) -> Unit
) : ListAdapter<AnnualEnvelope, AnnualEnvelopeAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemAnnualEnvelopeBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(envelope: AnnualEnvelope) {
            binding.tvEnvelopeName.text = envelope.name
            binding.tvEnvelopeAmount.text = CurrencyUtil.format(envelope.annualAmount)
            
            // --- Requirement: Calculate and display monthly set-aside amount ---
            val now = Calendar.getInstance()
            val currentMonth = now.get(Calendar.MONTH) + 1 // 1-12
            
            var monthsUntilDue = envelope.dueMonth - currentMonth
            if (monthsUntilDue <= 0) monthsUntilDue += 12
            
            // Standardised set-aside: Annual amount spread over the remaining months until due
            val monthlyAmount = envelope.annualAmount / monthsUntilDue.toDouble()
            binding.tvMonthlySetAside.text = "Set aside: ${CurrencyUtil.format(monthlyAmount)} / month"
            
            // Display due info with formatted month name
            val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            binding.tvDueMonth.text = "Due in ${monthNames[envelope.dueMonth - 1]} (${monthsUntilDue} months left)"

            binding.btnDeleteEnvelope.setOnClickListener { onDelete(envelope) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnnualEnvelopeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AnnualEnvelope>() {
            override fun areItemsTheSame(a: AnnualEnvelope, b: AnnualEnvelope) = a.id == b.id
            override fun areContentsTheSame(a: AnnualEnvelope, b: AnnualEnvelope) = a == b
        }
    }
}
