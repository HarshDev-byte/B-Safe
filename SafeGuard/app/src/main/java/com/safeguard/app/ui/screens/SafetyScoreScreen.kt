package com.safeguard.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.app.core.SafetyScoreManager
import com.safeguard.app.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScoreScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTriggers: () -> Unit
) {
    val safetyScore by viewModel.safetyScore.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Score") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Score Circle
            item {
                ScoreCircle(
                    score = safetyScore.totalScore,
                    grade = safetyScore.grade
                )
            }
            
            // Score Breakdown
            item {
                ScoreBreakdownCard(breakdown = safetyScore.breakdown)
            }
            
            // Recommendations
            if (safetyScore.recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = "Improve Your Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(safetyScore.recommendations) { recommendation ->
                    RecommendationCard(
                        recommendation = recommendation,
                        onClick = {
                            when (recommendation.action) {
                                SafetyScoreManager.RecommendationAction.ADD_CONTACT,
                                SafetyScoreManager.RecommendationAction.ADD_EMAIL_TO_CONTACT -> onNavigateToContacts()
                                SafetyScoreManager.RecommendationAction.ENABLE_TRIGGER,
                                SafetyScoreManager.RecommendationAction.ENABLE_LOCATION_UPDATES -> onNavigateToTriggers()
                                SafetyScoreManager.RecommendationAction.COMPLETE_PROFILE -> onNavigateToProfile()
                                else -> onNavigateToSettings()
                            }
                        }
                    )
                }
            }
            
            // Achievement unlocked message for high scores
            if (safetyScore.totalScore >= 85) {
                item {
                    AchievementCard()
                }
            }
        }
    }
}

@Composable
private fun ScoreCircle(
    score: Int,
    grade: SafetyScoreManager.SafetyGrade
) {
    val animatedScore by animateIntAsState(
        targetValue = score,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "score"
    )
    
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    val scoreColor = Color(grade.color)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Background circle
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 20f, cap = StrokeCap.Round)
                    )
                }
                
                // Progress circle
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 20f, cap = StrokeCap.Round)
                    )
                }
                
                // Score text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$animatedScore",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "out of 100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grade badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = scoreColor.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = grade.emoji,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = grade.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = getScoreMessage(score),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScoreBreakdownCard(breakdown: SafetyScoreManager.ScoreBreakdown) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Score Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            BreakdownItem(
                icon = Icons.Default.People,
                label = "Emergency Contacts",
                score = breakdown.contactsScore,
                maxScore = 30
            )
            BreakdownItem(
                icon = Icons.Default.TouchApp,
                label = "SOS Triggers",
                score = breakdown.triggersScore,
                maxScore = 25
            )
            BreakdownItem(
                icon = Icons.Default.Person,
                label = "Profile Info",
                score = breakdown.profileScore,
                maxScore = 15
            )
            BreakdownItem(
                icon = Icons.Default.Settings,
                label = "Settings & Permissions",
                score = breakdown.settingsScore,
                maxScore = 20
            )
            BreakdownItem(
                icon = Icons.Default.Verified,
                label = "Activity & Testing",
                score = breakdown.activityScore,
                maxScore = 10
            )
        }
    }
}

@Composable
private fun BreakdownItem(
    icon: ImageVector,
    label: String,
    score: Int,
    maxScore: Int
) {
    val progress = score.toFloat() / maxScore
    val color = when {
        progress >= 0.8f -> Color(0xFF4CAF50)
        progress >= 0.5f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$score/$maxScore",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: SafetyScoreManager.Recommendation,
    onClick: () -> Unit
) {
    val priorityColor = when (recommendation.priority) {
        SafetyScoreManager.Priority.HIGH -> MaterialTheme.colorScheme.error
        SafetyScoreManager.Priority.MEDIUM -> Color(0xFFFF9800)
        SafetyScoreManager.Priority.LOW -> MaterialTheme.colorScheme.primary
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = priorityColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Points badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = priorityColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "+${recommendation.pointsToGain}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = priorityColor
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AchievementCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🏆",
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Safety Champion!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "You've achieved excellent protection. Stay safe!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getScoreMessage(score: Int): String {
    return when {
        score >= 85 -> "Excellent! You're well protected. Keep it up!"
        score >= 70 -> "Good job! A few more steps to maximize your safety."
        score >= 50 -> "You're getting there. Complete the recommendations below."
        score >= 30 -> "Your safety setup needs attention. Let's improve it!"
        else -> "Critical! Please complete the setup to stay protected."
    }
}
