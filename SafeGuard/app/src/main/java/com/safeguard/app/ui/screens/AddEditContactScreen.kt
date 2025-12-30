package com.safeguard.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.safeguard.app.data.models.EmergencyContact
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    viewModel: MainViewModel,
    contactId: Long?,
    onNavigateBack: () -> Unit
) {
    val contacts by viewModel.emergencyContacts.collectAsState()
    val existingContact = contacts.find { it.id == contactId }
    val isEditing = existingContact != null

    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(existingContact?.phoneNumber ?: "") }
    var email by remember { mutableStateOf(existingContact?.email ?: "") }
    var relationship by remember { mutableStateOf(existingContact?.relationship ?: "") }
    var isPrimary by remember { mutableStateOf(existingContact?.isPrimary ?: false) }
    var enableSMS by remember { mutableStateOf(existingContact?.enableSMS ?: true) }
    var enableEmail by remember { mutableStateOf(existingContact?.enableEmail ?: true) }
    var enableCall by remember { mutableStateOf(existingContact?.enableCall ?: false) }
    var enableLiveLocation by remember { mutableStateOf(existingContact?.enableLiveLocation ?: false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Contact" else "Add Contact") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val contact = EmergencyContact(
                                    id = existingContact?.id ?: 0,
                                    name = name,
                                    phoneNumber = phoneNumber,
                                    email = email,
                                    relationship = relationship,
                                    isPrimary = isPrimary,
                                    enableSMS = enableSMS,
                                    enableEmail = enableEmail,
                                    enableCall = enableCall,
                                    enableLiveLocation = enableLiveLocation
                                )
                                if (isEditing) {
                                    viewModel.updateContact(contact)
                                } else {
                                    viewModel.addContact(contact)
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = name.isNotBlank() && (phoneNumber.isNotBlank() || email.isNotBlank())
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Info card about SIM-less alerts
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Works without SIM card!",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Add email to send alerts via WiFi/Internet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                supportingText = { Text("For SMS alerts (requires SIM card)") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                supportingText = { Text("For email alerts (works without SIM!)") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = relationship,
                onValueChange = { relationship = it },
                label = { Text("Relationship (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                supportingText = { Text("e.g., Parent, Spouse, Friend") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Alert Methods",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                "Choose how this contact will be notified",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Column(modifier = Modifier.padding(8.dp)) {
                    // SMS Alert
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SMS Alerts")
                            Text(
                                "Requires SIM card",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableSMS,
                            onCheckedChange = { enableSMS = it },
                            enabled = phoneNumber.isNotBlank()
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Email Alert
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Email Alerts")
                            Text(
                                "Works via WiFi - No SIM needed! ✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(
                            checked = enableEmail,
                            onCheckedChange = { enableEmail = it },
                            enabled = email.isNotBlank()
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Auto Call
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Call")
                            Text(
                                "Automatically call during SOS",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableCall,
                            onCheckedChange = { enableCall = it },
                            enabled = phoneNumber.isNotBlank()
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Live Location
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Live Location")
                            Text(
                                "Share continuous location updates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableLiveLocation,
                            onCheckedChange = { enableLiveLocation = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Contact
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star, 
                        contentDescription = null,
                        tint = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Primary Contact")
                        Text(
                            "First contact to be notified and called",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Help text
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Tip: Add both phone and email for maximum reliability. " +
                        "Email alerts work even without a SIM card - just need WiFi or mobile data!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
