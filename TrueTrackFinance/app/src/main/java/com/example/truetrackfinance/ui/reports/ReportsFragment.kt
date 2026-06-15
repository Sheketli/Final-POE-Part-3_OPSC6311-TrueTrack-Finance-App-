package com.example.truetrackfinance.ui.reports

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.truetrackfinance.R
import com.example.truetrackfinance.data.model.CategorySpending
import com.example.truetrackfinance.databinding.FragmentReportsBinding
import com.example.truetrackfinance.ui.viewmodel.ReportsViewModel
import com.example.truetrackfinance.ui.viewmodel.AuthViewModel
import com.example.truetrackfinance.ui.viewmodel.ReportPeriod
import com.example.truetrackfinance.util.CurrencyUtil
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ReportsFragment"

/**
 * ReportsFragment visualizes user spending data using interactive charts.
 */
@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ReportsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    
    private lateinit var varianceAdapter: VarianceAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Initializing high-fidelity analytical views")
        
        val userId = authViewModel.getActiveUserId()
        viewModel.initialise(userId)
        
        setupCharts()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupCharts() {
        // --- 1. Interactive Donut Chart ---
        binding.pieChart.apply {
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 75f
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setTouchEnabled(true)
            animateY(1000)
        }
        
        // --- 2. Stacked Bar Chart for Daily Trends ---
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            
            // Interactive features: Pinch-to-zoom and Drag-to-scroll
            isDoubleTapToZoomEnabled = true
            setPinchZoom(true)
            setScaleEnabled(true)
            setDragEnabled(true)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelRotationAngle = -45f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateX(1000)
        }
    }

    private fun setupRecyclerView() {
        varianceAdapter = VarianceAdapter()
        binding.rvVarianceTable.adapter = varianceAdapter
        binding.rvVarianceTable.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners() {
        binding.chipGroupReportPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            val period = when (checkedIds.firstOrNull()) {
                R.id.chip_this_month -> ReportPeriod.THIS_MONTH
                R.id.chip_last_month -> ReportPeriod.LAST_MONTH
                R.id.chip_last_3_months -> ReportPeriod.LAST_3_MONTHS
                else -> ReportPeriod.THIS_MONTH
            }
            viewModel.setPeriod(period)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            Log.v(TAG, "UI Update: Rendering data for ${state.dailySpending.size} spending days")
            
            updateDonutChart(state)
            updateStackedBarChart(state)
            
            varianceAdapter.submitList(state.categoryProgress)
            binding.tvReportsTotalSpent.text = CurrencyUtil.format(state.totalSpent)
            binding.tvTransactionCount.text = state.totalTransactionCount.toString()
        }
    }

    private fun updateDonutChart(state: com.example.truetrackfinance.ui.viewmodel.ReportsUiState) {
        val pieEntries = state.categorySpending.map { PieEntry(it.totalSpent.toFloat(), it.categoryName) }
        val pieDataSet = PieDataSet(pieEntries, "").apply {
            colors = state.categorySpending.map { Color.parseColor(it.colorHex) }
            sliceSpace = 2f
            setDrawValues(false)
        }
        binding.pieChart.data = PieData(pieDataSet)
        binding.pieChart.centerText = "Total\n${CurrencyUtil.format(state.totalSpent)}"
        binding.pieChart.setCenterTextSize(16f)
        binding.pieChart.invalidate()
    }

    private fun updateStackedBarChart(state: com.example.truetrackfinance.ui.viewmodel.ReportsUiState) {
        val barEntries = mutableListOf<BarEntry>()
        val colorMap = mutableMapOf<Long, Int>() // Track category colors for consistent stacking

        // Collect all categories involved in this period to build a consistent color palette
        val allCats = state.dailySpending.flatMap { it.categoryBreakdown }.distinctBy { it.categoryId }
        allCats.forEach { cat -> colorMap[cat.categoryId] = Color.parseColor(cat.colorHex) }
        
        // Formatter for dates
        val dateInFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateOutFormat = SimpleDateFormat("dd MMM", Locale.US)

        state.dailySpending.forEachIndexed { index, daily ->
            // In a stacked bar, values are provided as a float array
            // To maintain color alignment, we need to handle consistent ordering if categories vary per day
            val values = daily.categoryBreakdown.map { it.totalSpent.toFloat() }.toFloatArray()
            barEntries.add(BarEntry(index.toFloat(), values))
        }

        // Note: For advanced stacking with exact color mapping, a more rigid data structure is needed.
        // For the prototype, we use the distinct category colors in order.
        val barDataSet = BarDataSet(barEntries, "Daily Spending").apply {
            colors = allCats.map { Color.parseColor(it.colorHex) }
            setDrawValues(false)
        }

        binding.barChart.apply {
            data = BarData(barDataSet)
            
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val idx = value.toInt()
                    return if (idx in state.dailySpending.indices) {
                        try {
                            val date = dateInFormat.parse(state.dailySpending[idx].day)
                            dateOutFormat.format(date!!)
                        } catch (_: Exception) { "" }
                    } else ""
                }
            }

            // --- 3. Dashed Budget Target Reference Line ---
            axisLeft.removeAllLimitLines()
            if (state.dailyBudgetTarget > 0) {
                val ll = LimitLine(state.dailyBudgetTarget.toFloat(), "Daily Target").apply {
                    lineWidth = 2f
                    enableDashedLine(10f, 10f, 0f)
                    labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                    textSize = 10f
                    lineColor = ContextCompat.getColor(requireContext(), R.color.warning)
                }
                axisLeft.addLimitLine(ll)
            }
            
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
