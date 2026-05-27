package com.example

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          MainScreen(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var hasPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
  val logs by LogRepository.logs.collectAsState()
  var isServiceActive by remember { mutableStateOf(SettingsManager.isServiceActive(context)) }
  var currentTab by remember { mutableStateOf(Tab.Monitor) }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        hasPermission = isNotificationServiceEnabled(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Menu, contentDescription = "Menu")
        Spacer(modifier = Modifier.width(16.dp))
        Text(
          text = "AutoReply Bot",
          fontSize = 20.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Search, contentDescription = "Search")
        Spacer(modifier = Modifier.width(12.dp))
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center
        ) {
          Text(text = "JD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Main Content Scrollable
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      when (currentTab) {
        Tab.Monitor -> MonitorContent(context, isServiceActive, hasPermission, logs) { isServiceActive = it }
        Tab.Logs -> LogsContent(logs)
        Tab.Filters -> FiltersContent()
      }
    }

    // Bottom Action Area
    Column {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 16.dp)
      ) {
        Button(
          onClick = {
            try {
              val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
              context.startActivity(intent)
            } catch (e: Exception) {
              Toast.makeText(context, "Notification settings unavailable on this device.", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
          shape = CircleShape
        ) {
          Icon(Icons.Outlined.Settings, contentDescription = null)
          Spacer(modifier = Modifier.width(12.dp))
          Text("Open Service Settings", fontWeight = FontWeight.Medium)
        }
      }

      // Bottom Nav
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(80.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .border(0.5.dp, MaterialTheme.colorScheme.outline),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally, 
          modifier = Modifier.weight(1f).clickable { currentTab = Tab.Monitor }.alpha(if (currentTab == Tab.Monitor) 1f else 0.6f)
        ) {
          if (currentTab == Tab.Monitor) {
            Box(
              modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Outlined.PlayArrow, contentDescription = "Monitor", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
          } else {
            Icon(Icons.Outlined.PlayArrow, contentDescription = "Monitor", modifier = Modifier.padding(vertical = 4.dp))
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text("Monitor", fontSize = 11.sp, fontWeight = if (currentTab == Tab.Monitor) FontWeight.Bold else FontWeight.Medium)
        }
        
        Column(
          horizontalAlignment = Alignment.CenterHorizontally, 
          modifier = Modifier.weight(1f).clickable { currentTab = Tab.Logs }.alpha(if (currentTab == Tab.Logs) 1f else 0.6f)
        ) {
          if (currentTab == Tab.Logs) {
            Box(
              modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Outlined.DateRange, contentDescription = "Logs", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
          } else {
            Icon(Icons.Outlined.DateRange, contentDescription = "Logs", modifier = Modifier.padding(vertical = 4.dp))
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text("Logs", fontSize = 11.sp, fontWeight = if (currentTab == Tab.Logs) FontWeight.Bold else FontWeight.Medium)
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally, 
          modifier = Modifier.weight(1f).clickable { currentTab = Tab.Filters }.alpha(if (currentTab == Tab.Filters) 1f else 0.6f)
        ) {
          if (currentTab == Tab.Filters) {
            Box(
              modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Outlined.List, contentDescription = "Filters", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
          } else {
            Icon(Icons.Outlined.List, contentDescription = "Filters", modifier = Modifier.padding(vertical = 4.dp))
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text("Filters", fontSize = 11.sp, fontWeight = if (currentTab == Tab.Filters) FontWeight.Bold else FontWeight.Medium)
        }
      }
    }
  }
}

@Composable
fun MonitorContent(context: Context, isServiceActive: Boolean, hasPermission: Boolean, logs: List<ReplyLog>, onToggleService: (Boolean) -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Status Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)
        .padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column {
          Text(
            text = "SERVICE STATUS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (isServiceActive) "System Active" else "System Inactive",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
        // Visual toggle switch mock
        Box(
          modifier = Modifier
            .width(48.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(if (isServiceActive) MaterialTheme.colorScheme.primary else Color.Gray)
            .clickable {
              val newValue = !isServiceActive
              onToggleService(newValue)
              SettingsManager.setServiceActive(context, newValue)
            }
            .padding(horizontal = 4.dp),
          contentAlignment = if (isServiceActive) Alignment.CenterEnd else Alignment.CenterStart
        ) {
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(Color.White)
          )
        }
      }
      Text(
        text = "Monitoring WhatsApp notifications. The bot will automatically respond to messages using specified rules.",
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        lineHeight = 20.sp
      )
    }

    // Notification Access
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        .clickable {
          if (!hasPermission) {
            try {
              val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
              context.startActivity(intent)
            } catch (e: Exception) {
              Toast.makeText(context, "Notification settings unavailable on this device.", Toast.LENGTH_SHORT).show()
            }
          }
        }
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Outlined.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
          Text(text = "Notification Access", fontSize = 14.sp, fontWeight = FontWeight.Bold)
          Text(
            text = if (hasPermission) "Permission granted and verified" else "Tap to grant permission",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      Icon(
        if (hasPermission) Icons.Filled.CheckCircle else Icons.Filled.Warning,
        contentDescription = null,
        tint = if (hasPermission) Color(0xFF4CAF50) else Color(0xFFF44336)
      )
    }
    
    // Recent Log snippet
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "RECENT ACTIVITY LOG",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Live", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(0.8f)) {
        if (logs.isEmpty()) {
          Text(
            "Waiting for WhatsApp messages...",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        } else {
          logs.take(3).forEach { log ->
            Text(
              buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("[${log.timestamp}] ") }
                append("Captured: \"${log.groupName}\"\n")
                withStyle(style = SpanStyle(color = Color(0xFF4CAF50))) { append("Replied: ") }
                append("\"${log.replyText}\"")
              },
              fontSize = 11.sp,
              lineHeight = 16.sp,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .padding(8.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun LogsContent(logs: List<ReplyLog>) {
  Column(
    modifier = Modifier.fillMaxSize()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "All Activity Logs",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
      )
      if (logs.isNotEmpty()) {
        androidx.compose.material3.TextButton(onClick = { LogRepository.clearLogs() }) {
          Text("Clear All", color = MaterialTheme.colorScheme.error)
        }
      }
    }
    if (logs.isEmpty()) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text("No logs recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f).fillMaxWidth()
      ) {
        items(logs) { log ->
          Text(
            buildAnnotatedString {
              withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("[${log.timestamp}] ") }
              append("Captured: \"${log.groupName}\"\n")
              withStyle(style = SpanStyle(color = Color(0xFF4CAF50))) { append("Replied: ") }
              append("\"${log.replyText}\"")
            },
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
              .padding(12.dp)
          )
        }
      }
    }
  }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FiltersContent() {
  var filterWaOnly by remember { mutableStateOf(true) }
  var allChats by remember { mutableStateOf(true) }
  
  val rules by RuleRepository.rules.collectAsState()
  var showAddDialog by remember { mutableStateOf(false) }

  if (showAddDialog) {
      AddRuleDialog(onDismiss = { showAddDialog = false })
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "Configuration Rules",
      fontSize = 18.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    
    // Rule 1
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        .clickable { filterWaOnly = !filterWaOnly }
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(text = "Filter WhatsApp Only", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(text = "package: com.whatsapp", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
       // Toggle mini
      Box(
        modifier = Modifier
          .width(40.dp)
          .height(20.dp)
          .clip(CircleShape)
          .background(if (filterWaOnly) MaterialTheme.colorScheme.primary else Color.Gray)
          .padding(horizontal = 2.dp),
        contentAlignment = if (filterWaOnly) Alignment.CenterEnd else Alignment.CenterStart
      ) {
        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White))
      }
    }
    
    // Rule 2
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        .clickable { allChats = !allChats }
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(text = "Target Chats", fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(text = if (allChats) "All messages (Group & Personal)" else "Exclude personal chats", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      // Toggle mini
      Box(
        modifier = Modifier
          .width(40.dp)
          .height(20.dp)
          .clip(CircleShape)
          .background(if (allChats) MaterialTheme.colorScheme.primary else Color.Gray)
          .padding(horizontal = 2.dp),
        contentAlignment = if (allChats) Alignment.CenterEnd else Alignment.CenterStart
      ) {
        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.White))
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Reply Rules",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        androidx.compose.material3.TextButton(onClick = { showAddDialog = true }) {
            Text("Add Rule")
        }
    }

    if (rules.isEmpty()) {
        Text("No rules configured. Set up rules to automatically reply to specific messages.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }

    rules.forEach { rule ->
        RuleItemView(rule)
    }
  }
}

@Composable
fun RuleItemView(rule: ReplyRule) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val title = if (rule.matchType == MatchType.WelcomeMessage) "Welcome Message" else "Keyword: ${rule.incomingKeyword}"
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(text = "Match Type: ${rule.matchType.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            val scopeInfo = "${rule.targetScope.label}${if (rule.replyOnlyIfMentioned && rule.targetScope != RuleScope.Private) " (Mention only)" else ""}"
            Text(text = "Scope: $scopeInfo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Reply: ${rule.replyMessage}", fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
        androidx.compose.material3.IconButton(onClick = { RuleRepository.deleteRule(rule.id) }) {
            androidx.compose.material3.Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Rule", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(onDismiss: () -> Unit) {
    var keyword by remember { mutableStateOf("") }
    var replyMessage by remember { mutableStateOf("") }
    var matchType by remember { mutableStateOf(MatchType.ExactMatch) }
    var targetScope by remember { mutableStateOf(RuleScope.Both) }
    var replyOnlyIfMentioned by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var expandedScope by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reply Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                
                // Match Type Dropdown
                Column {
                    androidx.compose.material3.OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                        Text("Type: ${matchType.label}")
                    }
                    if (expanded) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(4.dp)) {
                            MatchType.values().forEach { type ->
                                Text(
                                    text = type.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            matchType = type
                                            expanded = false 
                                        }
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }

                // Scope Dropdown
                Column {
                    androidx.compose.material3.OutlinedButton(onClick = { expandedScope = !expandedScope }, modifier = Modifier.fillMaxWidth()) {
                        Text("Scope: ${targetScope.label}")
                    }
                    if (expandedScope) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).padding(4.dp)) {
                            RuleScope.values().forEach { scope ->
                                Text(
                                    text = scope.label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            targetScope = scope
                                            expandedScope = false 
                                        }
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }
                
                if (targetScope != RuleScope.Private) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Checkbox(
                            checked = replyOnlyIfMentioned,
                            onCheckedChange = { replyOnlyIfMentioned = it }
                        )
                        Text("Only reply if mentioned (@)", fontSize = 14.sp)
                    }
                }

                if (matchType != MatchType.WelcomeMessage) {
                    androidx.compose.material3.OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Incoming Keyword/Pattern") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                androidx.compose.material3.OutlinedTextField(
                    value = replyMessage,
                    onValueChange = { replyMessage = it },
                    label = { Text("Reply Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                RuleRepository.addRule(
                    ReplyRule(
                        incomingKeyword = keyword,
                        matchType = matchType,
                        replyMessage = replyMessage,
                        targetScope = targetScope,
                        replyOnlyIfMentioned = replyOnlyIfMentioned
                    )
                )
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Mengecek apakah aplikasi ini sudah diberikan hak akses Notification Listener
fun isNotificationServiceEnabled(context: Context): Boolean {
  val pkgName = context.packageName
  val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
  if (!TextUtils.isEmpty(flat)) {
    val names = flat.split(":")
    for (name in names) {
      val cn = ComponentName.unflattenFromString(name)
      if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
        return true
      }
    }
  }
  return false
}

enum class Tab {
  Monitor, Logs, Filters
}
