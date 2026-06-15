package com.example.truetrackfinance.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.truetrackfinance.data.db.entity.Badge
import com.example.truetrackfinance.data.model.BadgeKey
import com.example.truetrackfinance.data.repository.BadgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BadgeViewModel"

@HiltViewModel
class BadgeViewModel @Inject constructor(
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    private val _userId = MutableStateFlow<Long>(-1L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val badges: LiveData<List<Badge>> = _userId
        .filter { it > 0 }
        .flatMapLatest { badgeRepository.observeBadges(it) }
        .asLiveData(viewModelScope.coroutineContext)

    /** Event for triggering celebration UI when a badge is earned. */
    private val _newBadgeAwarded = MutableLiveData<BadgeKey?>()
    val newBadgeAwarded: LiveData<BadgeKey?> = _newBadgeAwarded

    fun initialise(userId: Long) {
        _userId.value = userId
    }

    fun onBadgeAwarded(key: BadgeKey) {
        _newBadgeAwarded.value = key
    }

    fun clearNewBadgeEvent() {
        _newBadgeAwarded.value = null
    }
}
