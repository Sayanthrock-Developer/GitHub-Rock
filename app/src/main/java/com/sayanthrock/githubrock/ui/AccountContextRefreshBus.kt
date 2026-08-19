package com.sayanthrock.githubrock.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Process-local event used to refresh the existing navigation state after an account/org switch. */
object AccountContextRefreshBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun requestRefresh() {
        _events.tryEmit(Unit)
    }
}
