package com.example.childtrackerapp.schedule.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.data.repository.ScheduleRepository
import com.example.childtrackerapp.schedule.model.Schedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditScheduleUiState(
    val schedule: Schedule? = null,
    val date: String = "",
    val title: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",
    val location: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EditScheduleUiState())
    val uiState: StateFlow<EditScheduleUiState> = _uiState.asStateFlow()
    
    fun loadSchedule(scheduleId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            repository.getScheduleById(scheduleId)
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Lỗi khi tải lịch trình: ${e.message}"
                        ) 
                    }
                }
                .collect { schedule ->
                    if (schedule != null) {
                        _uiState.update {
                            it.copy(
                                schedule = schedule,
                                date = schedule.date,
                                title = schedule.title,
                                startTime = schedule.startTime,
                                endTime = schedule.endTime,
                                description = schedule.description,
                                location = schedule.location,
                                isLoading = false,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Không tìm thấy lịch trình"
                            )
                        }
                    }
                }
        }
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
        val originalSchedule = state.schedule
        
        if (originalSchedule == null) {
            _uiState.update { it.copy(error = "Không tìm thấy lịch trình") }
            return
        }
        
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
                val updatedSchedule = originalSchedule.copy(
                    title = state.title,
                    startTime = state.startTime,
                    endTime = state.endTime,
                    description = state.description,
                    location = state.location
                )
                
                repository.updateSchedule(updatedSchedule)
                
                _uiState.update { 
                    it.copy(isLoading = false, isSaved = true) 
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
}
