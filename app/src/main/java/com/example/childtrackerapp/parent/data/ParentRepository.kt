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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.Calendar
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
                val name = appSnap.getString("name") ?: ""
                val packageName = appSnap.key ?: return@mapNotNull null
                val isAllowed = appSnap.child("allowed").getValue(Boolean::class.java) ?: false
                val usageTime = appSnap.getString("usageTime") ?: ""
                val iconBase64 = appSnap.child("iconBase64").getValue(String::class.java)
                val startTime = appSnap.child("startTime").getValue(String::class.java) ?: "00:00"
                val endTime = appSnap.child("endTime").getValue(String::class.java) ?: "23:59"
                val allowedDays = appSnap.child("allowedDays").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList()

                AppInfo(
                    name = name,
                    packageName = packageName,
                    iconBase64 = iconBase64,
                    allowed = isAllowed,
                    usageTime = usageTime,
                    startTime = startTime,
                    endTime = endTime,
                    allowedDays = allowedDays
                )
            }.sortedByDescending { app ->
                parseUsageTimeToMinutes(app.usageTime)
            }

        } catch (e: Exception) {
            Log.e("ParentRepo", "loadApps error: ${e.message}")
            emptyList()
        }
    }

    suspend fun updateAppSettings(childId: String, appInfo: AppInfo): Boolean {
        return try {
            val path = "blocked_items/$childId/apps/${appInfo.packageName}"
            val map = mapOf(
                "allowed" to appInfo.allowed,
                "startTime" to appInfo.startTime,
                "endTime" to appInfo.endTime,
                "allowedDays" to appInfo.allowedDays
            )
            db.child(path).updateChildren(map).await()
            true
        } catch (e: Exception) {
            false
        }
    }


    suspend fun getUsageStats(childId: String, filter: UsageFilter): UsageStats {
        return try {
            val dbRef = FirebaseDatabase.getInstance()
                .getReference("blocked_items")
                .child(childId)
                .child("apps")

            val appsSnapshot = dbRef.get().await()

            val allApps = mutableListOf<AppUsage>()

            val now = Calendar.getInstance()

            appsSnapshot.children.forEach { appSnap ->
                val packageName = appSnap.key ?: return@forEach
                val name = appSnap.getString("name").ifEmpty { packageName }

                val usageByDate = appSnap.child("usage")

                var totalMinutes = 0

                usageByDate.children.forEach { dateSnap ->
                    val dateStr = dateSnap.key ?: return@forEach
                    val usageStr = dateSnap.getValue(String::class.java)


                    if (isWithinFilter(dateStr, filter)) {
                        totalMinutes += parseUsageTimeToMinutes(usageStr!!)
                    }
                }

                if (totalMinutes > 0) {
                    allApps.add(AppUsage(packageName, name, totalMinutes))
                }
            }

            val sorted = allApps.sortedByDescending { it.timeMinutes }
            val topApps = sorted.take(5)

            val childSnapshot = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(childId)
                .get().await()

            val childName = childSnapshot.getString("name").ifEmpty { "Không rõ" }

            UsageStats(
                childName = childName,
                dateString = getDateString(filter),
                totalMinutes = sorted.sumOf { it.timeMinutes },
                topApps = topApps,
                allApps = sorted
            )

        } catch (e: Exception) {
            Log.e("ParentRepository", "Error: $e")
            UsageStats("Không rõ", "", 0, emptyList(), emptyList())
        }
    }
}
