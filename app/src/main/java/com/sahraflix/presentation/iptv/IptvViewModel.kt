package com.sahraflix.presentation.iptv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class IptvTab(val streamType: StreamType) {
    LIVE(StreamType.LIVE), MOVIES(StreamType.MOVIE), SERIES(StreamType.SERIES)
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class IptvViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val contentRepository: ContentRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(IptvTab.LIVE)
    val selectedTab: StateFlow<IptvTab> = _selectedTab
    
    fun selectTab(tab: IptvTab) {
        _selectedTab.value = tab
        _selectedCategoryId.value = null // reset category when switching tabs
    }

    val categories: StateFlow<List<CategoryEntity>> = _selectedTab.flatMapLatest { tab ->
        categoryDao.observeByType(tab.streamType)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId

    fun selectCategory(id: String?) {
        _selectedCategoryId.value = id
    }

    // Paged channels for the selected tab and category
    val channels: Flow<PagingData<CatalogEntry>> = combine(_selectedTab, _selectedCategoryId) { tab, catId ->
        tab to catId
    }.flatMapLatest { (tab, catId) ->
        contentRepository.iptv(tab.streamType, catId)
    }.cachedIn(viewModelScope)
}
