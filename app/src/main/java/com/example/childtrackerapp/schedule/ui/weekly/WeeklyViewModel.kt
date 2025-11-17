package com.example.childtrackerapp.schedule.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.model.DaySchedules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WeeklyUiState(
    val weekSchedules: List<DaySchedules> = emptyList(),
    val today: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WeeklyViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyUiState())
    val uiState: StateFlow<WeeklyUiState> = _uiState.asStateFlow()

    init {
        loadWeekSchedules()
    }

    private fun loadWeekSchedules() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val mondayDate = getMondayOfCurrentWeek()

            repository.getSchedulesForWeek(mondayDate)
                .catch { e ->
                    _uiState.update {
                        it.copy(error = e.message, isLoading = false)
                    }
                }
                .collect { weekSchedulesMap ->
                    val daySchedules = weekSchedulesMap.map { (date, schedules) ->
                        DaySchedules(
                            date = date,
                            dayName = getDayName(date),
                            schedules = schedules
                        )
                    }

                    _uiState.update {
                        it.copy(
                            weekSchedules = daySchedules,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun getMondayOfCurrentWeek(): String {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek
        val daysToSubtract = if (dayOfWeek == DayOfWeek.SUNDAY) 6 else dayOfWeek.value - 1
        val monday = today.minusDays(daysToSubtract.toLong())
        return monday.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun getDayName(dateStr: String): String {
        val date = LocalDate.parse(dateStr)
        val dayNames = listOf(
            "Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư",
            "Thứ năm", "Thứ sáu", "Thứ bảy"
        )
        val dayIndex = if (date.dayOfWeek == DayOfWeek.SUNDAY) 0 else date.dayOfWeek.value
        return dayNames[dayIndex]
    }
}
