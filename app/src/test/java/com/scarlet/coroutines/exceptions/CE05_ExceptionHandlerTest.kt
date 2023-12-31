package com.scarlet.coroutines.exceptions

import com.scarlet.util.completeStatus
import com.scarlet.util.log
import com.scarlet.util.onCompletion
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.lang.RuntimeException
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class ExceptionHandlerTest {

    private val ehandler = CoroutineExceptionHandler { context, exception ->
        log("Global exception handler Caught $exception in: $context")
    }

    /**
     * Coroutine Exception Handlers installed at scope
     */

    @Test
    fun `CEH at the scope`() = runTest(timeout = 20_000.seconds) {
        val scope = CoroutineScope(Job() + ehandler)

        scope.launch {
            launch {
                delay(10_000)
                throw RuntimeException("oops")
            }.onCompletion("child1")

            launch {
                delay(20_000)
            }.onCompletion("child2")

        }.onCompletion("parent").join()

        scope.completeStatus("scope") // Is scope cancelled?
    }

    /**
     * Coroutine Exception Handlers installed at root coroutines
     */

    @Test
    // Why top-level scope cancelled?
    fun `CEH at the root coroutine - child of scope`() = runTest {
        val scope = CoroutineScope(Job())

        scope.launch(ehandler) {
            launch {
                delay(100)
                throw RuntimeException("oops")
            }.onCompletion("child1")

            launch {
                delay(200)
            }.onCompletion("child2")
        }.onCompletion("parent").join()

        scope.completeStatus("scope") // Is scope cancelled?
    }

    @Test
    fun `CEH at the root coroutine - child of supervisorScope`() = runTest {
        supervisorScope {
            launch(ehandler) {
                launch {
                    delay(100)
                    throw RuntimeException("oops")
                }.onCompletion("child1")

                launch {
                    delay(200)
                }.onCompletion("child2")
            }.onCompletion("parent")
        }
    }

    /**
     * CEHs installed neither at the scope nor at root coroutines do not take effect.
     */

    @Test
    fun `CEH not at the root coroutine - child of coroutineScope`() = runTest {
        coroutineScope {
            launch(ehandler) {
                launch {
                    delay(100)
                    throw RuntimeException("oops")
                }.onCompletion("child1")

                launch { delay(200) }.onCompletion("child2")
            }.onCompletion("parent")
        }
    }

    @Test
    fun `CEH not at the root coroutine - not a direct child of scope`() = runTest {
        val scope = CoroutineScope(Job())

        scope.launch {
            launch(ehandler) {
                delay(100)
                throw RuntimeException("oops")
            }.onCompletion("child1")

            launch {
                delay(200)
            }.onCompletion("child2")
        }.onCompletion("parent").join()

        scope.completeStatus("scope") // Is scope cancelled?
    }
}

