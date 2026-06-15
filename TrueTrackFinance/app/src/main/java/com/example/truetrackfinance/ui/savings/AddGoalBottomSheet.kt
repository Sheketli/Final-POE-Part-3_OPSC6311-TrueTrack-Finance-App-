package com.example.truetrackfinance.ui.savings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.truetrackfinance.data.db.entity.SavingsGoal
import com.example.truetrackfinance.databinding.BottomSheetAddGoalBinding
import com.example.truetrackfinance.ui.viewmodel.SavingsViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.util.DateUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint

private const val TAG = "AddGoalBottomSheet"

/**
 * AddGoalBottomSheet provides a form for creating a new savings target.
 */
@AndroidEntryPoint
class AddGoalBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddGoalBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SavingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private var selectedDeadline: Long = System.currentTimeMillis() + 2592000000L // Default 30 days

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)

        binding.btnPickDeadline.text = DateUtil.formatDisplay(selectedDeadline)
        binding.btnPickDeadline.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Deadline")
                .setSelection(selectedDeadline)
                .build()
            
            picker.show(parentFragmentManager, "DatePicker")
            picker.addOnPositiveButtonClickListener { date ->
                selectedDeadline = date
                binding.btnPickDeadline.text = DateUtil.formatDisplay(date)
            }
        }

        binding.btnSaveGoal.setOnClickListener {
            val name = binding.etGoalName.text.toString().trim()
            val amount = binding.etGoalTarget.text.toString().toDoubleOrNull() ?: 0.0
            
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a goal name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid target amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goal = SavingsGoal(
                userId = userId,
                name = name,
                targetAmount = amount,
                currentAmount = 0.0,
                deadline = selectedDeadline
            )

            Log.i(TAG, "Action: Creating new savings goal '${name}' for R$amount")
            viewModel.addGoal(goal)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddGoalBottomSheet"
    }
}
