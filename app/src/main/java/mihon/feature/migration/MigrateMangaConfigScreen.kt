package mihon.feature.migration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.updateAndGet
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MigrateMangaConfigScreen(private val mangaId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { ScreenModel() }
        val state by screenModel.state.collectAsState()
        val (selectedSources, availableSources) = state.sources.partition { it.isSelected }
        val showLanguage by remember(state) {
            derivedStateOf {
                state.sources.distinctBy { it.source.lang }.size > 1
            }
        }

        val lazyListState = rememberLazyListState()
        Scaffold(
            topBar = {
                AppBar(
                    title = null,
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectAllLabel),
                                    icon = Icons.Outlined.SelectAll,
                                    onClick = { screenModel.toggleSelection(ScreenModel.SelectionConfig.All) },
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectNoneLabel),
                                    icon = Icons.Outlined.Deselect,
                                    onClick = { screenModel.toggleSelection(ScreenModel.SelectionConfig.None) },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectEnabledLabel),
                                    onClick = { screenModel.toggleSelection(ScreenModel.SelectionConfig.Enabled) },
                                ),
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.migrationConfigScreen_selectPinnedLabel),
                                    onClick = { screenModel.toggleSelection(ScreenModel.SelectionConfig.Pinned) },
                                ),
                            ),
                        )
                    },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                    icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null) },
                    onClick = { navigator.replace(MigrateSearchScreen(mangaId)) },
                    expanded = lazyListState.shouldExpandFAB(),
                )
            },
        ) { contentPadding ->
            val reorderableState = rememberReorderableLazyListState(lazyListState, contentPadding) { from, to ->
                val fromIndex = selectedSources.indexOfFirst { it.id == from.key }
                val toIndex = selectedSources.indexOfFirst { it.id == to.key }
                if (fromIndex == -1 || toIndex == -1) return@rememberReorderableLazyListState
                screenModel.orderSource(fromIndex, toIndex)
            }

            FastScrollLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = contentPadding,
            ) {
                listOf(selectedSources, availableSources).fastForEachIndexed { listIndex, sources ->
                    val selectedSourcesList = listIndex == 0
                    if (sources.isNotEmpty()) {
                        val headerPrefix = if (selectedSourcesList) "selected" else "available"
                        item("$headerPrefix-header") {
                            Text(
                                text = stringResource(
                                    resource = if (selectedSourcesList) {
                                        MR.strings.migrationConfigScreen_selectedHeader
                                    } else {
                                        MR.strings.migrationConfigScreen_availableHeader
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(MaterialTheme.padding.medium)
                                    .animateItem(),
                            )
                        }
                    }
                    itemsIndexed(
                        items = sources,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        SourceItemContainer(
                            firstItem = index == 0,
                            lastItem = index == (sources.size - 1),
                            source = item.source,
                            showLanguage = showLanguage,
                            isSelected = item.isSelected,
                            dragEnabled = selectedSourcesList && sources.size > 1,
                            state = reorderableState,
                            key = { if (selectedSourcesList) it.id else "available-${it.id}" },
                            onClick = { screenModel.toggleSelection(item.id) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun LazyItemScope.SourceItemContainer(
        firstItem: Boolean,
        lastItem: Boolean,
        source: Source,
        showLanguage: Boolean,
        isSelected: Boolean,
        dragEnabled: Boolean,
        state: ReorderableLazyListState,
        key: (Source) -> Any,
        onClick: () -> Unit,
    ) {
        val shape = remember(firstItem, lastItem) {
            val top = if (firstItem) 12.dp else 0.dp
            val bottom = if (lastItem) 12.dp else 0.dp
            RoundedCornerShape(top, top, bottom, bottom)
        }

        ReorderableItem(
            state = state,
            key = key(source),
            enabled = dragEnabled,
        ) { _ ->
            ElevatedCard(
                shape = shape,
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.padding.medium)
                    .animateItem(),
            ) {
                SourceItem(
                    source = source,
                    showLanguage = showLanguage,
                    isSelected = isSelected,
                    dragEnabled = dragEnabled,
                    scope = this@ReorderableItem,
                    onClick = onClick,
                )
            }
        }

        if (!lastItem) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium))
        }
    }

    @Composable
    private fun SourceItem(
        source: Source,
        showLanguage: Boolean,
        isSelected: Boolean,
        dragEnabled: Boolean,
        scope: ReorderableCollectionItemScope,
        onClick: () -> Unit,
    ) {
        ListItem(
            headlineContent = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SourceIcon(source = source)
                    Text(
                        text = source.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (showLanguage) {
                        Pill(
                            text = LocaleHelper.getLocalizedDisplayName(source.lang),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            trailingContent = if (dragEnabled) {
                {
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = null,
                        modifier = with(scope) {
                            Modifier.draggableHandle()
                        },
                    )
                }
            } else {
                null
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
            modifier = Modifier
                .clickable(onClick = onClick)
                .alpha(if (isSelected) 1f else DISABLED_ALPHA),
        )
    }

    private class ScreenModel(
        private val sourceManager: SourceManager = Injekt.get(),
        private val sourcePreferences: SourcePreferences = Injekt.get(),
    ) : StateScreenModel<ScreenModel.State>(State()) {

        init {
            screenModelScope.launchIO {
                initSources()
            }
        }

        private val sourcesComparator = { includedSources: List<Long> ->
            compareBy<MigrationSource>(
                { !it.isSelected },
                { includedSources.indexOf(it.source.id) },
                { with(it.source) { "$name (${LocaleHelper.getLocalizedDisplayName(lang)})" } },
            )
        }

        private fun updateSources(save: Boolean = true, action: (List<MigrationSource>) -> List<MigrationSource>) {
            val state = mutableState.updateAndGet { state ->
                val updatedSources = action(state.sources)
                val includedSources = updatedSources.mapNotNull { if (!it.isSelected) null else it.id }
                state.copy(sources = updatedSources.sortedWith(sourcesComparator(includedSources)))
            }
            if (!save) return
            state.sources
                .filter { source -> source.isSelected }
                .map { source -> source.source.id }
                .let { sources -> sourcePreferences.migrationSources().set(sources) }
        }

        private fun initSources() {
            val languages = sourcePreferences.enabledLanguages().get()
            val pinnedSources = sourcePreferences.pinnedSources().get().mapNotNull { it.toLongOrNull() }
            val includedSources = sourcePreferences.migrationSources().get()
            val disabledSources = sourcePreferences.disabledSources().get()
                .mapNotNull { it.toLongOrNull() }
            val sources = sourceManager.getCatalogueSources()
                .asSequence()
                .filterIsInstance<HttpSource>()
                .filter { it.lang in languages }
                .map {
                    val source = Source(
                        id = it.id,
                        lang = it.lang,
                        name = it.name,
                        supportsLatest = false,
                        isStub = false,
                    )
                    MigrationSource(
                        source = source,
                        isSelected = when {
                            includedSources.isNotEmpty() -> source.id in includedSources
                            pinnedSources.isNotEmpty() -> source.id in pinnedSources
                            else -> source.id !in disabledSources
                        },
                    )
                }
                .toList()

            updateSources(save = false) { sources }
        }

        fun toggleSelection(id: Long) {
            updateSources { sources ->
                sources.map { source ->
                    source.copy(isSelected = if (source.source.id == id) !source.isSelected else source.isSelected)
                }
            }
        }

        fun toggleSelection(config: SelectionConfig) {
            val pinnedSources = sourcePreferences.pinnedSources().get().mapNotNull { it.toLongOrNull() }
            val disabledSources = sourcePreferences.disabledSources().get().mapNotNull { it.toLongOrNull() }
            val isSelected: (Long) -> Boolean = {
                when (config) {
                    SelectionConfig.All -> true
                    SelectionConfig.None -> false
                    SelectionConfig.Pinned -> it in pinnedSources
                    SelectionConfig.Enabled -> it !in disabledSources
                }
            }
            updateSources { sources ->
                sources.map { source ->
                    source.copy(isSelected = isSelected(source.source.id))
                }
            }
        }

        fun orderSource(from: Int, to: Int) {
            updateSources {
                it.toMutableList()
                    .apply {
                        add(to, removeAt(from))
                    }
                    .toList()
            }
        }

        data class State(
            val sources: List<MigrationSource> = emptyList(),
        )

        enum class SelectionConfig {
            All,
            None,
            Pinned,
            Enabled,
        }
    }

    data class MigrationSource(
        val source: Source,
        val isSelected: Boolean,
    ) {
        val id = source.id
        val visualName = source.visualName
    }
}
