package com.example.childtrackerapp.parent.data

import android.net.Uri
import android.util.Log
import com.example.childtrackerapp.model.ChildLocation
import com.example.childtrackerapp.model.User
import com.example.childtrackerapp.model.VoiceMessage
import com.example.childtrackerapp.parent.ui.model.AppInfo
import com.example.childtrackerapp.parent.ui.model.AppUsage
import com.example.childtrackerapp.parent.ui.model.Child
import com.example.childtrackerapp.parent.ui.model.UsageFilter
import com.example.childtrackerapp.parent.ui.model.UsageStats
import com.example.childtrackerapp.utils.getDateString
import com.example.childtrackerapp.utils.isWithinFilter
import com.example.childtrackerapp.utils.parseUsageTimeToMinutes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ParentRepository @Inject constructor() {

    private val db = FirebaseDatabase.getInstance().reference

    // 🔹 Lưu vị trí của con
    private val _childLocations = MutableStateFlow<Map<String, ChildLocation>>(emptyMap())
    val childLocations: StateFlow<Map<String, ChildLocation>> = _childLocations

    // 🔹 Lưu tin nhắn từ con
    private val _voiceMessageFromChild = MutableStateFlow<String?>(null)
    val voiceMessageFromChild: StateFlow<String?> = _voiceMessageFromChild

    fun listenChildrenLocations(parentId: String) {
        // Lấy user theo parentId
        db.child("users").orderByChild("parentId").equalTo(parentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val childIds = snapshot.children.mapNotNull { it.key }

                    if (childIds.isEmpty()) {
                        _childLocations.value = emptyMap()
                        return
                    }

                    // Lắng nghe vị trí của từng con
                    childIds.forEach { childId ->
                        db.child("locations").child(childId)
                            .addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(locSnap: DataSnapshot) {
                                    val lat = locSnap.child("lat").getValue(Double::class.java)
                                    val lng = locSnap.child("lng").getValue(Double::class.java)
                                    val name = locSnap.child("name").getValue(String::class.java)

                                    if (lat != null && lng != null) {
                                        val newMap = _childLocations.value.toMutableMap()
                                        newMap[childId] = ChildLocation(lat, lng,name)
                                        _childLocations.value = newMap
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }



    // 🔹 Lắng nghe tin nhắn từ con
    fun startListeningFromChild(childId: String) {
        db.child("messages").child(childId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msg = snapshot.getValue(VoiceMessage::class.java)
                msg?.let {
                    if (it.from == "child") {
                        _voiceMessageFromChild.value = it.text
                        db.child("messages").child(childId).removeValue()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun createChildAccount(
        name: String,
        email: String,
        password: String,
        parentId: String
    ): User {
        val uid = FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .await().user?.uid ?: throw Exception("Cannot create child")

        val child = User(
            uid = uid,
            name = name,
            email = email,
            role = "child",
            parentId = parentId
        )

        FirebaseDatabase.getInstance().getReference("users")
            .child(uid)
            .setValue(child)

        return child
    }

    suspend fun sendVoiceMessage(childId: String, audioFile: File): String {
        val storageRef = FirebaseStorage.getInstance()
            .reference.child("voiceMessages/$childId/${audioFile.name}")

        val url = storageRef.putFile(Uri.fromFile(audioFile))
            .await()
            .storage.downloadUrl.await()
            .toString()

        val messageData = mapOf(
            "audioUrl" to url,
            "timestamp" to System.currentTimeMillis(),
            "from" to "parent"
        )

        FirebaseDatabase.getInstance().reference
            .child("messages")
            .child(childId)
            .push()
            .setValue(messageData)

        return url  // 🔥 MUST RETURN
    }


    // allow app
    suspend fun getChildrenOfParent(parentId: String): List<Child> {
        return try {
            val usersRef = FirebaseDatabase.getInstance().getReference("users")
            val snapshot =
                usersRef.orderByChild("parentId")
                .equalTo(parentId)
                .get()
                .await()

            snapshot.children.mapNotNull { childSnap ->
                val role = childSnap.child("role").getValue(String::class.java)
                val uid = childSnap.child("uid").getValue(String::class.java)
                val name = childSnap.child("name").getValue(String::class.java)

                if ((role == "child" || role == "con") && uid != null) {
                    Child(uid = uid, name = name ?: "Không tên")
                } else null
            }
        } catch (e: Exception) {
            Log.e("ChildrenRepository", "Error fetching children", e)
            emptyList()
        }
    }

    fun DataSnapshot.getString(key: String): String = child(key).getValue(String::class.java) ?: ""
    fun DataSnapshot.getBoolean(key: String): Boolean = child(key).getValue(Boolean::class.java) ?: true
    fun DataSnapshot.getInt(key: String): Int = child(key).getValue(Int::class.java) ?: 0

    suspend fun loadApps(childId: String): List<AppInfo> {
        return try {
            val ref = FirebaseDatabase.getInstance().getReference("blocked_items")
            val snapshot = ref.child(childId).get().await()

            snapshot.child("apps").children.mapNotNull { appSnap ->
                val name = appSnap.getString("name")
                val packageName = appSnap.key ?: return@mapNotNull null
                val isAllowed = appSnap.getBoolean("allowed")
                val usageTime = appSnap.getString("usageTime")
                val iconBase64 = appSnap.child("iconBase64").getValue(String::class.java)

                AppInfo(name, packageName, iconBase64, isAllowed, usageTime)
            }

        } catch (e: Exception) {
            Log.e("ParentRepo", "loadApps error: ${e.message}")
            emptyList<AppInfo>()
        }
    }


    suspend fun getUsageStats(childId: String, filter: UsageFilter = UsageFilter.DAY): UsageStats {
        return try {
            Log.d("ParentRepository", "Fetching usage stats for childId: $childId, filter: $filter")
            val dbRef = FirebaseDatabase.getInstance().getReference("blocked_items")
            val appsSnapshot = dbRef.child(childId).child("apps").get().await()
            Log.d("ParentRepository", "Apps snapshot fetched, children count: ${appsSnapshot.childrenCount}")

            // Parse usage time & lọc theo filter
            val allApps = appsSnapshot.children.mapNotNull { appSnap ->
                val packageName = appSnap.key ?: return@mapNotNull null
                val name = appSnap.getString("name").ifEmpty { packageName }
                val usageTimeStr = appSnap.getString("usageTime")
                val minutes = parseUsageTimeToMinutes(usageTimeStr)
                Log.d("ParentRepository", "App: $name, usageTimeStr: $usageTimeStr, minutes: $minutes")



                // Lọc theo filter nếu cần
                if (minutes > 0 && isWithinFilter(usageTimeStr, filter)) {
                    AppUsage(packageName, name, minutes)
                } else null
            }.sortedByDescending { it.timeMinutes }

            val totalMinutes = allApps.sumOf { it.timeMinutes }
            val topApps = allApps.take(5)

            Log.d("ParentRepository", "Total minutes: $totalMinutes, topApps: ${topApps.map { it.name }}")

            // Lấy tên child
            val childSnapshot = FirebaseDatabase.getInstance()
                .getReference("users").child(childId).get().await()
            val childName = childSnapshot.getString("name").ifEmpty { "Không rõ" }

            UsageStats(
                childName = childName,
                dateString = getDateString(filter),
                totalMinutes = totalMinutes,
                topApps = topApps,
                allApps = allApps
            )

        } catch (e: Exception) {
            Log.e("ParentRepository", "getUsageStats error: ${e.message}")
            UsageStats(
                childName = "Không rõ",
                dateString = "",
                totalMinutes = 0,
                topApps = emptyList(),
                allApps = emptyList()
            )
        }
    }



}
