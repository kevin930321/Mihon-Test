package tachiyomi.data.manga

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.repository.MangaRepository
import java.time.LocalDate
import java.time.ZoneId

class MangaRepositoryImpl(
    private val handler: DatabaseHandler,
) : MangaRepository {

    override suspend fun getMangaById(id: Long): Manga {
        return handler.awaitOne { mangasQueries.getMangaById(id, MangaMapper::mapManga) }
    }

    override suspend fun getMangaByIdAsFlow(id: Long): Flow<Manga> {
        return handler.subscribeToOne { mangasQueries.getMangaById(id, MangaMapper::mapManga) }
    }

    override suspend fun getMangaByUrlAndSourceId(url: String, sourceId: Long): Manga? {
        return handler.awaitOneOrNull {
            mangasQueries.getMangaByUrlAndSource(
                url,
                sourceId,
                MangaMapper::mapManga,
            )
        }
    }

    override fun getMangaByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?> {
        return handler.subscribeToOneOrNull {
            mangasQueries.getMangaByUrlAndSource(
                url,
                sourceId,
                MangaMapper::mapManga,
            )
        }
    }

    override suspend fun getFavorites(): List<Manga> {
        return handler.awaitList { mangasQueries.getFavorites(MangaMapper::mapManga) }
    }

    override suspend fun getReadMangaNotInLibrary(): List<Manga> {
        return handler.awaitList { mangasQueries.getReadMangaNotInLibrary(MangaMapper::mapManga) }
    }

    override suspend fun getLibraryManga(): List<LibraryManga> {
        return handler.awaitList { libraryViewQueries.library(MangaMapper::mapLibraryManga) }
    }

    override fun getLibraryMangaAsFlow(): Flow<List<LibraryManga>> {
        return handler.subscribeToList { libraryViewQueries.library(MangaMapper::mapLibraryManga) }
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Manga>> {
        return handler.subscribeToList { mangasQueries.getFavoriteBySourceId(sourceId, MangaMapper::mapManga) }
    }

    override suspend fun getDuplicateLibraryManga(id: Long, title: String): List<MangaWithChapterCount> {
        return handler.awaitList {
            mangasQueries.getDuplicateLibraryManga(id, title, MangaMapper::mapMangaWithChapterCount)
        }
    }

    override suspend fun getUpcomingManga(statuses: Set<Long>): Flow<List<Manga>> {
        val epochMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return handler.subscribeToList {
            mangasQueries.getUpcomingManga(epochMillis, statuses, MangaMapper::mapManga)
        }
    }

    override suspend fun resetViewerFlags(): Boolean {
        return try {
            handler.await { mangasQueries.resetViewerFlags() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            mangas_categoriesQueries.deleteMangaCategoryByMangaId(mangaId)
            categoryIds.map { categoryId ->
                mangas_categoriesQueries.insert(mangaId, categoryId)
            }
        }
    }

    override suspend fun update(update: MangaUpdate): Boolean {
        return try {
            partialUpdate(update)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateAll(mangaUpdates: List<MangaUpdate>): Boolean {
        return try {
            partialUpdate(*mangaUpdates.toTypedArray())
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    // `insert` 函數在您提供的程式碼中不存在，但為了完整性，從專案的邏輯推斷它應該是這樣
    override suspend fun insert(manga: Manga): Long? {
        return handler.awaitOneOrNull(inTransaction = true) {
            mangasQueries.insert(
                source = manga.source,
                url = manga.url,
                artist = manga.artist,
                author = manga.author,
                description = manga.description,
                genre = manga.genre,
                title = manga.title,
                status = manga.status,
                thumbnailUrl = manga.thumbnailUrl,
                favorite = manga.favorite,
                lastUpdate = manga.lastUpdate,
                nextUpdate = manga.nextUpdate,
                initialized = manga.initialized,
                viewerFlags = manga.viewerFlags,
                chapterFlags = manga.chapterFlags,
                coverLastModified = manga.coverLastModified,
                dateAdded = manga.dateAdded,
                updateStrategy = manga.updateStrategy,
                calculateInterval = manga.fetchInterval.toLong(),
                version = manga.version,
            )
            mangasQueries.selectLastInsertedRowId().executeAsOneOrNull()
        }
    }


    /**
     * [主要修改]
     * 這個函數被完全重寫以解決問題。
     *
     * 1. **查詢名稱修正**: 從 `mangasQueries.update` 改為 `mangasQueries.updateManga` 以匹配 .sq 檔案。
     * 2. **邏輯修正**: `MangaUpdate` 物件只包含部分更新。但我們的 `updateManga` SQL 查詢需要所有欄位。
     *    因此，我們在更新前先 `getMangaById` 獲取舊的完整資料，然後用 `MangaUpdate` 中的新值（如果非 null）覆蓋舊值，
     *    最後將完整的資料傳給 `updateManga` 查詢。這確保了我們不會意外地用 null 清除現有資料。
     */
    private suspend fun partialUpdate(vararg mangaUpdates: MangaUpdate) {
        handler.await(inTransaction = true) {
            for (update in mangaUpdates) {
                val oldManga = handler.awaitOne { mangasQueries.getMangaById(update.id, MangaMapper::mapManga) }

                mangasQueries.updateManga(
                    id = oldManga.id,
                    url = update.url ?: oldManga.url,
                    source = update.source ?: oldManga.source,
                    favorite = if (update.favorite != null) if (update.favorite) 1 else 0 else if (oldManga.favorite) 1 else 0,
                    last_update = update.lastUpdate ?: oldManga.lastUpdate,
                    next_update = update.nextUpdate ?: oldManga.nextUpdate,
                    date_added = update.dateAdded ?: oldManga.dateAdded,
                    viewer_flags = update.viewerFlags ?: oldManga.viewerFlags,
                    chapter_flags = update.chapterFlags ?: oldManga.chapterFlags,
                    cover_last_modified = update.coverLastModified ?: oldManga.coverLastModified,
                    title = update.title ?: oldManga.title,
                    artist = update.artist ?: oldManga.artist,
                    author = update.author ?: oldManga.author,
                    description = update.description ?: oldManga.description,
                    genre = update.genre ?: oldManga.genre,
                    status = update.status ?: oldManga.status,
                    thumbnail_url = update.thumbnailUrl ?: oldManga.thumbnailUrl,
                    update_strategy = update.updateStrategy ?: oldManga.updateStrategy,
                    initialized = if (update.initialized != null) if (update.initialized) 1 else 0 else if (oldManga.initialized) 1 else 0,
                    
                    // 傳遞新增的自定義欄位
                    customTitle = update.customTitle ?: oldManga.customTitle,
                    customAuthor = update.customAuthor ?: oldManga.customAuthor,
                    customArtist = update.customArtist ?: oldManga.customArtist,
                    customDescription = update.customDescription ?: oldManga.customDescription,
                )
            }
        }
    }
}
