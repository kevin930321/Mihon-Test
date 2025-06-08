package tachiyomi.domain.manga.interactor

// [最終修正] 新增了 UpdateManga 和 MangaUpdate 這兩個關鍵的 import 語句。
import tachiyomi.domain.manga.model.MangaUpdate

class SetCustomMangaInfo(
    private val updateManga: UpdateManga,
) {
    suspend fun await(
        mangaId: Long,
        customTitle: String?,
        customAuthor: String?,
        customArtist: String?,
        customDescription: String?,
    ): Boolean {
        val update = MangaUpdate(
            id = mangaId,
            customTitle = customTitle,
            customAuthor = customAuthor,
            customArtist = customArtist,
            customDescription = customDescription,
        )
        // 現在因為 import 了 UpdateManga，這裡的 await 就可以被正確解析了
        return updateManga.await(update)
    }
}
