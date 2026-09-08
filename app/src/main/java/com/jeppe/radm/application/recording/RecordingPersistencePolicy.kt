package com.jeppe.radm.application.recording

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class RecordingPersistenceException(
    val attempts: Int,
    cause: Throwable,
) : IllegalStateException(
    "Recording data could not be saved after $attempts attempts",
    cause,
)

/** REC R00 sections 85-87: one write plus three bounded retries with short backoff. */
class RecordingPersistencePolicy(
    private val retryCount: Int = DEFAULT_RETRY_COUNT,
    private val backoff: suspend (retryNumber: Int) -> Unit = { retryNumber ->
        delay(BASE_BACKOFF_MS * retryNumber)
    },
) {
    init {
        require(retryCount >= 0) { "Retry count must be non-negative" }
    }

    suspend fun <T> execute(write: suspend () -> T): T {
        var lastFailure: Throwable? = null
        repeat(retryCount + 1) { attemptIndex ->
            try {
                return write()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                lastFailure = failure
                if (attemptIndex < retryCount) backoff(attemptIndex + 1)
            }
        }
        throw RecordingPersistenceException(retryCount + 1, checkNotNull(lastFailure))
    }

    companion object {
        const val DEFAULT_RETRY_COUNT = 3
        private const val BASE_BACKOFF_MS = 25L
    }
}
