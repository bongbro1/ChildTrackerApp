package com.example.childtrackerapp.parent.ui.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.childtrackerapp.parent.ui.model.AppInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.childtrackerapp.parent.helper.toImageBitmapOrNull
import com.example.childtrackerapp.parent.ui.model.Child
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsUiState
import com.example.childtrackerapp.parent.ui.viewmodel.AllowedAppsViewModel
import com.example.childtrackerapp.service.ChildTrackerVpnService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowedAppsScreen(
    uiState: AllowedAppsUiState,
    onToggle: (packageName: String, allowed: Boolean) -> Unit,
    onBack: () -> Unit,
    onAddWebsite: (childId: String, website: String) -> Unit,
    onRemoveWebsite: (childId: String, website: String) -> Unit,
    onChildSelected: (childId: String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AllowedAppsViewModel = viewModel()
    var newWebsite by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Apps, 1: Web
    val tabs = listOf("Ứng dụng", "Web bị cấm")

    var blockedWebsites by remember { mutableStateOf(listOf<String>()) }

    val children = uiState.children
    var selectedChildId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(children) {
        if (selectedChildId == null && children.isNotEmpty()) {
            selectedChildId = children.first().uid
            onChildSelected(selectedChildId!!)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.height(16.dp))
        ChildDropdown(
            children = children,
            selectedChildId = selectedChildId,
            onChildSelected = { childId ->
                selectedChildId = childId
                // Làm gì đó khi chọn child
                onChildSelected(childId)
                val blockedWebsites = uiState.blockedWebsites
                ChildTrackerVpnService.instance?.setBlockedWebsites(blockedWebsites)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tab menu
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTabIndex) {
            0 -> { // Tab Apps
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.apps) { app ->
                            BlockedAppItem(
                                app = app,
                                onToggle = { allowed -> viewModel.toggleApp(selectedChildId as String,app.packageName, allowed)
                                    Toast.makeText(
                                        context,
                                        "${app.name} đã ${if (allowed) "cho phép" else "chặn"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }

            1 -> { // Tab Web bị cấm
                Column(modifier = Modifier.fillMaxSize()) {
                    // Input + Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        TextField(
                            value = newWebsite,
                            onValueChange = { newWebsite = it },
                            placeholder = { Text("Nhập địa chỉ website") },
                            modifier = Modifier
                                .weight(1f)
                                .height(53.dp),
                            singleLine = true,
                            shape = RectangleShape
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newWebsite.isNotBlank() && selectedChildId != null) {
                                    val websiteTrimmed = newWebsite.trim()

                                    // Cập nhật UI tạm thời
                                    blockedWebsites = blockedWebsites + websiteTrimmed

                                    // Gọi ViewModel với childId
                                    onAddWebsite(selectedChildId!!, websiteTrimmed)

                                    newWebsite = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RectangleShape,
                            modifier = Modifier.height(53.dp)
                        ) {
                            Text("Thêm")
                        }
                    }


                    Spacer(modifier = Modifier.height(16.dp))

                    var blockedWebsites = uiState.blockedWebsites

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(blockedWebsites) { site ->
                            BlockedWebsiteItem(
                                site = site,
                                onRemove = { removedSite ->
                                    blockedWebsites = blockedWebsites - removedSite
                                    onRemoveWebsite(selectedChildId!!, removedSite)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedWebsiteItem(
    site: String,
    onRemove: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = site,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onRemove(site) }
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                    contentDescription = "Xóa",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}



@Composable
fun BlockedAppItem(app: AppInfo, onToggle: (Boolean) -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            val iconBitmap = app.iconBase64.toImageBitmapOrNull()
            // Icon app
            iconBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 12.dp)
                )
            } ?: Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 12.dp)
            )// placeholder nếu không có icon

            // Tên app
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Tên app
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = app.usageTime ?: "0m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Switch
            Switch(
                checked = app.isAllowed,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDropdown(
    children: List<Child>,
    selectedChildId: String?,
    onChildSelected: (childId: String) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // selectedText sẽ tự động cập nhật khi selectedChildId hoặc children thay đổi
    val selectedText = remember(selectedChildId, children) {
        children.firstOrNull { it.uid == selectedChildId }?.name ?: "Chọn child"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = selectedText,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                children.forEach { child ->
                    DropdownMenuItem(
                        text = { Text(child.name) },
                        onClick = {
                            expanded = false
                            onChildSelected(child.uid)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAllowedAppsScreen() {
    val sampleApps = listOf(
        AppInfo("Messenger Kids", "com.facebook.messenger.kids", null, false, "50m"),
        AppInfo("Google Chrome", "com.android.chrome", null, true, "50m")
    )

    val uiState = AllowedAppsUiState(isLoading = false, apps = sampleApps)

    AllowedAppsScreen(
        uiState = uiState,
        onToggle = { pkg, allowed -> /* không làm gì trong preview */ },
        onBack = {},
        onAddWebsite = {  _, _ -> /* không làm gì */ },
        onRemoveWebsite = {  _, _ -> /* không làm gì */ },
        onChildSelected = { /* không làm gì */ }
    )
}
