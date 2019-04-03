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

import com.ivianuu.closeable.Closeable
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class RealStateStore<T>(
    initialState: T,
    internal val executor: Executor,
    internal val callbackExecutor: Executor
) : StateStore<T> {

    private var state = initialState

    override val isClosed: Boolean
        get() = _isClosed
    private var _isClosed = false

    private val closeListeners = mutableSetOf<CloseListener>()
    private val stateListeners = mutableSetOf<StateListener<T>>()

    private val lock = ReentrantLock()

    private val jobs = Jobs<T>()

    override fun withState(consumer: Consumer<T>): Unit = lock.withLock {
        if (_isClosed) return@withLock
        jobs.enqueueGet(consumer)
        executor.execute(this::flushQueues)
    }

    override fun setState(reducer: Reducer<T>): Unit = lock.withLock {
        if (_isClosed) return@withLock
        jobs.enqueueSet(reducer)
        executor.execute(this::flushQueues)
    }

    override fun peekState(): T = lock.withLock { state }

    override fun addStateListener(listener: StateListener<T>): Closeable = lock.withLock {
        stateListeners.add(listener)

        // send current state
        val state = state
        callbackExecutor.execute { listener(state) }

        return@withLock Closeable { removeStateListener(listener) }
    }

    override fun removeStateListener(listener: StateListener<T>): Unit = lock.withLock {
        stateListeners.remove(listener)
    }

    override fun close(): Unit = lock.withLock {
        if (_isClosed) return@withLock
        _isClosed = true

        stateListeners.clear()

        val listeners = closeListeners.toList()
        closeListeners.clear()

        callbackExecutor.execute { listeners.forEach { it() } }
    }

    override fun addCloseListener(listener: CloseListener): Closeable = lock.withLock {
        if (_isClosed) {
            listener()
            return@withLock Closeable { removeCloseListener(listener) }
        }
        closeListeners.add(listener)
        return@withLock Closeable { removeCloseListener(listener) }
    }

    override fun removeCloseListener(listener: CloseListener): Unit = lock.withLock {
        closeListeners.remove(listener)
    }

    private fun flushQueues(): Unit = lock.withLock {
        if (_isClosed) return@withLock
        flushSetStateQueue()
        flushGetStateQueue()
        if (jobs.hasJobs) {
            flushQueues()
        }
    }

    private fun flushSetStateQueue() {
        val reducer = jobs.dequeueSet() ?: return
        val newState = reducer(state)
        setState(newState)
    }

    private fun flushGetStateQueue() {
        val consumer = jobs.dequeueGet() ?: return
        consumer(state)
    }

    private fun setState(state: T) {
        if (this.state == state) return

        // set the new state
        this.state = state

        // capture stateListeners
        val listeners = stateListeners.toList()

        // notify stateListeners
        callbackExecutor.execute { listeners.forEach { it(state) } }
    }

    private class Jobs<T> {

        val hasJobs
            get() = getStateQueue.isNotEmpty() || setStateQueue.isNotEmpty()

        private val getStateQueue = LinkedList<Consumer<T>>()
        private val setStateQueue = LinkedList<Reducer<T>>()

        fun enqueueGet(consumer: Consumer<T>) {
            getStateQueue.add(consumer)
        }

        fun enqueueSet(reducer: Reducer<T>) {
            setStateQueue.add(reducer)
        }

        fun dequeueGet(): Consumer<T>? {
            return getStateQueue.poll()
        }

        fun dequeueSet(): Reducer<T>? {
            return setStateQueue.poll()
        }
    }
}