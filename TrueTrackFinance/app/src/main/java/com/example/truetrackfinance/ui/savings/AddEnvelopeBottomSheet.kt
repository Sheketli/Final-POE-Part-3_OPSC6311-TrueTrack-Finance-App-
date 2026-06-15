package com.example.truetrackfinance.ui.savings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.truetrackfinance.data.db.entity.AnnualEnvelope
import com.example.truetrackfinance.databinding.BottomSheetAddEnvelopeBinding
import com.example.truetrackfinance.ui.viewmodel.SavingsViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "AddEnvelopeBottomSheet"

/**
 * AddEnvelopeBottomSheet allows users to plan for irregular annual expenses.
 */
@AndroidEntryPoint
class AddEnvelopeBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddEnvelopeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SavingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var selectedMonth: Int = 1 // 1-12

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddEnvelopeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)

        setupMonthChips()

        binding.btnSaveEnvelope.setOnClickListener {
            val name = binding.etEnvelopeName.text.toString().trim()
            val amount = binding.etEnvelopeAmount.text.toString().toDoubleOrNull() ?: 0.0
            
            if (name.isEmpty() || amount <= 0) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val envelope = AnnualEnvelope(
                userId = userId,
                name = name,
                annualAmount = amount,
                dueMonth = selectedMonth
            )

            Log.i(TAG, "Action: Creating annual envelope '${name}' for R$amount")
            viewModel.addEnvelope(envelope)
            dismiss()
        }
    }

    private fun setupMonthChips() {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        months.forEachIndexed { index, name ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isChecked = (index + 1 == selectedMonth)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedMonth = index + 1
                }
            }
            binding.chipGroupMonths.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddEnvelopeBottomSheet"
    }
}
