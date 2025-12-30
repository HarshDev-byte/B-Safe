package com.safeguard.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Search suggestion item
 */
data class SearchSuggestion(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: SuggestionType = SuggestionType.PLACE
)

enum class SuggestionType {
    PLACE,
    RECENT,
    SAVED,
    CONTACT
}

/**
 * Google Maps-style search bar for the map
 */
@Composable
fun MapSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search here",
    suggestions: List<SearchSuggestion> = emptyList(),
    onSuggestionClick: (SearchSuggestion) -> Unit = {},
    onVoiceSearchClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    showSuggestions: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(modifier = modifier) {
        // Search bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(if (isFocused) 8.dp else 4.dp, RoundedCornerShape(28.dp))
                .semantics { contentDescription = "Search for places" },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back/Menu button
                IconButton(
                    onClick = {
                        if (isFocused || query.isNotEmpty()) {
                            onQueryChange("")
                            focusManager.clearFocus()
                        } else {
                            onBackClick?.invoke()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFocused || query.isNotEmpty()) 
                            Icons.Default.ArrowBack 
                        else 
                            Icons.Default.Search,
                        contentDescription = if (isFocused) "Clear search" else "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Text field
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch(query)
                            focusManager.clearFocus()
                        }
                    )
                )

                // Clear button (when there's text)
                AnimatedVisibility(
                    visible = query.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Voice search button
                IconButton(onClick = onVoiceSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Suggestions dropdown
        AnimatedVisibility(
            visible = showSuggestions && suggestions.isNotEmpty() && isFocused,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(suggestions) { suggestion ->
                        SearchSuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                onSuggestionClick(suggestion)
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionItem(
    suggestion: SearchSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on type
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when (suggestion.type) {
                        SuggestionType.RECENT -> MaterialTheme.colorScheme.surfaceVariant
                        SuggestionType.SAVED -> MaterialTheme.colorScheme.primaryContainer
                        SuggestionType.CONTACT -> MaterialTheme.colorScheme.tertiaryContainer
                        SuggestionType.PLACE -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (suggestion.type) {
                    SuggestionType.RECENT -> Icons.Default.History
                    SuggestionType.SAVED -> Icons.Default.Bookmark
                    SuggestionType.CONTACT -> Icons.Default.Person
                    SuggestionType.PLACE -> Icons.Default.Place
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = when (suggestion.type) {
                    SuggestionType.RECENT -> MaterialTheme.colorScheme.onSurfaceVariant
                    SuggestionType.SAVED -> MaterialTheme.colorScheme.primary
                    SuggestionType.CONTACT -> MaterialTheme.colorScheme.tertiary
                    SuggestionType.PLACE -> MaterialTheme.colorScheme.secondary
                }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = suggestion.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Default.NorthWest,
            contentDescription = "Use suggestion",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Compact search bar for when map is in focus
 */
@Composable
fun CompactMapSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search here"
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice search",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
