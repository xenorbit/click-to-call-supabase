package com.example.c2c.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.c2c.data.SupabaseRepository
import com.example.c2c.data.PreferencesManager
import com.example.c2c.ui.theme.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    val supabaseRepository = remember { SupabaseRepository() }
    
    // State
    var isPaired by remember { mutableStateOf(false) }
    var pairingCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var permissionsGranted by remember { mutableStateOf(false) }
    
    // Permission launcher for multiple permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            Toast.makeText(context, "Permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Some permissions denied. App may not work properly.", Toast.LENGTH_LONG).show()
        }
    }
    
    // Check and request permissions on first launch
    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        
        // Phone permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            permissionsGranted = true
        }
    }
    
    // Load saved pairing code on start
    LaunchedEffect(Unit) {
        preferencesManager.pairingCode.collect { code ->
            if (code.isNotEmpty()) {
                pairingCode = code
            }
        }
    }
    
    // Check paired status from local storage
    LaunchedEffect(Unit) {
        preferencesManager.isPaired.collect { paired ->
            isPaired = paired
        }
    }
    
    // Function to check pairing status from server
    fun checkPairingStatus() {
        if (pairingCode.isEmpty()) return
        
        scope.launch {
            isRefreshing = true
            try {
                val result = supabaseRepository.checkPairingStatus(pairingCode)
                result.fold(
                    onSuccess = { serverPaired ->
                        isPaired = serverPaired
                        preferencesManager.setIsPaired(serverPaired)
                        if (serverPaired) {
                            Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Not paired yet", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFailure = { error ->
                        Log.e("HomeScreen", "Failed to check status", error)
                        Toast.makeText(context, "Failed to check status", Toast.LENGTH_SHORT).show()
                    }
                )
            } finally {
                isRefreshing = false
            }
        }
    }
    
    // Function to generate and register pairing code
    fun generateAndRegisterCode() {
        scope.launch {
            isLoading = true
            statusMessage = ""
            
            try {
                // Generate new code
                val newCode = preferencesManager.generatePairingCode()
                pairingCode = newCode
                
                // Get FCM token
                val fcmToken = try {
                    FirebaseMessaging.getInstance().token.await()
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Failed to get FCM token", e)
                    null
                }
                
                if (fcmToken == null) {
                    statusMessage = "Failed to get device token. Please try again."
                    Toast.makeText(context, "Failed to get device token", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@launch
                }
                
                Log.d("HomeScreen", "FCM Token: $fcmToken")
                Log.d("HomeScreen", "Pairing Code: $newCode")
                
                // Register with Supabase
                val result = supabaseRepository.registerDevice(
                    pairingCode = newCode,
                    fcmToken = fcmToken,
                    deviceName = android.os.Build.MODEL
                )
                
                result.fold(
                    onSuccess = { deviceId ->
                        // Save pairing data
                        preferencesManager.setPairingCode(newCode)
                        preferencesManager.setFcmToken(fcmToken)
                        preferencesManager.setDeviceId(deviceId)
                        statusMessage = "Ready! Enter code in Chrome extension."
                        Toast.makeText(context, "Device registered successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        statusMessage = "Registration failed: ${error.message}"
                        Toast.makeText(context, "Registration failed. Check internet connection.", Toast.LENGTH_LONG).show()
                        Log.e("HomeScreen", "Registration failed", error)
                    }
                )
                
            } catch (e: Exception) {
                statusMessage = "Error: ${e.message}"
                Log.e("HomeScreen", "Error generating code", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Click-to-Call",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = OnBackground
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Card with refresh button
            StatusCard(
                isPaired = isPaired,
                isRefreshing = isRefreshing,
                onRefresh = { checkPairingStatus() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (!isPaired) {
                // Pairing Code Display
                PairingCodeCard(
                    code = pairingCode.ifEmpty { "------" },
                    isLoading = isLoading,
                    statusMessage = statusMessage,
                    onGenerateCode = { generateAndRegisterCode() }
                )
            } else {
                // Connected State
                ConnectedCard()
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Instructions
            InstructionsSection()
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatusCard(
    isPaired: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPaired) Success.copy(alpha = 0.2f)
                        else Error.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaired) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isPaired) Success else Error,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPaired) "Connected" else "Not Connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface
                )
                Text(
                    text = if (isPaired) "Ready to receive calls" else "Pair with Chrome extension",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant
                )
            }
            
            // Refresh button - green when connected
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isPaired) Success.copy(alpha = 0.15f) else Primary.copy(alpha = 0.15f))
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = if (isPaired) Success else Primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh status",
                        tint = if (isPaired) Success else Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PairingCodeCard(
    code: String,
    isLoading: Boolean,
    statusMessage: String,
    onGenerateCode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pairing Code",
                style = MaterialTheme.typography.titleMedium,
                color = OnSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Large code display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariant)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Primary
                    )
                } else {
                    Text(
                        text = code.chunked(3).joinToString(" "),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        letterSpacing = 4.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status message
            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (statusMessage.contains("failed", ignoreCase = true) || 
                               statusMessage.contains("error", ignoreCase = true)) Error 
                            else Success,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = "Enter this code in the Chrome extension",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onGenerateCode,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Registering...",
                        style = MaterialTheme.typography.labelLarge
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (code == "------") "Generate Code" else "Regenerate Code",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Ready to Receive Calls",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select a phone number in Chrome and\nright-click to dial from this device",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InstructionsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InstructionStep(number = "1", text = "Click 'Generate Code' to get a pairing code")
            InstructionStep(number = "2", text = "Enter the code in the Chrome extension")
            InstructionStep(number = "3", text = "Right-click any phone number in Chrome")
            InstructionStep(number = "4", text = "Tap the notification to dial")
        }
    }
}

@Composable
fun InstructionStep(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}
