package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import com.example.data.WhitelistContact
import com.example.ui.theme.RoyalBlue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.SleekBackground
import com.example.ui.theme.SleekSurface
import com.example.ui.theme.SleekPrimary
import com.example.ui.theme.SleekSecondary
import com.example.ui.theme.SleekTertiary
import com.example.ui.theme.SleekPinkAccent
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekDarkText
import com.example.ui.theme.SleekMutedText
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WhiteText
import com.example.ui.theme.MutedText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val whitelist by viewModel.whitelistContacts.collectAsState()
    val phonebookContacts by viewModel.deviceContacts.collectAsState()
    val isPhonebookLoading by viewModel.isDeviceContactsLoading.collectAsState()
    
    // Permission state checks
    var hasContactsPermission by remember { mutableStateOf(false) }
    var hasPhoneStatePermission by remember { mutableStateOf(false) }
    var hasCallLogPermission by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    
    // Helper to refresh permission values
    fun checkPermissions() {
        val nextContacts = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val nextPhoneState = context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val nextCallLog = context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        val nextOverlay = Settings.canDrawOverlays(context)
        
        hasContactsPermission = nextContacts
        hasPhoneStatePermission = nextPhoneState
        hasCallLogPermission = nextCallLog
        hasOverlayPermission = nextOverlay
        
        if (nextContacts && phonebookContacts.isEmpty() && !isPhonebookLoading) {
            viewModel.loadPhonebookContacts(context)
        }
    }
    
    // Check initially
    LaunchedEffect(Unit) {
        checkPermissions()
    }
    
    // Refresh permission status on resuming of activity (e.g. from Settings pages)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Permission request launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        checkPermissions()
        val allGranted = results.values.all { it }
        if (allGranted) {
            Toast.makeText(context, "تم منح صلاحيات النظام بنجاح!", Toast.LENGTH_SHORT).show()
            viewModel.loadPhonebookContacts(context)
        } else {
            Toast.makeText(context, "يرجى منح الصلاحيات لتشغيل التنبيهات بدون مشاكل", Toast.LENGTH_LONG).show()
        }
    }
    
    // Modals & Bottom sheets controllers
    var showAddSelectorSheet by remember { mutableStateOf(false) }
    var showPhonebookPicker by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    
    val bottomSheetState = rememberModalBottomSheetState()
    
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = SleekBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSelectorSheet = true },
                containerColor = SleekPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_contact_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة جهة اتصال")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SleekBackground)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Brand Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    HeaderBrandSection()
                }
                
                // Permission Status Board
                item {
                    PermissionsDashboard(
                        hasContacts = hasContactsPermission,
                        hasCallState = hasPhoneStatePermission,
                        hasCallLog = hasCallLogPermission,
                        hasOverlay = hasOverlayPermission,
                        onRequestSystemPermissions = {
                            permissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG
                                )
                            )
                        },
                        onRequestOverlayPermission = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
                
                // Contact Whitelist Title Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Add shortcut (Left side)
                        Text(
                            text = "إضافة جديد +",
                            color = SleekPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showAddSelectorSheet = true }
                                .padding(8.dp)
                        )
                        // Section title (Right side)
                        Text(
                            text = "قائمة التنبيه الفوري (Priority)",
                            color = SleekDarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Whitelist Body
                if (whitelist.isEmpty()) {
                    item {
                        EmptyWhitelistState()
                    }
                } else {
                    items(whitelist, key = { it.id }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onToggleEnabled = { viewModel.toggleContactEnabled(contact) },
                            onDelete = { viewModel.removeContact(contact.id) }
                        )
                    }
                }
                
                // Developer Rights sector at the footer of the scroll area
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    DeveloperRightsCard(context)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
        
        // --- BOTTOM SHEETS & DIALOGS ---
        
        // Choice sheet: Device phonebook vs Manual fields
        if (showAddSelectorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSelectorSheet = false },
                sheetState = bottomSheetState,
                containerColor = SleekSurface,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .background(SleekBorder, CircleShape)
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "كيف ترغب في إضافة رقم الهاتف؟",
                        color = SleekDarkText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Phone book Import Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showAddSelectorSheet = false
                                    if (hasContactsPermission) {
                                        viewModel.loadPhonebookContacts(context)
                                        showPhonebookPicker = true
                                    } else {
                                        Toast
                                            .makeText(
                                                context,
                                                "يرجى تمكين صلاحية جهات الاتصال أولاً",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "دفتر الهاتف",
                                    color = SleekDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        
                        // Manual Entry Option
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showAddSelectorSheet = false
                                    showManualAddDialog = true
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, SleekBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = SleekPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "كتابة يدوية",
                                    color = SleekDarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
        
        // Picker dialogue for phone book contacts
        if (showPhonebookPicker) {
            PhonebookPickerDialog(
                contacts = phonebookContacts,
                isLoading = isPhonebookLoading,
                onDismiss = { showPhonebookPicker = false },
                onContactSelected = { deviceContact ->
                    viewModel.addContact(deviceContact.name, deviceContact.phoneNumber)
                    showPhonebookPicker = false
                    Toast.makeText(context, "تمت إضافة ${deviceContact.name} للتنبيهات", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        // Manual form dialog
        if (showManualAddDialog) {
            ManualAddDialog(
                onDismiss = { showManualAddDialog = false },
                onAdd = { name, number ->
                    viewModel.addContact(name, number)
                    showManualAddDialog = false
                    Toast.makeText(context, "تمت إضافة $name للتنبيهات", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun HeaderBrandSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings / Info badge (Left-aligned)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SleekSecondary)
                .clickable {
                    // Quick info context
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "حول البرنامج",
                tint = Color(0xFF21005D),
                modifier = Modifier.size(20.dp)
            )
        }

        // Brand logo and title column (Right-aligned)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "No Silent",
                    color = SleekDarkText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "تنبيهات المكالمات الهامة",
                    color = SleekMutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SleekPrimary),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.no_silent_logo_1779708161705),
                    contentDescription = "No Silent Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun PermissionsDashboard(
    hasContacts: Boolean,
    hasCallState: Boolean,
    hasCallLog: Boolean,
    hasOverlay: Boolean,
    onRequestSystemPermissions: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    val allGranted = hasContacts && hasCallState && hasCallLog && hasOverlay
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = SleekSurface
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            SleekBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "أذونات الوصول اللازمة",
                color = SleekPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            
            // Sub items matching Design HTML
            PermissionDashboardRow(
                title = "الوصول لجهات الاتصال",
                description = "لاختيار الأرقام المسجلة بهاتفك مباشرة",
                isGranted = hasContacts,
                icon = Icons.Default.Person
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            PermissionDashboardRow(
                title = "الظهور فوق التطبيقات",
                description = "لعرض شاشة التنبيه الفوري وكتم الرنين",
                isGranted = hasOverlay,
                icon = Icons.Default.Phone
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            PermissionDashboardRow(
                title = "مراقبة المكالمة الواردة",
                description = "للتعرف على رقم المتصل أثناء رنين الهاتف",
                isGranted = hasCallState && hasCallLog,
                icon = Icons.Default.Call
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!allGranted) {
                if (!hasContacts || !hasCallState || !hasCallLog) {
                    Button(
                        onClick = onRequestSystemPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White)
                    ) {
                        Text(
                            text = "تفعيل صلاحيات الهاتف",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (!hasOverlay) {
                    Button(
                        onClick = onRequestOverlayPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSecondary, contentColor = SleekPrimary)
                    ) {
                        Text(
                            text = "تفعيل الظهور فوق التطبيقات",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Surface(
                    color = SuccessGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "جميع الصلاحيات مفعّلة بنجاح. التطبيق جاهز تماماً لتلقي المكالمات وتنبيهك بقوة!",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionDashboardRow(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch Status (Left side)
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(22.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) SleekPrimary else SleekBorder),
                contentAlignment = if (isGranted) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            // Info text and icon (Right side)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = title,
                        color = SleekDarkText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = description,
                        color = SleekMutedText,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Right
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SleekTertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SleekPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: WhitelistContact,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    val opacity = if (contact.isEnabled) 1.0f else 0.6f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_item_${contact.id}")
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = opacity)
        ),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.dp,
            SleekBorder.copy(alpha = opacity)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Delete action (Far left)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_contact_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف جهة الاتصال",
                    tint = Color.Red.copy(alpha = 0.7f)
                )
            }
            
            // Toggle Switch
            Switch(
                checked = contact.isEnabled,
                onCheckedChange = { onToggleEnabled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SleekPrimary,
                    uncheckedThumbColor = Color(0xFF938F99),
                    uncheckedTrackColor = SleekBorder
                ),
                modifier = Modifier.testTag("toggle_contact_${contact.id}")
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Name and phone number block (Center/Right)
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = contact.name,
                    color = SleekDarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = contact.phoneNumber,
                        color = SleekMutedText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (contact.isEnabled) SleekPrimary else Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Circle Avatar (Far right)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (contact.isEnabled) SleekPinkAccent else SleekSecondary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (contact.name.isNotEmpty()) contact.name.take(1) else "?",
                    color = if (contact.isEnabled) Color(0xFF31111D) else Color(0xFF21005D),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyWhitelistState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SleekSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = SleekPrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "قائمة جهات الاتصال فارغة",
            color = SleekDarkText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "أضف جهات الاتصال المهمة بالضغط على زر (+) بالأسفل ليقوم الهاتف بتجاوز الصامت ورنين تنبيهي صاخب مكثف فور اتصالهم بك.",
            color = SleekMutedText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun DeveloperRightsCard(context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Upper row: Developer profile & copyright badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copyright badge (Left aligned)
                Surface(
                    color = SleekTertiary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(start = 2.dp)
                ) {
                    Text(
                        text = "حقوق محفوظة 2026",
                        color = SleekPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // Developer name info (Right aligned)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "تطوير وبرمجة",
                            color = SleekMutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = "أمين محمد",
                            color = SleekDarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SleekPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SleekBorder.copy(alpha = 0.5f))
            )

            // Social channels grid (3 columns matching CSS layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Instagram Link
                FooterSocialItem(
                    label = "إنستغرام",
                    handle = "@89oc9",
                    modifier = Modifier.weight(1f)
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/89oc9"))
                    context.startActivity(intent)
                }

                // Telegram Link
                FooterSocialItem(
                    label = "تيليغرام",
                    handle = "@Ameen_al",
                    modifier = Modifier.weight(1f)
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Ameen_alshammary"))
                    context.startActivity(intent)
                }

                // WhatsApp Link
                FooterSocialItem(
                    label = "واتساب",
                    handle = "0775678",
                    modifier = Modifier.weight(1f)
                ) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=9647756786034"))
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun FooterSocialItem(
    label: String,
    handle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SleekBorder.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = SleekMutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = handle,
                color = SleekPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



@Composable
fun PhonebookPickerDialog(
    contacts: List<DeviceContact>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onContactSelected: (DeviceContact) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = remember(searchQuery, contacts) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchQuery)
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "اختر جهة اتصال للبرنامج",
                    color = SleekDarkText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث بالاسم أو الرقم..", color = SleekMutedText, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekDarkText,
                        unfocusedTextColor = SleekDarkText,
                        cursorColor = SleekPrimary
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SleekMutedText)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = SleekPrimary)
                    } else if (filteredContacts.isEmpty()) {
                        Text(
                            text = if (contacts.isEmpty()) "لم يتم العثور على أرقام في هاتفك أو لم تمنح الصلاحية." else "لا توجد نتائج مطابقة لبحثك.",
                            color = SleekMutedText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredContacts) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(1.dp, SleekBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { onContactSelected(contact) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = contact.name,
                                            color = SleekDarkText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = contact.phoneNumber,
                                            color = SleekMutedText,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekBorder, contentColor = SleekDarkText),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إلغاء وتراجع", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, SleekBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "إضافة يدوية للرقم",
                    color = SleekDarkText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم جهة الاتصال", color = SleekMutedText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekDarkText,
                        unfocusedTextColor = SleekDarkText,
                        focusedLabelColor = SleekPrimary,
                        cursorColor = SleekPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("رقم الهاتف", color = SleekMutedText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SleekPrimary,
                        unfocusedBorderColor = SleekBorder,
                        focusedTextColor = SleekDarkText,
                        unfocusedTextColor = SleekDarkText,
                        focusedLabelColor = SleekPrimary,
                        cursorColor = SleekPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekBorder, contentColor = SleekDarkText),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            if (name.isNotBlank() && number.isNotBlank()) {
                                onAdd(name, number)
                            } else {
                                // Simple blank check
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary, contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إضافة وحفظ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Custom Border decorator helper to avoid creating too many redundant imports
@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}
