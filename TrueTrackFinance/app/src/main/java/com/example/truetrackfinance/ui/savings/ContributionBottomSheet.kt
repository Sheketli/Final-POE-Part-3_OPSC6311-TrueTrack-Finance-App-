package com.example.truetrackfinance.ui.savings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.truetrackfinance.databinding.BottomSheetContributeBinding
import com.example.truetrackfinance.ui.viewmodel.SavingsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "ContributionSheet"

/**
 * ContributionBottomSheet allows users to log manual contributions to their savings goals.
 */
@AndroidEntryPoint
class ContributionBottomSheet(
    private val goalId: Long,
    private val goalName: String
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetContributeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SavingsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetContributeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Logging contribution for $goalName")
        
        binding.tvGoalTitleSheet.text = "Contributing to: $goalName"

        binding.btnSaveContribution.setOnClickListener {
            val amount = binding.etContributionAmount.text.toString().toDoubleOrNull() ?: 0.0
            
            if (amount > 0) {
                Log.i(TAG, "Action: Adding R$amount to goal ID $goalId")
                viewModel.addContribution(goalId, amount)
                Toast.makeText(requireContext(), "Contribution saved!", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ContributionBottomSheet"
    }
}
