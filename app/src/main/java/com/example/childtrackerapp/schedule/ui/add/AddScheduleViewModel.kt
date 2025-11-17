package com.example.childtrackerapp.schedule.ui.add

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.model.Schedule
import com.example.childtrackerapp.schedule.utils.ExcelParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ImportProgress(
    val isImporting: Boolean = false,
    val total: Int = 0,
    val imported: Int = 0,
    val errors: List<String> = emptyList(),
    val completed: Boolean = false
)

data class AddScheduleUiState(
    val date: String = "",
    val title: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",
    val location: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val importProgress: ImportProgress? = null
)

@HiltViewModel
class AddScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddScheduleUiState())
    val uiState: StateFlow<AddScheduleUiState> = _uiState.asStateFlow()

    fun setDate(date: String) {
        _uiState.update { it.copy(date = date) }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, error = null) }
    }

    fun updateStartTime(time: String) {
        _uiState.update { it.copy(startTime = time, error = null) }
    }

    fun updateEndTime(time: String) {
        _uiState.update { it.copy(endTime = time, error = null) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateLocation(location: String) {
        _uiState.update { it.copy(location = location) }
    }

    fun saveSchedule() {
        val state = _uiState.value

        // Validation
        if (state.title.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập tiêu đề") }
            return
        }

        if (state.startTime.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập thời gian bắt đầu") }
            return
        }

        if (state.endTime.isBlank()) {
            _uiState.update { it.copy(error = "Vui lòng nhập thời gian kết thúc") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val schedule = Schedule(
                    id = UUID.randomUUID().toString(),
                    title = state.title,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    description = state.description,
                    location = state.location,
                    date = state.date
                )

                repository.addSchedule(schedule)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        success = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Lỗi khi lưu lịch trình: ${e.message}"
                    )
                }
            }
        }
    }

    fun importFromExcel(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    importProgress = ImportProgress(isImporting = true),
                    error = null
                )
            }

            try {
                // Parse Excel file
                val parser = ExcelParser(context)
                val result = parser.parseExcelFile(uri)

                if (result.schedules.isEmpty() && result.errors.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            importProgress = ImportProgress(
                                isImporting = false,
                                errors = result.errors,
                                completed = true
                            ),
                            error = "Không thể import lịch trình. Vui lòng kiểm tra file Excel."
                        )
                    }
                    return@launch
                }

                // Update progress
                _uiState.update {
                    it.copy(
                        importProgress = ImportProgress(
                            isImporting = true,
                            total = result.schedules.size,
                            imported = 0
                        )
                    )
                }

                // Import schedules in batch
                repository.addScheduleBatch(result.schedules)

                // Update final state
                val successMessage = if (result.errors.isEmpty()) {
                    "Đã import thành công ${result.schedules.size} lịch trình"
                } else {
                    "Đã import ${result.schedules.size} lịch trình với ${result.errors.size} lỗi"
                }

                _uiState.update {
                    it.copy(
                        importProgress = ImportProgress(
                            isImporting = false,
                            total = result.schedules.size,
                            imported = result.schedules.size,
                            errors = result.errors,
                            completed = true
                        ),
                        success = result.schedules.isNotEmpty(),
                        isSaved = result.schedules.isNotEmpty(),
                        error = if (result.errors.isNotEmpty()) successMessage else null
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        importProgress = ImportProgress(
                            isImporting = false,
                            errors = listOf("Lỗi: ${e.message}"),
                            completed = true
                        ),
                        error = "Lỗi khi import file: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(success = false, isSaved = false) }
    }

    fun clearImportProgress() {
        _uiState.update { it.copy(importProgress = null) }
    }
}