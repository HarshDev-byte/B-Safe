package com.safeguard.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeguard.app.data.models.EmergencyContact
import com.safeguard.app.data.models.RegionalSettings
import com.safeguard.app.ui.viewmodels.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val settings by viewModel.userSettings.collectAsState()
    val regionalSettings by viewModel.regionalSettings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        val progress = (pagerState.currentPage + 1) / 5f
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> EmergencyContactPage(viewModel)
                2 -> TriggerSetupPage(viewModel, settings)
                3 -> RegionalSettingsPage(viewModel, regionalSettings)
                4 -> PersonalInfoPage(viewModel, settings)
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text("Back")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 4) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete()
                    }
                }
            ) {
                Text(if (pagerState.currentPage < 4) "Next" else "Get Started")
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to B-Safe",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your personal safety companion that works even when you can't interact with your phone normally.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        FeatureItem(Icons.Default.Sms, "Instant SMS alerts to trusted contacts")
        FeatureItem(Icons.Default.LocationOn, "Share your location automatically")
        FeatureItem(Icons.Default.VolumeUp, "Trigger SOS with button sequences")
        FeatureItem(Icons.Default.WifiOff, "Works offline - no internet needed")
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmergencyContactPage(viewModel: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    val contacts by viewModel.emergencyContacts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Add Emergency Contacts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "These people will be notified when you trigger an SOS alert.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = relationship,
            onValueChange = { relationship = it },
            label = { Text("Relationship (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && phone.isNotBlank()) {
                    viewModel.addContact(
                        EmergencyContact(
                            name = name,
                            phoneNumber = phone,
                            relationship = relationship,
                            isPrimary = contacts.isEmpty()
                        )
                    )
                    name = ""
                    phone = ""
                    relationship = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && phone.isNotBlank()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (contacts.isNotEmpty()) {
            Text(
                text = "Added Contacts (${contacts.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            contacts.forEach { contact ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(contact.name, fontWeight = FontWeight.Medium)
                            Text(
                                contact.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (contact.isPrimary) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Primary") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TriggerSetupPage(
    viewModel: MainViewModel,
    settings: com.safeguard.app.data.models.UserSettings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "SOS Trigger Methods",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose how you want to trigger emergency alerts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        TriggerOption(
            icon = Icons.Default.VolumeUp,
            title = "Volume Button Sequence",
            description = "Press volume buttons in a pattern (e.g., Up-Up-Down-Down)",
            enabled = settings.enableVolumeButtonTrigger,
            onToggle = {
                viewModel.updateUserSettings(settings.copy(enableVolumeButtonTrigger = it))
            }
        )

        TriggerOption(
            icon = Icons.Default.Vibration,
            title = "Shake Detection",
            description = "Shake your phone vigorously to trigger SOS",
            enabled = settings.enableShakeTrigger,
            onToggle = {
                viewModel.updateUserSettings(settings.copy(enableShakeTrigger = it))
            }
        )

        TriggerOption(
            icon = Icons.Default.Power,
            title = "Power Button Pattern",
            description = "Press power button multiple times quickly",
            enabled = settings.enablePowerButtonTrigger,
            onToggle = {
                viewModel.updateUserSettings(settings.copy(enablePowerButtonTrigger = it))
            }
        )

        TriggerOption(
            icon = Icons.Default.Widgets,
            title = "Widget & Lock Screen",
            description = "Quick access from home screen and lock screen",
            enabled = settings.enableWidgetTrigger,
            onToggle = {
                viewModel.updateUserSettings(settings.copy(enableWidgetTrigger = it))
            }
        )
    }
}

@Composable
private fun TriggerOption(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun RegionalSettingsPage(
    viewModel: MainViewModel,
    settings: RegionalSettings
) {
    var emergencyNumber by remember { mutableStateOf(settings.emergencyNumber) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Regional Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Configure emergency numbers for your region.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = emergencyNumber,
            onValueChange = { 
                emergencyNumber = it
                viewModel.updateRegionalSettings(settings.copy(emergencyNumber = it))
            },
            label = { Text("Emergency Number") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            supportingText = { Text("e.g., 911 (US), 999 (UK), 112 (EU)") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Common Emergency Numbers",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("🇺🇸 USA/Canada: 911", style = MaterialTheme.typography.bodySmall)
                Text("🇬🇧 UK: 999", style = MaterialTheme.typography.bodySmall)
                Text("🇪🇺 Europe: 112", style = MaterialTheme.typography.bodySmall)
                Text("🇮🇳 India: 112 / 100 (Police)", style = MaterialTheme.typography.bodySmall)
                Text("🇦🇺 Australia: 000", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PersonalInfoPage(
    viewModel: MainViewModel,
    settings: com.safeguard.app.data.models.UserSettings
) {
    var userName by remember { mutableStateOf(settings.userName) }
    var bloodGroup by remember { mutableStateOf(settings.bloodGroup) }
    var medicalNotes by remember { mutableStateOf(settings.medicalNotes) }
    var includeInSOS by remember { mutableStateOf(settings.includePersonalInfoInSOS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Personal Safety Info",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Optional information that can be included in emergency messages.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = { 
                userName = it
                viewModel.updateUserSettings(settings.copy(userName = it))
            },
            label = { Text("Your Name (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bloodGroup,
            onValueChange = { 
                bloodGroup = it
                viewModel.updateUserSettings(settings.copy(bloodGroup = it))
            },
            label = { Text("Blood Group (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("e.g., A+, B-, O+, AB+") }
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = medicalNotes,
            onValueChange = { 
                medicalNotes = it
                viewModel.updateUserSettings(settings.copy(medicalNotes = it))
            },
            label = { Text("Medical Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            supportingText = { Text("Allergies, conditions, medications, etc.") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Include in SOS messages", fontWeight = FontWeight.Medium)
                Text(
                    "Share this info with emergency contacts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = includeInSOS,
                onCheckedChange = { 
                    includeInSOS = it
                    viewModel.updateUserSettings(settings.copy(includePersonalInfoInSOS = it))
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Your data is stored locally on your device and only shared during emergencies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}
