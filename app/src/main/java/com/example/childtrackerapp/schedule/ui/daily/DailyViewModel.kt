package com.example.childtrackerapp.schedule.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.model.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DailyUiState(
    val selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {
    
    private val _selectedDate = MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
    
    private val _uiState = MutableStateFlow(DailyUiState())
    val uiState: StateFlow<DailyUiState> = _uiState.asStateFlow()
    
    init {
        observeSchedules()
    }
    
    private fun observeSchedules() {
        viewModelScope.launch {
            _selectedDate.flatMapLatest { date ->
                repository.getSchedulesForDate(date)
            }.catch { e ->
                _uiState.update { 
                    it.copy(error = e.message, isLoading = false) 
                }
            }.collect { schedules ->
                _uiState.update {
                    it.copy(
                        schedules = schedules,
                        selectedDate = _selectedDate.value,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
    
    fun previousDay() {
        val currentDate = LocalDate.parse(_selectedDate.value)
        _selectedDate.value = currentDate.minusDays(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    fun nextDay() {
        val currentDate = LocalDate.parse(_selectedDate.value)
        _selectedDate.value = currentDate.plusDays(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    fun selectDate(date: String) {
        _selectedDate.value = date
    }
    
    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteSchedule(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Thêm method này vào DailyViewModel

    fun setSelectedDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSchedules()
    }

    // Method loadSchedules() để reload lịch của ngày được chọn
    private fun loadSchedules() {
        viewModelScope.launch {
            repository.getSchedulesForDate(_uiState.value.selectedDate)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { schedules ->
                    _uiState.update {
                        it.copy(
                            schedules = schedules,
                            error = null
                        )
                    }
                }
        }
    }
}
