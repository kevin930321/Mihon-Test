package tachiyomi.domain.manga.interactor

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
        return updateManga.await(update)
    }
}
