package com.example.childtrackerapp.child.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.Athu.viewmodel.Logoutable
import com.example.childtrackerapp.child.data.ChildRepository
import com.example.childtrackerapp.child.helper.GeoFenceHelper
import com.example.childtrackerapp.service.LocationService
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChildViewModel @Inject constructor(
    app: Application,
    private val sessionManager: SessionManager
) : AndroidViewModel(app), Logoutable {

    private val auth = FirebaseAuth.getInstance()
    private val childId: String get() = auth.currentUser?.uid ?: "unknown_child"

    private val repository = ChildRepository(childId)

    val isSharingLocation = MutableLiveData(false)
    override suspend fun logout() {
        sessionManager.clearSession()
    }

    init {
        GeoFenceHelper.init(app.applicationContext)
        repository.startListeningFromParent()
        viewModelScope.launch {
            repository.voiceMessageFromParent.collectLatest { msg ->
                msg?.let { GeoFenceHelper.voiceAlert?.speak(it) }
            }
        }
    }

    fun startSharing(context: Context) {
        if (childId == "unknown_child") return
        val intent = Intent(context, LocationService::class.java)
        ContextCompat.startForegroundService(context, intent)
        isSharingLocation.value = true
    }

    fun stopSharing(context: Context) {
        val intent = Intent(context, LocationService::class.java)
        context.stopService(intent)
        isSharingLocation.value = false
    }

    fun sendVoiceToParent(msg: String) {
        viewModelScope.launch { repository.sendVoiceMessageToParent(msg) }
    }

    override fun onCleared() {
        super.onCleared()
        GeoFenceHelper.release()
    }
}
