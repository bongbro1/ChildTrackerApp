package com.example.childtrackerapp.parent.ui.viewmodel


import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.childtrackerapp.parent.ui.model.AppInfo
import com.example.childtrackerapp.parent.ui.model.Child
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.database.FirebaseDatabase
import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.childtrackerapp.Athu.data.SessionManager
import com.example.childtrackerapp.parent.helper.toBitmapFromBase64
import com.google.firebase.database.DataSnapshot
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.util.Calendar

class AllowedAppsViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(AllowedAppsUiState())
    val uiState: StateFlow<AllowedAppsUiState> = _uiState

    private val _blockedWebsites = mutableStateListOf<String>()
    val blockedWebsites: List<String> get() = _blockedWebsites

    private val sessionManager = SessionManager(app)
    private val currentParentId: String? = sessionManager.getUserId()
    var selectedChildId: String? = null

    fun DataSnapshot.getString(key: String): String = child(key).getValue(String::class.java) ?: ""
    fun DataSnapshot.getBoolean(key: String): Boolean = child(key).getValue(Boolean::class.java) ?: true

    private val fb = FirebaseDatabase.getInstance().getReference("blocked_items");

    init {
        loadChildren()
    }

    fun onChildSelected(childId: String) {
        selectedChildId = childId
        loadApps(childId)
        loadBlockedWebsites(childId)
    }

    private fun loadChildren() {
        viewModelScope.launch {
            try {
                val usersRef = FirebaseDatabase.getInstance().getReference("users")
                val snapshot = usersRef.orderByChild("parentId").equalTo(currentParentId).get().await()
                val childrenList = snapshot.children.mapNotNull { childSnap ->

                    val role = childSnap.child("role").getValue(String::class.java)
                    val uid = childSnap.child("uid").getValue(String::class.java)
                    val name = childSnap.child("name").getValue(String::class.java)
                    if ((role == "child" || role == "con") && uid != null) {
                        Child(uid = uid, name = name ?: "Không tên")
                    } else null
                }
                _uiState.value = _uiState.value.copy(children = childrenList)
            } catch (e: Exception) {
                // log lỗi
                Log.e("AllowedAppsVM", "Failed to load children", e)
            }
        }
    }

    fun loadApps(childId: String) {
        fb.child(childId).get()
            .addOnSuccessListener { snapshot ->
                val appsList = snapshot.child("apps").children.mapNotNull { appSnap ->
                    val name = appSnap.getString("name")
                    val packageName = appSnap.key ?: return@mapNotNull null
                    val isAllowed = appSnap.getBoolean("allowed")
                    val usageTime = appSnap.getString("usageTime")
                    val iconBase64 = appSnap.child("iconBase64").getValue(String::class.java)

                    AppInfo(name, packageName, iconBase64, isAllowed, usageTime)
                }

                val websites = snapshot.child("websites").children.mapNotNull { siteSnap ->
                    if (siteSnap.value == true) siteSnap.key?.replace("_", ".") else null
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        apps = appsList,
                        blockedWebsites = websites,
                        isLoading = false
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("DEBUG", "Failed to load blocked items: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
    }

    fun loadBlockedWebsites(childId: String) {
        fb.child(childId).child("websites").get().addOnSuccessListener { snapshot ->

            val blockedSites = mutableListOf<String>()
            snapshot.children.forEach { child ->
                val site = child.key?.replace("_", ".") ?: ""
                if (site.isNotEmpty()) blockedSites.add(site)
            }
            // Cập nhật UI state riêng cho blockedWebsites
            _uiState.update {
                it.copy(blockedWebsites = blockedSites)
            }

        }.addOnFailureListener { exception ->
            Log.e("DEBUG", "Failed to load blocked websites: ${exception.message}")
        }
    }

    fun toggleApp(selectedId: String, packageName: String, allowed: Boolean) {

        // Cập nhật lên Firebase
        fb.child(selectedId).child("apps").child(packageName).child("allowed").setValue(allowed)
        .addOnSuccessListener {

            loadApps(selectedId)
        }
        .addOnFailureListener { e ->
            Log.e("AllowedAppsVM", "Failed to update $packageName: ${e.message}")
        }
    }

    fun setAppAllowed(pkg: String, allowed: Boolean) {
        fb.child(pkg).setValue(allowed)
    }

    fun addBlockedWebsite(childId: String, website: String) {
        if (!_blockedWebsites.contains(website)) {
            _blockedWebsites.add(website)
            fb.child(childId)
                .child("websites")
                .child(website.replace(".", "_"))
                .setValue(true)

            _uiState.update { it.copy(blockedWebsites = _blockedWebsites.toList()) }
        }
    }
    fun removeBlockedWebsite(childId: String, website: String) {
        _blockedWebsites.remove(website)

        fb.child(childId)
            .child("websites")
            .child(website.replace(".", "_"))
            .removeValue()

        _uiState.update { it.copy(blockedWebsites = _blockedWebsites.toList()) }
    }

}

data class AllowedAppsUiState(
    val isLoading: Boolean = true,
    val children: List<Child> = emptyList(),
    val apps: List<AppInfo> = emptyList(),
    val blockedWebsites: List<String> = emptyList()
)


//"blocked_items": {
//    "R2Uat1htnfhVMngBhJrPir9rgaJ3": {
//        "websites": {
//        "quanlynhansu_io_vn": true
//    }
//    },
//    "zf8ZExtPIFNzVcR8U9U5TMYW79l1": {
//        "apps": [
//        {
//            "allowed": true,
//            "iconBase64": "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\n",
//            "name": "ChildTrackerApp",
//            "packageName": "com.example.childtrackerapp",
//            "usageTime": "1h 10m"
//        },
//        {
//            "allowed": true,
//            "iconBase64": "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\n",
//            "name": "Lịch",
//            "packageName": "com.samsung.android.calendar",
//            "usageTime": "0h 0m"
//        }
//        ],
//        "websites": {
//        "facebook_com": true
//    }
//    }
//},
