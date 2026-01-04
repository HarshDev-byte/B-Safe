package com.safeguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.app.ai.SmartSafetyAssistant
import com.safeguard.app.ai.ThreatDetectionAI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInsightsScreen(
    insights: List<SmartSafetyAssistant.SafetyInsight>,
    prediction: SmartSafetyAssistant.DailyPrediction?,
    threatAssessment: ThreatDetectionAI.ThreatAssessment?,
    isAIMonitoring: Boolean,
    onToggleAIMonitoring: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onInsightAction: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Safety Assistant")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✨", fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // AI Monitoring Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isAIMonitoring) "AI ON" else "AI OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAIMonitoring) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isAIMonitoring,
                            onCheckedChange = onToggleAIMonitoring,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Real-time Threat Assessment
            if (threatAssessment != null && isAIMonitoring) {
                item {
                    ThreatAssessmentCard(threatAssessment)
                }
            }

            // Daily Prediction
            if (prediction != null) {
                item {
                    DailyPredictionCard(prediction)
                }
            }

            // AI Status Card
            item {
                AIStatusCard(isAIMonitoring)
            }

            // Insights Header
            if (insights.isNotEmpty()) {
                item {
                    Text(
                        text = "Personalized Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(insights) { insight ->
                    InsightCard(
                        insight = insight,
                        onAction = { insight.actionRoute?.let { onInsightAction(it) } }
                    )
                }
            }

            // AI Features Info
            item {
                AIFeaturesCard()
            }
        }
    }
}

@Composable
private fun ThreatAssessmentCard(assessment: ThreatDetectionAI.ThreatAssessment) {
    val riskColor = Color(assessment.overallRisk.color)
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (assessment.overallRisk == ThreatDetectionAI.RiskLevel.CRITICAL)
                riskColor.copy(alpha = alpha * 0.2f)
            else
                riskColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(riskColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-time Threat Level",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = riskColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = assessment.overallRisk.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Risk Score Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Risk Score", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "${assessment.riskScore}/100",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = assessment.riskScore / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = riskColor,
                    trackColor = riskColor.copy(alpha = 0.2f)
                )
            }

            if (assessment.detectedThreats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Detected:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                assessment.detectedThreats.take(3).forEach { threat ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("• ", color = riskColor)
                        Text(
                            text = threat.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = assessment.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DailyPredictionCard(prediction: SmartSafetyAssistant.DailyPrediction) {
    val riskColor = when (prediction.riskLevel) {
        "High" -> Color(0xFFF44336)
        "Moderate" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔮", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Today's Safety Forecast",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${prediction.dayOfWeek} ${prediction.timeOfDay}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Risk Level Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${prediction.riskScore}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                    Text(
                        text = "Risk Score",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = riskColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = prediction.riskLevel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = riskColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Risk Level",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (prediction.factors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Contributing Factors:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                prediction.factors.forEach { factor ->
                    Text(
                        text = "• $factor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = prediction.recommendation,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AIStatusCard(isMonitoring: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isMonitoring) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isMonitoring)
                            Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))
                        else
                            Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMonitoring) "AI Protection Active" else "AI Protection Off",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isMonitoring) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isMonitoring) 
                        "Monitoring for threats in real-time" 
                    else 
                        "Enable to detect potential dangers automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: SmartSafetyAssistant.SafetyInsight,
    onAction: () -> Unit
) {
    val priorityColor = when (insight.priority) {
        SmartSafetyAssistant.Priority.URGENT -> Color(0xFFF44336)
        SmartSafetyAssistant.Priority.HIGH -> Color(0xFFFF9800)
        SmartSafetyAssistant.Priority.MEDIUM -> Color(0xFFFFC107)
        SmartSafetyAssistant.Priority.LOW -> Color(0xFF4CAF50)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = priorityColor.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(insight.icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = insight.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = priorityColor
                    )
                }
                
                // Priority indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (insight.actionLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = priorityColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(insight.actionLabel)
                }
            }
        }
    }
}

@Composable
private fun AIFeaturesCard() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🧠 AI-Powered Features",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val features = listOf(
                "🎯" to "Threat Detection - Detects falls, sudden stops, device snatching",
                "🔮" to "Safety Predictions - Daily risk forecasts based on patterns",
                "🗣️" to "Voice Commands - Say 'Help' in 10+ languages to trigger SOS",
                "📍" to "Route Analysis - Warns about dangerous areas on your path",
                "⚡" to "Auto-Alert - Automatically notifies contacts in critical situations"
            )
            
            features.forEach { (emoji, description) ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(emoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
