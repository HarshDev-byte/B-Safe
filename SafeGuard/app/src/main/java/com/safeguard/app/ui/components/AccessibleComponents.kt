package com.safeguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeguard.app.core.SOSManager

/**
 * Accessible SOS Button with enhanced feedback for users with disabilities
 * - Large touch target (minimum 48dp, recommended 72dp+)
 * - High contrast colors
 * - Haptic feedback
 * - Screen reader support
 * - Visual feedback for all states
 */
@Composable
fun AccessibleSOSButton(
    sosState: SOSManager.SOSState,
    onTriggerSOS: () -> Unit,
    onCancelSOS: () -> Unit,
    onCancelCountdown: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    enableHaptics: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val isActive = sosState is SOSManager.SOSState.Active
    val isCountdown = sosState is SOSManager.SOSState.Countdown

    // Pulsing animation for active/countdown states
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive || isCountdown) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isActive) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Color transitions
    val buttonColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFFD32F2F)
            isCountdown -> Color(0xFFFF6D00)
            isPressed -> Color(0xFFB71C1C)
            else -> Color(0xFFE53935)
        },
        animationSpec = tween(300),
        label = "button_color"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "press_scale"
    )

    // Accessibility description
    val contentDescription = when (sosState) {
        is SOSManager.SOSState.Idle -> "SOS Button. Double tap to send emergency alert to your contacts."
        is SOSManager.SOSState.Countdown -> "SOS activating in ${sosState.secondsRemaining} seconds. Tap to cancel."
        is SOSManager.SOSState.Active -> "SOS is active. Emergency contacts are being notified. Tap to cancel."
        is SOSManager.SOSState.Cancelling -> "Cancelling SOS alert."
    }

    Box(
        modifier = modifier
            .size(size + 40.dp) // Extra space for glow effect
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
                if (isActive) {
                    this.liveRegion = LiveRegionMode.Assertive
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer glow ring for active state
        if (isActive || isCountdown) {
            Box(
                modifier = Modifier
                    .size(size + 30.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                buttonColor.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main button
        Box(
            modifier = Modifier
                .size(size)
                .scale(pulseScale * pressScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            buttonColor,
                            buttonColor.copy(alpha = 0.8f)
                        )
                    )
                )
                .border(
                    width = 4.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (enableHaptics) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        when (sosState) {
                            is SOSManager.SOSState.Idle -> onTriggerSOS()
                            is SOSManager.SOSState.Countdown -> onCancelCountdown()
                            is SOSManager.SOSState.Active -> onCancelSOS()
                            else -> {}
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (sosState) {
                    is SOSManager.SOSState.Countdown -> {
                        Text(
                            text = "${sosState.secondsRemaining}",
                            color = Color.White,
                            fontSize = (size.value / 3).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "TAP TO CANCEL",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    is SOSManager.SOSState.Active -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size((size.value / 4).dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ACTIVE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap to stop",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }
                    else -> {
                        Text(
                            text = "SOS",
                            color = Color.White,
                            fontSize = (size.value / 4).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap for help",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * High contrast status card for visibility
 */
@Composable
fun AccessibleStatusCard(
    isProtected: Boolean,
    contactCount: Int,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isProtected) {
        Color(0xFF1B5E20) // Dark green - high contrast
    } else {
        Color(0xFFB71C1C) // Dark red - high contrast
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isProtected) {
                    "Protection active. $contactCount emergency contacts configured."
                } else {
                    "Warning: Protection not fully configured. Please add emergency contacts."
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isProtected) Icons.Default.Shield else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (isProtected) "Protection Active" else "Setup Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (contactCount > 0) {
                        "$contactCount emergency contact${if (contactCount > 1) "s" else ""}"
                    } else {
                        "Add contacts to enable protection"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

/**
 * Large, accessible quick action button
 */
@Composable
fun AccessibleQuickAction(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier
            .semantics {
                contentDescription = "$label. $description"
                role = Role.Button
                if (!enabled) {
                    disabled()
                }
            }
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Voice activation indicator with visual feedback
 */
@Composable
fun VoiceActivationIndicator(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.5f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale1"
    )
    
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale2"
    )

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isListening) 0f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .semantics {
                contentDescription = if (isListening) {
                    "Voice activation listening. Say 'Help me' or 'Emergency' to trigger SOS."
                } else {
                    "Voice activation inactive."
                }
                if (isListening) {
                    liveRegion = LiveRegionMode.Polite
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            // Ripple effects
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(scale2)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1 * 0.5f))
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(scale1)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1))
            )
        }
        
        // Microphone icon
        Surface(
            shape = CircleShape,
            color = if (isListening) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = null,
                    tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Safety score visualization
 */
@Composable
fun SafetyScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        score >= 80 -> Color(0xFF2E7D32) // Green
        score >= 60 -> Color(0xFFF9A825) // Yellow
        score >= 40 -> Color(0xFFFF6F00) // Orange
        else -> Color(0xFFD32F2F) // Red
    }

    val label = when {
        score >= 80 -> "Excellent"
        score >= 60 -> "Good"
        score >= 40 -> "Fair"
        else -> "Needs Attention"
    }

    Card(
        modifier = modifier.semantics {
            contentDescription = "Safety score: $score out of 100. Status: $label"
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Safety Score",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f))
                    .border(8.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
