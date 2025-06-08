package tachiyomi.domain.manga.interactor

// [修正] 新增了這兩個關鍵的 import 語句。
// 這會告訴編譯器去哪裡尋找 MangaUpdate 和 UpdateManga 的定義，解決 "Unresolved reference" 錯誤。
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
