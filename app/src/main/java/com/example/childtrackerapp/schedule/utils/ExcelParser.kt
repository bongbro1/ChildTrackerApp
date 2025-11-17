package com.example.childtrackerapp.schedule.utils

import android.content.Context
import android.net.Uri
import com.example.childtrackerapp.schedule.model.Schedule
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.usermodel.DateUtil
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class ExcelParser(private val context: Context) {

    data class ExcelParseResult(
        val schedules: List<Schedule>,
        val errors: List<String>
    )

    /**
     * Parse Excel file từ URI
     * Cấu trúc file Excel:
     * - Dòng 1: Header (Tiêu đề, Bắt đầu, Kết thúc, Mô tả, Địa điểm, Ngày)
     * - Dòng 2+: Dữ liệu
     */
    fun parseExcelFile(uri: Uri): ExcelParseResult {
        val schedules = mutableListOf<Schedule>()
        val errors = mutableListOf<String>()

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            if (inputStream == null) {
                errors.add("Không thể mở file Excel")
                return ExcelParseResult(emptyList(), errors)
            }

            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0) // Lấy sheet đầu tiên

            // Bỏ qua dòng header (dòng 0)
            for (rowIndex in 1 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(rowIndex) ?: continue

                try {
                    // Kiểm tra dòng trống
                    if (isRowEmpty(row)) continue

                    // Đọc dữ liệu từ các cột
                    val title = getCellValueAsString(row.getCell(0))
                    val startTime = getTimeValue(row.getCell(1))  // Xử lý đặc biệt cho time
                    val endTime = getTimeValue(row.getCell(2))    // Xử lý đặc biệt cho time
                    val description = getCellValueAsString(row.getCell(3))
                    val location = getCellValueAsString(row.getCell(4))
                    val date = getCellValueAsString(row.getCell(5))

                    // Log để debug
                    android.util.Log.d("ExcelParser", "Dòng ${rowIndex + 1}: title=$title, date=$date, startTime=$startTime, endTime=$endTime")

                    // Validate dữ liệu
                    val validationError = validateScheduleData(
                        title, startTime, endTime, description, location, date, rowIndex + 1
                    )

                    if (validationError != null) {
                        errors.add(validationError)
                        continue
                    }

                    // Format dữ liệu
                    val formattedStartTime = formatTime(startTime)
                    val formattedEndTime = formatTime(endTime)
                    val formattedDate = formatDate(date)

                    // Log để debug
                    android.util.Log.d("ExcelParser", "Formatted: date=$formattedDate, startTime=$formattedStartTime, endTime=$formattedEndTime")

                    if (formattedStartTime == null || formattedEndTime == null || formattedDate == null) {
                        errors.add("Dòng ${rowIndex + 1}: Định dạng thời gian hoặc ngày không hợp lệ (date='$date', start='$startTime', end='$endTime')")
                        continue
                    }

                    // Tạo Schedule object
                    val schedule = Schedule(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        startTime = formattedStartTime,
                        endTime = formattedEndTime,
                        description = description,
                        location = location,
                        date = formattedDate
                    )

                    schedules.add(schedule)

                } catch (e: Exception) {
                    errors.add("Dòng ${rowIndex + 1}: Lỗi đọc dữ liệu - ${e.message}")
                    android.util.Log.e("ExcelParser", "Error at row ${rowIndex + 1}", e)
                }
            }

            workbook.close()
            inputStream.close()

        } catch (e: Exception) {
            errors.add("Lỗi khi đọc file Excel: ${e.message}")
        }

        return ExcelParseResult(schedules, errors)
    }

    private fun isRowEmpty(row: org.apache.poi.ss.usermodel.Row): Boolean {
        for (cellIndex in 0 until 6) {
            val cell = row.getCell(cellIndex)
            if (cell != null && getCellValueAsString(cell).isNotBlank()) {
                return false
            }
        }
        return true
    }

    private fun getCellValueAsString(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""

        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.trim()
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Format date to dd/MM/yyyy
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(cell.dateCellValue)
                } else {
                    // Kiểm tra nếu là số nguyên
                    val numValue = cell.numericCellValue
                    if (numValue == numValue.toLong().toDouble()) {
                        numValue.toLong().toString()
                    } else {
                        numValue.toString()
                    }
                }
            }
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                try {
                    if (cell.cachedFormulaResultType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            sdf.format(cell.dateCellValue)
                        } else {
                            cell.numericCellValue.toString()
                        }
                    } else {
                        cell.stringCellValue.trim()
                    }
                } catch (e: Exception) {
                    try {
                        cell.numericCellValue.toString()
                    } catch (ex: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }

    /**
     * Đọc giá trị thời gian từ cell - xử lý đặc biệt cho time format của Excel
     */
    private fun getTimeValue(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""

        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue.trim()
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Excel lưu time dưới dạng fraction of day
                    // Ví dụ: 7:00 = 0.291666... (7/24)
                    val date = cell.dateCellValue
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = date
                    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val minute = calendar.get(java.util.Calendar.MINUTE)

                    // Format thành HH:mm
                    String.format("%02d:%02d", hour, minute)
                } else {
                    // Nếu là số thuần, coi như text
                    val numValue = cell.numericCellValue
                    if (numValue == numValue.toLong().toDouble()) {
                        numValue.toLong().toString()
                    } else {
                        numValue.toString()
                    }
                }
            }
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                try {
                    if (cell.cachedFormulaResultType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                        if (DateUtil.isCellDateFormatted(cell)) {
                            val date = cell.dateCellValue
                            val calendar = java.util.Calendar.getInstance()
                            calendar.time = date
                            val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                            val minute = calendar.get(java.util.Calendar.MINUTE)
                            String.format("%02d:%02d", hour, minute)
                        } else {
                            cell.numericCellValue.toString()
                        }
                    } else {
                        cell.stringCellValue.trim()
                    }
                } catch (e: Exception) {
                    ""
                }
            }
            else -> ""
        }
    }

    private fun validateScheduleData(
        title: String,
        startTime: String,
        endTime: String,
        description: String,
        location: String,
        date: String,
        rowNumber: Int
    ): String? {
        if (title.isBlank()) {
            return "Dòng $rowNumber: Tiêu đề không được để trống"
        }
        if (startTime.isBlank()) {
            return "Dòng $rowNumber: Thời gian bắt đầu không được để trống"
        }
        if (endTime.isBlank()) {
            return "Dòng $rowNumber: Thời gian kết thúc không được để trống"
        }
        if (date.isBlank()) {
            return "Dòng $rowNumber: Ngày không được để trống"
        }
        return null
    }

    /**
     * Format time từ nhiều định dạng về HH:mm
     * Hỗ trợ: "07:00", "7:00", "07:00:00", "7h00", "7h"
     */
    private fun formatTime(timeStr: String): String? {
        val cleaned = timeStr.trim()

        // Pattern: HH:mm hoặc H:mm
        val pattern1 = Regex("""^(\d{1,2}):(\d{2})(?::\d{2})?$""")
        pattern1.find(cleaned)?.let {
            val hour = it.groupValues[1].toInt()
            val minute = it.groupValues[2].toInt()
            if (hour in 0..23 && minute in 0..59) {
                return String.format("%02d:%02d", hour, minute)
            }
        }

        // Pattern: 7h00 hoặc 7h
        val pattern2 = Regex("""^(\d{1,2})h(\d{0,2})$""")
        pattern2.find(cleaned)?.let {
            val hour = it.groupValues[1].toInt()
            val minute = if (it.groupValues[2].isNotEmpty()) it.groupValues[2].toInt() else 0
            if (hour in 0..23 && minute in 0..59) {
                return String.format("%02d:%02d", hour, minute)
            }
        }

        return null
    }

    /**
     * Format date từ nhiều định dạng về yyyy-MM-dd
     * Hỗ trợ: "17/11/2025", "17-11-2025", "2025-11-17", "2025/11/17", Excel date number
     */
    private fun formatDate(dateStr: String): String? {
        val cleaned = dateStr.trim()

        // Thử parse với nhiều format
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("d/M/yyyy", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
            SimpleDateFormat("d-M-yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()),
            SimpleDateFormat("d/M/yy", Locale.getDefault())
        )

        for (format in formats) {
            format.isLenient = false
            try {
                val date = format.parse(cleaned)
                if (date != null) {
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    return outputFormat.format(date)
                }
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }
}