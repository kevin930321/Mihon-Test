package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.WheelTextPicker
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

@Composable
fun DeleteChaptersDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        title = {
            Text(text = stringResource(MR.strings.are_you_sure))
        },
        text = {
            Text(text = stringResource(MR.strings.confirm_delete_chapters))
        },
    )
}

@Composable
fun SetIntervalDialog(
    interval: Int,
    nextUpdate: Instant?,
    onDismissRequest: () -> Unit,
    onValueChanged: ((Int) -> Unit)? = null,
) {
    var selectedInterval by rememberSaveable { mutableIntStateOf(if (interval < 0) -interval else 0) }

    val nextUpdateDays = remember(nextUpdate) {
        return@remember if (nextUpdate != null) {
            val now = Instant.now()
            now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.pref_library_update_smart_update)) },
        text = {
            Column {
                if (nextUpdateDays != null && nextUpdateDays >= 0 && interval >= 0) {
                    Text(
                        stringResource(
                            MR.strings.manga_interval_expected_update,
                            pluralStringResource(
                                MR.plurals.day,
                                count = nextUpdateDays,
                                nextUpdateDays,
                            ),
                            pluralStringResource(
                                MR.plurals.day,
                                count = interval.absoluteValue,
                                interval.absoluteValue,
                            ),
                        ),
                    )
                } else {
                    Text(
                        stringResource(MR.strings.manga_interval_expected_update_null),
                    )
                }
                Spacer(Modifier.height(MaterialTheme.padding.small))

                if (onValueChanged != null && (!isReleaseBuildType)) {
                    Text(stringResource(MR.strings.manga_interval_custom_amount))

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val size = DpSize(width = maxWidth / 2, height = 128.dp)
                        val items = (0..FetchInterval.MAX_INTERVAL)
                            .map {
                                if (it == 0) {
                                    stringResource(MR.strings.label_default)
                                } else {
                                    it.toString()
                                }
                            }
                            .toImmutableList()
                        WheelTextPicker(
                            items = items,
                            size = size,
                            startIndex = selectedInterval,
                            onSelectionChanged = { selectedInterval = it },
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onValueChanged?.invoke(selectedInterval)
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

/**
 * 新增的對話框，用於編輯漫畫資訊
 *
 * @param onDismissRequest 當使用者請求關閉對話框時呼叫
 * @param manga 要編輯的漫畫物件
 * @param onConfirm 當使用者點擊確認按鈕時呼叫，回傳更新後的資訊
 */
@Composable
fun EditMangaInfoDialog(
    onDismissRequest: () -> Unit,
    manga: Manga,
    onConfirm: (title: String, author: String, artist: String, description: String) -> Unit,
) {
    // 為每個輸入欄位建立可記住的狀態
    // 使用 rememberSaveable 以在畫面旋轉等配置變更後保留使用者輸入
    // 初始值優先使用自定義資訊，如果不存在則使用原始資訊
    var title by rememberSaveable(manga) { mutableStateOf(manga.customTitle ?: manga.title) }
    var author by rememberSaveable(manga) { mutableStateOf(manga.customAuthor ?: manga.author ?: "") }
    var artist by rememberSaveable(manga) { mutableStateOf(manga.customArtist ?: manga.artist ?: "") }
    var description by rememberSaveable(manga) { mutableStateOf(manga.customDescription ?: manga.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.action_edit_info)) },
        text = {
            // 使用可滾動的 Column，以防描述過長而無法顯示
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                // 標題輸入框
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(MR.strings.title)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.padding.small))

                // 作者輸入框
                TextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(text = stringResource(MR.strings.author)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.padding.small))

                // 繪者輸入框
                TextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(text = stringResource(MR.strings.artist)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.padding.small))

                // 描述輸入框
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = stringResource(MR.strings.description)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(title.trim(), author.trim(), artist.trim(), description.trim())
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
