package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import eu.kanade.tachiyomi.ui.manga.notes.MangaNotesScreenState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private const val MAX_LENGTH = 10_000

@Composable
fun MangaNotesTextArea(
    state: MangaNotesScreenState.Success,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val richTextState = rememberRichTextState()
    richTextState.config.linkColor = MaterialTheme.colorScheme.primary
    richTextState.config.listIndent = 15
    val focusRequester = remember { FocusRequester() }

    val largeFontSize = MaterialTheme.typography.headlineMedium.fontSize

    Column(
        modifier = modifier
            .fillMaxSize(),
    ) {
        RichTextEditor(
            state = richTextState,
            textStyle = MaterialTheme.typography.bodyMedium,
            maxLength = MAX_LENGTH,
            placeholder = {
                Text(text = stringResource(MR.strings.notes_placeholder))
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )
        AnimatedVisibility(
            visible = WindowInsets.isImeVisible,
        ) {
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(top = 4.dp),
            ) {
                item {
                    MangaNotesTextAreaButton(
                        onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                        isSelected = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
                        icon = Icons.Outlined.FormatBold,
                    )
                }
                item {
                    MangaNotesTextAreaButton(
                        onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                        isSelected = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
                        icon = Icons.Outlined.FormatItalic,
                    )
                }
                item {
                    MangaNotesTextAreaButton(
                        onClick = {
                            richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                        },
                        isSelected =
                        richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true,
                        icon = Icons.Outlined.FormatUnderlined,
                    )
                }
                item {
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp),
                    )
                }
                item {
                    MangaNotesTextAreaButton(
                        onClick = { richTextState.toggleUnorderedList() },
                        isSelected = richTextState.isUnorderedList,
                        icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    )
                }
                item {
                    MangaNotesTextAreaButton(
                        onClick = { richTextState.toggleOrderedList() },
                        isSelected = richTextState.isOrderedList,
                        icon = Icons.Outlined.FormatListNumbered,
                    )
                }
                item {
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp),
                    )
                }
                item {
                    MangaNotesTextAreaButton(
                        onClick = { richTextState.toggleSpanStyle(SpanStyle(fontSize = largeFontSize)) },
                        isSelected = richTextState.currentSpanStyle.fontSize == largeFontSize,
                        icon = Icons.Outlined.FormatSize,
                    )
                }
            }
        }
    }

    LaunchedEffect(focusRequester) {
        state.notes?.let { richTextState.setHtml(it) }
        focusRequester.requestFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            onSave(richTextState.toHtml())
        }
    }
}

@Composable
fun MangaNotesTextAreaButton(
    onClick: () -> Unit,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                onClick = onClick,
                enabled = true,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = icon.name,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .background(color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                .padding(6.dp),
        )
    }
}
