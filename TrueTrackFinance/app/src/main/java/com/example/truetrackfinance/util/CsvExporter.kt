package com.example.truetrackfinance.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.example.truetrackfinance.data.model.ExpenseWithCategory
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CsvExporter"

/**
 * CsvExporter handles the generation of a data portability file.
 * Requirement: Profile screen must provide Export to CSV for data portability.
 */
object CsvExporter {

    /**
     * Requirement: Export all user expenses to a CSV file.
     * Extracts complex database relationships into a flat, readable file.
     */
    fun exportExpenses(context: Context, expenses: List<ExpenseWithCategory>) {
        try {
            Log.d(TAG, "Process: Starting CSV generation for ${expenses.size} records")
            
            // 1. Create a unique timestamped filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "TrueTrack_Finance_Export_$timestamp.csv"
            val file = File(context.cacheDir, fileName)

            // 2. Open FileWriter and write data row by row
            FileWriter(file).use { writer ->
                // Write standardized CSV Header
                writer.append("Date,Description,Category,Amount,Currency,Recurring\n")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                expenses.forEach { e ->
                    val date = dateFormat.format(Date(e.date))
                    // Clean description of commas to prevent cell break-out
                    val desc = e.description.replace(",", ";") 
                    val cat = e.categoryName ?: "Uncategorised"
                    val amount = String.format(Locale.US, "%.2f", e.amount)
                    val recurring = if (e.isRecurring) "Yes" else "No"

                    writer.append("$date,$desc,$cat,$amount,${e.currency},$recurring\n")
                }
            }

            Log.i(TAG, "Process: Export file created at ${file.absolutePath}")
            
            // 3. Trigger the Android system share sheet
            shareFile(context, file)

        } catch (e: Exception) {
            Log.e(TAG, "Critical Fault: Failed to export CSV file", e)
        }
    }

    /** Uses FileProvider to securely share the cache-stored CSV with other apps. */
    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.file_provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "TrueTrack Finance Transaction Audit")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        Log.v(TAG, "UI Action: Launching system share sheet for data portability")
        context.startActivity(Intent.createChooser(intent, "Save or Send Your Data"))
    }
}
