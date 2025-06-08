package tachiyomi.data.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.toDomainManga
import java.io.IOException
import java.net.HttpRetryException

abstract class SourcePagingSource : PagingSource<Long, Manga>() {

    abstract val source: CatalogueSource
    abstract val query: String
    abstract val filterList: FilterList

    abstract val networkToLocalManga: NetworkToLocalManga

    override fun getRefreshKey(state: PagingState<Long, Manga>): Long? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Manga> {
        val page = params.key ?: 1

        val sourceFilters = filterList

        val result = try {
            source.getSearchManga(page.toInt(), query, sourceFilters)
        } catch (e: Exception) {
            if (e is HttpRetryException) {
                return LoadResult.Error(e)
            }
            if (e is IOException || e is NullPointerException) {
                return LoadResult.Error(e)
            }
            throw e
        }

        val mangaList = result.mangas
            .map { it.toDomainManga(source.id) }
            .map { networkToLocalManga.await(it) }

        return toLoadResult(result, mangaList, page)
    }

    private fun toLoadResult(
        mangasPage: MangasPage,
        manga: List<Manga>,
        page: Long,
    ): LoadResult.Page<Long, Manga> {
        return LoadResult.Page(
            data = manga,
            prevKey = if (page == 1L) null else page - 1,
            nextKey = if (mangasPage.hasNextPage) page + 1 else null,
        )
    }
}
