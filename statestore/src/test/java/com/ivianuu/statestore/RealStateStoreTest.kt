/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.statestore

import com.ivianuu.statestore.util.TestCloseListener
import com.ivianuu.statestore.util.TestExecutor
import com.ivianuu.statestore.util.TestState
import com.ivianuu.statestore.util.TestStateListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealStateStoreTest {

    private val executor = TestExecutor()
    private val callbackExecutor = TestExecutor()
    private val store = RealStateStore(TestState(), executor, callbackExecutor)

    @Test
    fun testCloseListener() {
        val listener = TestCloseListener()
        store.addCloseListener(listener)
        assertEquals(0, listener.closeCalls)
        store.close()
        assertEquals(1, listener.closeCalls)
        store.close()
        assertEquals(1, listener.closeCalls)
    }

    @Test
    fun testRemoveCloseListener() {
        val listener = TestCloseListener()
        store.addCloseListener(listener)
        store.removeCloseListener(listener)
        store.close()
        assertEquals(0, listener.closeCalls)
    }

    @Test
    fun testCloseListenerAddedTwice() {
        val listener = TestCloseListener()
        store.addCloseListener(listener)
        store.addCloseListener(listener)
        store.close()
        assertEquals(1, listener.closeCalls)
    }

    @Test
    fun testGetAndSetState() {
        var called = false
        store.withState {
            called = true
            assertEquals(TestState(0), it)
        }

        assertTrue(called)

        called = false
        store.setState {
            called = true
            inc()
        }

        assertTrue(called)

        store.withState {
            called = true
            assertEquals(TestState(1), it)
        }

        assertTrue(called)
    }

    @Test
    fun testGetAndSetStateWhileClosed() {
        store.close()

        var called = false
        store.withState { called = true }

        assertFalse(called)

        called = false
        store.setState {
            called = true
            inc()
        }

        assertFalse(called)
    }

    @Test
    fun testPeekState() {
        assertEquals(TestState(0), store.peekState())
        store.setState { inc() }
        assertEquals(TestState(1), store.peekState())
    }

    @Test
    fun testListener() {
        val listener = TestStateListener<TestState>()
        val expectedState = mutableListOf<TestState>()
        expectedState.add(TestState(0))

        store.addStateListener(listener)
        assertEquals(expectedState, listener.history)

        store.setState { inc() }
        expectedState.add(TestState(1))
        assertEquals(expectedState, listener.history)

        store.removeStateListener(listener)

        store.setState { inc() }
        assertEquals(expectedState, listener.history)
    }

    @Test
    fun testListenerNotCalledForNoop() {
        val listener = TestStateListener<TestState>()
        store.addStateListener(listener)

        assertEquals(1, listener.calls)

        store.setState { this }

        assertEquals(1, listener.calls)
    }

    @Test
    fun testListenerNotCalledForSameValue() {
        val listener = TestStateListener<TestState>()
        store.addStateListener(listener)

        assertEquals(1, listener.calls)

        store.setState { copy() }

        assertEquals(1, listener.calls)
    }

    @Test
    fun testExecutorUsage() {
        assertEquals(0, executor.executeCalls)
        assertEquals(0, callbackExecutor.executeCalls)

        store.setState { inc() }

        assertEquals(1, executor.executeCalls)
        assertEquals(1, callbackExecutor.executeCalls)

        store.setState { this }
        assertEquals(2, executor.executeCalls)
        assertEquals(1, callbackExecutor.executeCalls)
    }

    @Test
    fun testDefaultExecutor() {
        StateStorePlugins.defaultExecutor = executor
        StateStorePlugins.defaultCallbackExecutor = callbackExecutor

        val store = StateStore(TestState()) as RealStateStore

        assertEquals(executor, store.executor)
        assertEquals(callbackExecutor, store.callbackExecutor)

        StateStorePlugins.defaultExecutor = null
        StateStorePlugins.defaultCallbackExecutor = null
    }
}