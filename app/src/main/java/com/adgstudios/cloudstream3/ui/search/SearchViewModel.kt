package com.adgstudios.cloudstream3.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adgstudios.cloudstream3.APIHolder.apis
import com.adgstudios.cloudstream3.AcraApplication.Companion.getKey
import com.adgstudios.cloudstream3.AcraApplication.Companion.getKeys
import com.adgstudios.cloudstream3.AcraApplication.Companion.setKey
import com.adgstudios.cloudstream3.SearchResponse
import com.adgstudios.cloudstream3.apmap
import com.adgstudios.cloudstream3.mvvm.Resource
import com.adgstudios.cloudstream3.ui.APIRepository
import com.adgstudios.cloudstream3.utils.Coroutines.ioSafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OnGoingSearch(
    val apiName: String,
    val data: Resource<List<SearchResponse>>
)

const val SEARCH_HISTORY_KEY = "search_history"

class SearchViewModel : ViewModel() {
    private val _searchResponse: MutableLiveData<Resource<List<SearchResponse>>> =
        MutableLiveData()
    val searchResponse: LiveData<Resource<List<SearchResponse>>> get() = _searchResponse

    private val _currentSearch: MutableLiveData<List<OnGoingSearch>> = MutableLiveData()
    val currentSearch: LiveData<List<OnGoingSearch>> get() = _currentSearch

    private val _currentHistory: MutableLiveData<List<SearchHistoryItem>> = MutableLiveData()
    val currentHistory: LiveData<List<SearchHistoryItem>> get() = _currentHistory

    private val repos = apis.map { APIRepository(it) }

    fun clearSearch() {
        _searchResponse.postValue(Resource.Success(ArrayList()))
        _currentSearch.postValue(emptyList())
    }

    private var onGoingSearch: Job? = null
    fun searchAndCancel(
        query: String,
        providersActive: Set<String> = setOf(),
        ignoreSettings: Boolean = false
    ) {
        onGoingSearch?.cancel()
        onGoingSearch = search(query, providersActive, ignoreSettings)
    }

    fun updateHistory() = viewModelScope.launch {
        ioSafe {
            val items = getKeys(SEARCH_HISTORY_KEY)?.mapNotNull {
                getKey<SearchHistoryItem>(it)
            }?.sortedByDescending { it.searchedAt } ?: emptyList()
            _currentHistory.postValue(items)
        }
    }

    private fun search(
        query: String,
        providersActive: Set<String>,
        ignoreSettings: Boolean = false
    ) =
        viewModelScope.launch {
            if (query.length <= 1) {
                clearSearch()
                return@launch
            }

            val key = query.hashCode().toString()
            setKey(
                SEARCH_HISTORY_KEY,
                key,
                SearchHistoryItem(
                    searchedAt = System.currentTimeMillis(),
                    searchText = query,
                    type = emptyList(), // TODO implement tv type
                    key = key,
                )
            )

            _searchResponse.postValue(Resource.Loading())

            val currentList = ArrayList<OnGoingSearch>()

            _currentSearch.postValue(ArrayList())

            withContext(Dispatchers.IO) { // This interrupts UI otherwise
                repos.filter { a ->
                    ignoreSettings || (providersActive.isEmpty() || providersActive.contains(a.name))
                }.apmap { a -> // Parallel
                    val search = a.search(query)
                    currentList.add(OnGoingSearch(a.name, search))
                    _currentSearch.postValue(currentList)
                }
            }
            _currentSearch.postValue(currentList)

            val list = ArrayList<SearchResponse>()
            val nestedList =
                currentList.map { it.data }
                    .filterIsInstance<Resource.Success<List<SearchResponse>>>().map { it.value }

            // I do it this way to move the relevant search results to the top
            var index = 0
            while (true) {
                var added = 0
                for (sublist in nestedList) {
                    if (sublist.size > index) {
                        list.add(sublist[index])
                        added++
                    }
                }
                if (added == 0) break
                index++
            }

            _searchResponse.postValue(Resource.Success(list))
        }
}