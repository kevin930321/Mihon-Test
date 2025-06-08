package tachiyomi.data.source

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.domain.source.service.SourceManager

class SourceRepositoryImpl(
    private val sourceManager: SourceManager,
    private val networkToLocalManga: NetworkToLocalManga,
    private val handler: DatabaseHandler,
) : SourceRepository {

    override fun getSources(): Flow<List<Source>> {
        return sourceManager.getSources()
    }

    override fun getOnlineSources(): Flow<List<Source>> {
        return sourceManager.getOnlineSources()
    }

    override fun getSourcesWithFavoriteCount(): Flow<List<SourceWithCount>> {
        return handler.subscribeToList { mangasQueries.getSourceIdWithFavoriteCount() }
            .map { list ->
                list.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    SourceWithCount(source, count)
                }
            }
    }

    override fun getSourcesWithNonLibraryManga(): Flow<List<SourceWithCount>> {
        return handler.subscribeToList { mangasQueries.getSourceIdsWithNonLibraryManga() }
            .map { list ->
                list.map { (sourceId, count) ->
                    val source = sourceManager.getOrStub(sourceId)
                    SourceWithCount(source, count)
                }
            }
    }

    override fun search(
        source: CatalogueSource,
        query: String,
        filterList: FilterList,
    ): Flow<PagingData<Manga>> {
        return Pager(
            config = PagingConfig(pageSize = 25),
            pagingSourceFactory = {
                object : SourcePagingSource() {
                    override val source: CatalogueSource = source
                    override val query: String = query
                    override val filterList: FilterList = filterList
                    override val networkToLocalManga: NetworkToLocalManga = this@SourceRepositoryImpl.networkToLocalManga
                }
            },
        ).flow
    }

    override fun getPopular(source: CatalogueSource): Flow<PagingData<Manga>> {
        return Pager(
            config = PagingConfig(pageSize = 25),
            pagingSourceFactory = {
                object : SourcePagingSource() {
                    override val source: CatalogueSource = source
                    override val query: String = ""
                    override val filterList: FilterList = FilterList()
                    override val networkToLocalManga: NetworkToLocalManga = this@SourceRepositoryImpl.networkToLocalManga
                }
            },
        ).flow
    }

    override fun getLatest(source: CatalogueSource): Flow<PagingData<Manga>> {
        return Pager(
            config = PagingConfig(pageSize = 25),
            pagingSourceFactory = {
                object : SourcePagingSource() {
                    override val source: CatalogueSource = source
                    override val query: String = ""
                    override val filterList: FilterList = FilterList()
                    override val networkToLocalManga: NetworkToLocalManga = this@SourceRepositoryImpl.networkToLocalManga
                }
            },
        ).flow
    }
}
