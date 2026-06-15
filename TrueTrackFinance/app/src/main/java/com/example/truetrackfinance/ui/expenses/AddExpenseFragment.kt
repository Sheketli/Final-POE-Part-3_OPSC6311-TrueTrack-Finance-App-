package com.example.truetrackfinance.ui.expenses

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.truetrackfinance.R
import com.example.truetrackfinance.data.db.entity.Expense
import com.example.truetrackfinance.databinding.FragmentAddExpenseBinding
import com.example.truetrackfinance.ui.viewmodel.ExpenseViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.data.repository.CurrencyRepository
import com.example.truetrackfinance.util.CurrencyUtil
import com.example.truetrackfinance.util.DateUtil
import com.example.truetrackfinance.util.ImageUtil
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private const val TAG = "AddExpenseFragment"

/**
 * AddExpenseFragment allows users to log new financial transactions or edit existing ones.
 * Features: Amount, Description, Date Picker, Category Chips, Receipt Photo (Camera/Gallery),
 * and Recurring toggle.
 */
@AndroidEntryPoint
class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ExpenseViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val args: AddExpenseFragmentArgs by navArgs()

    @Inject
    lateinit var currencyRepository: CurrencyRepository

    private var selectedDate: Long = System.currentTimeMillis()
    private var startTime: Long? = null
    private var endTime: Long? = null
    private var selectedCategoryId: Long? = null
    private var capturedPhotoPath: String? = null

    // --- Activity Result Launchers for modern Permission/Intent handling ---

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            Log.i(TAG, "Photo captured successfully from CameraX")
            capturedPhotoPath = ImageUtil.saveCameraBitmap(requireContext(), bitmap)
            updatePhotoPreview()
        }
    }

    private val pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Log.i(TAG, "Image selected from Gallery: $uri")
            capturedPhotoPath = ImageUtil.saveCompressedImage(requireContext(), uri)
            updatePhotoPreview()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "Camera permission granted, launching capture")
            takePhotoLauncher.launch()
        } else {
            Log.w(TAG, "Camera permission denied by user")
            Toast.makeText(requireContext(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing form with design requirements")
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupUI()
        setupCategories()
        setupDateTimePickers()
        setupSaveButton()
        
        // Populate existing data if in Edit Mode
        if (args.expenseId != -1L) {
            Log.i(TAG, "Entering Edit Mode for Expense ID: ${args.expenseId}")
            viewModel.getExpenseById(args.expenseId).observe(viewLifecycleOwner) { expense ->
                expense ?: return@observe
                binding.etAmount.setText(String.format(Locale.US, "%.2f", expense.amount))
                binding.etDescription.setText(expense.description)
                selectedDate = expense.date
                binding.btnPickDate.text = DateUtil.formatDisplay(expense.date)
                
                startTime = expense.startTime
                binding.btnStartTime.text = DateUtil.formatTime(expense.startTime)
                
                endTime = expense.endTime
                binding.btnEndTime.text = DateUtil.formatTime(expense.endTime)

                selectedCategoryId = expense.categoryId
                binding.switchRecurring.isChecked = expense.isRecurring
                capturedPhotoPath = expense.receiptPhotoPath
                updatePhotoPreview()
                
                // Set the correct frequency chip if recurring
                if (expense.isRecurring) {
                    when (expense.recurringFrequency) {
                        "Daily" -> binding.chipDaily.isChecked = true
                        "Weekly" -> binding.chipWeekly.isChecked = true
                        "Monthly" -> binding.chipMonthly.isChecked = true
                        "Annually" -> binding.chipAnnually.isChecked = true
                    }
                }
            }
        }
    }

    private fun setupUI() {
        // Character counter for description validation
        binding.etDescription.doOnTextChanged { text, _, _, _ ->
            binding.tvCharCount.text = getString(R.string.char_count_format, text?.length ?: 0, 100)
        }
        
        // Show/Hide frequency selector based on recurring toggle
        binding.switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            Log.v(TAG, "Recurring toggle set to: $isChecked")
            binding.scrollFrequencies.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Photo upload card click logic
        binding.cardPhotoUpload.setOnClickListener {
            showPhotoOptionsDialog()
        }

        // Setup Currency Spinner
        val currencies = currencyRepository.getSupportedCurrencies()
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, currencies)
        binding.spinnerCurrency.setAdapter(adapter)
    }

    /**
     * Category Chips populated from the user's custom categories in the database.
     */
    private fun setupCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            Log.v(TAG, "Mapping ${categories.size} categories into selectable chips")
            binding.chipGroupCategories.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = "${category.emoji ?: ""} ${category.name}"
                    isCheckable = true
                    setChipBackgroundColorResource(R.color.forest_green_light)
                    setTextColor(Color.BLACK)
                    isChecked = (category.id == selectedCategoryId)
                    
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            selectedCategoryId = category.id
                            Log.d(TAG, "Category selected: ${category.name}")
                        }
                    }
                }
                binding.chipGroupCategories.addView(chip)
            }
        }
    }

    private fun setupDateTimePickers() {
        binding.btnPickDate.text = DateUtil.formatDisplay(selectedDate)
        binding.btnPickDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(selectedDate)
                .build()
            
            picker.show(parentFragmentManager, "DatePicker")
            picker.addOnPositiveButtonClickListener { date ->
                selectedDate = date
                binding.btnPickDate.text = DateUtil.formatDisplay(date)
                Log.d(TAG, "Date picked: $date")
            }
        }

        binding.btnStartTime.setOnClickListener {
            showTimePicker(isStartTime = true)
        }

        binding.btnEndTime.setOnClickListener {
            showTimePicker(isStartTime = false)
        }
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(if (isStartTime) "Select Start Time" else "Select End Time")
            .build()

        picker.show(parentFragmentManager, "TimePicker")
        picker.addOnPositiveButtonClickListener {
            val cal = Calendar.getInstance().apply {
                timeInMillis = selectedDate
                set(Calendar.HOUR_OF_DAY, picker.hour)
                set(Calendar.MINUTE, picker.minute)
            }
            if (isStartTime) {
                startTime = cal.timeInMillis
                binding.btnStartTime.text = DateUtil.formatTime(startTime)
                Log.d(TAG, "Start Time set: ${binding.btnStartTime.text}")
            } else {
                endTime = cal.timeInMillis
                binding.btnEndTime.text = DateUtil.formatTime(endTime)
                Log.d(TAG, "End Time set: ${binding.btnEndTime.text}")
            }
        }
    }

    private fun showPhotoOptionsDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Receipt Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndCapture()
                    1 -> pickGalleryLauncher.launch("image/*")
                    2 -> {
                        capturedPhotoPath = null
                        updatePhotoPreview()
                        Log.d(TAG, "Receipt photo removed")
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePhotoLauncher.launch()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun updatePhotoPreview() {
        if (capturedPhotoPath != null) {
            val file = File(capturedPhotoPath!!)
            if (file.exists()) {
                // UI feedback for attached photo as seen in screenshots
                binding.cardPhotoUpload.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.forest_green_light))
            }
        } else {
            binding.cardPhotoUpload.setCardBackgroundColor(Color.WHITE)
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveExpense.setOnClickListener {
            val amountInput = binding.etAmount.text.toString()
            val amount = CurrencyUtil.parseAmount(amountInput) ?: 0.0
            val desc = binding.etDescription.text.toString().trim()
            val currency = binding.spinnerCurrency.text.toString()
            
            // Comprehensive validation to prevent crashes and invalid data
            if (amount <= 0) {
                Toast.makeText(requireContext(), "Enter a valid amount greater than 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (desc.isEmpty()) {
                Toast.makeText(requireContext(), "Description cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCategoryId == null) {
                Toast.makeText(requireContext(), "Please assign a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val userId = authViewModel.getActiveUserId()
                val user = authViewModel.getActiveUser().value
                val baseCurrency = user?.baseCurrency ?: "ZAR"
                val exchangeRate = currencyRepository.getExchangeRate(currency, baseCurrency)

                val frequency = if (binding.switchRecurring.isChecked) {
                    when {
                        binding.chipDaily.isChecked -> "Daily"
                        binding.chipWeekly.isChecked -> "Weekly"
                        binding.chipFortnightly.isChecked -> "Fortnightly"
                        binding.chipMonthly.isChecked -> "Monthly"
                        binding.chipAnnually.isChecked -> "Annually"
                        else -> "Monthly"
                    }
                } else null

                val expense = Expense(
                    id = if (args.expenseId == -1L) 0 else args.expenseId,
                    userId = userId,
                    categoryId = selectedCategoryId,
                    amount = amount,
                    description = desc,
                    date = selectedDate,
                    startTime = startTime,
                    endTime = endTime,
                    receiptPhotoPath = capturedPhotoPath,
                    isRecurring = binding.switchRecurring.isChecked,
                    recurringFrequency = frequency,
                    nextRecurrenceDate = if (binding.switchRecurring.isChecked) {
                        DateUtil.calculateNextDate(selectedDate, frequency)
                    } else null,
                    currency = currency,
                    exchangeRateToBase = exchangeRate
                )
                
                Log.i(TAG, "Saving Expense: [${desc}] $currency $amount, Rate: $exchangeRate")
                viewModel.saveExpense(expense)
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
