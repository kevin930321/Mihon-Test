package mihon.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import mihon.domain.manga.model.Manga
import mihon.domain.manga.model.MangaUpdate

interface MangaRepository {

    suspend fun getMangaById(id: Long): Manga

    suspend fun getMangaByIdAsFlow(id: Long): Flow<Manga>

    suspend fun getMangaByUrlAndSourceId(url: String, sourceId: Long): Manga?

    fun getMangaByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?>

    suspend fun getFavorites(): List<Manga>

    suspend fun getLibraryManga(): List<Manga>

    fun getLibraryMangaAsFlow(): Flow<List<Manga>>

    fun getMangaFavoritesPerCategoryAsFlow(): Flow<Map<Long, List<Manga>>>

    suspend fun getMangaDuplicates(title: String, sourceId: Long): List<Manga>

    suspend fun resetMangaViewerFlags(): Boolean

    suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>)

    suspend fun insertManga(manga: Manga): Long?

    suspend fun updateManga(update: MangaUpdate): Boolean

    suspend fun updateAllManga(mangaUpdates: List<MangaUpdate>): Boolean

    /**
     * 新增的函式：更新漫畫的自訂資訊。
     * 這個函式會接收一個 Manga 物件，並將其中包含的自訂標題、作者等資訊
     * 更新到資料庫中對應的漫畫條目。
     *
     * @param manga 包含要更新的自訂資訊的 Manga 物件。
     *              實作時應只更新 customTitle, customAuthor 等自訂欄位。
     * @return 如果更新成功則返回 true，否則返回 false。
     */
    suspend fun setCustomMangaInfo(manga: Manga): Boolean
}
