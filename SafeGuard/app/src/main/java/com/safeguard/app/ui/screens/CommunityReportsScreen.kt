package com.safeguard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.app.core.CrowdSourcedSafetyManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityReportsScreen(
    reports: List<CrowdSourcedSafetyManager.SafetyReport>,
    areaSafetyScore: CrowdSourcedSafetyManager.AreaSafetyScore?,
    onSubmitReport: (CrowdSourcedSafetyManager.ReportType, CrowdSourcedSafetyManager.Severity, String) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<CrowdSourcedSafetyManager.ReportType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Community Safety")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("👥", fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showReportDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Report") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Area Safety Score
            areaSafetyScore?.let { score ->
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AreaSafetyCard(score)
                }
            }

            // Filter Chips
            item {
                Text(
                    text = "Filter by Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All") }
                        )
                    }
                    items(CrowdSourcedSafetyManager.ReportType.values().take(6)) { type ->
                        FilterChip(
                            selected = selectedFilter == type,
                            onClick = { selectedFilter = if (selectedFilter == type) null else type },
                            label = { Text("${type.emoji} ${type.label.split("/")[0]}") }
                        )
                    }
                }
            }

            // Reports Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${reports.size} nearby",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reports List
            val filteredReports = if (selectedFilter != null) {
                reports.filter { it.type == selectedFilter }
            } else reports

            if (filteredReports.isEmpty()) {
                item {
                    EmptyReportsCard()
                }
            } else {
                items(filteredReports) { report ->
                    ReportCard(
                        report = report,
                        onUpvote = { onUpvote(report.id) },
                        onDownvote = { onDownvote(report.id) }
                    )
                }
            }

            // Info Card
            item {
                CommunityInfoCard()
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Report Dialog
    if (showReportDialog) {
        SubmitReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { type, severity, description ->
                onSubmitReport(type, severity, description)
                showReportDialog = false
            }
        )
    }
}

@Composable
private fun AreaSafetyCard(score: CrowdSourcedSafetyManager.AreaSafetyScore) {
    val levelColor = Color(score.level.color)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = levelColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Area Safety Score",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${score.recentIncidents} incidents nearby",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${score.score}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = levelColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = score.level.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = levelColor
                        )
                    }
                }
            }

            if (score.topConcerns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Top Concerns:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    score.topConcerns.forEach { concern ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${concern.emoji} ${concern.label.split("/")[0]}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = score.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportCard(
    report: CrowdSourcedSafetyManager.SafetyReport,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit
) {
    val severityColor = Color(report.severity.color)
    val timeAgo = getTimeAgo(report.timestamp)

    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(report.type.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = report.type.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timeAgo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Severity Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = severityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = report.severity.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vote buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (report.isVerified) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = "Verified",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Upvote
                    IconButton(onClick = onUpvote, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ThumbUp,
                            contentDescription = "Helpful",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    Text(
                        text = "${report.upvotes}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Downvote
                    IconButton(onClick = onDownvote, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.ThumbDown,
                            contentDescription = "Not accurate",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFF44336)
                        )
                    }
                    Text(
                        text = "${report.downvotes}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyReportsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎉", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Reports Nearby",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This area looks safe! Be the first to report if you see something.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommunityInfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Community Powered Safety",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Reports from users like you help keep everyone safe. Share what you see to help others.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitReportDialog(
    onDismiss: () -> Unit,
    onSubmit: (CrowdSourcedSafetyManager.ReportType, CrowdSourcedSafetyManager.Severity, String) -> Unit
) {
    var selectedType by remember { mutableStateOf<CrowdSourcedSafetyManager.ReportType?>(null) }
    var selectedSeverity by remember { mutableStateOf(CrowdSourcedSafetyManager.Severity.MEDIUM) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Safety Concern") },
        text = {
            Column {
                Text(
                    text = "What did you observe?",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Report Type Grid
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CrowdSourcedSafetyManager.ReportType.values().toList()) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { 
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(type.emoji, fontSize = 20.sp)
                                    Text(type.label.split("/")[0], fontSize = 10.sp)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Severity",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrowdSourcedSafetyManager.Severity.values().forEach { severity ->
                        FilterChip(
                            selected = selectedSeverity == severity,
                            onClick = { selectedSeverity = severity },
                            label = { Text(severity.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(severity.color).copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("What did you see? Be specific to help others.") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedType?.let { type ->
                        onSubmit(type, selectedSeverity, description)
                    }
                },
                enabled = selectedType != null && description.isNotBlank()
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = minutes / 60
    
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
