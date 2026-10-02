package dev.sol.accesshub.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How many accessibility services are enabled, shared process-wide.
 *
 * The badge in the navigation bar lives outside the services screen, so toggling a service there has
 * to publish the new count at once instead of waiting for the next tab switch to re-read the
 * settings database. `null` means nothing has published a value yet.
 */
object EnabledServicesCounter {
    private val _count = MutableStateFlow<Int?>(null)
    val count: StateFlow<Int?> = _count.asStateFlow()

    fun publish(value: Int) {
        _count.value = value
    }
}
