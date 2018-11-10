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

import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class RealStateStore<T>(
    initialState: T,
    private val executor: Executor,
    private val callbackExecutor: Executor
) : StateStore<T> {

    private var state = initialState

    private var closed = false

    private val stateListeners = mutableListOf<((T) -> Unit)>()
    private val closeListeners = mutableListOf<() -> Unit>()

    private val lock = ReentrantLock()

    private val jobs = Jobs<T>()

    override fun withState(consumer: (T) -> Unit): Unit = lock.withLock {
        if (closed) return@withLock

        jobs.enqueueGetStateBlock(consumer)
        executor.execute { flushQueues() }
    }

    override fun setState(reducer: T.() -> T): Unit = lock.withLock {
        if (closed) return@withLock

        jobs.enqueueSetStateBlock(reducer)
        executor.execute { flushQueues() }
    }

    override fun peekState() = lock.withLock { state }

    override fun addStateListener(listener: (T) -> Unit): Unit = lock.withLock {
        stateListeners.add(listener)

        // send current state
        val state = state
        callbackExecutor.execute { listener(state) }
    }

    override fun removeStateListener(listener: (T) -> Unit): Unit = lock.withLock {
        stateListeners.remove(listener)
    }

    override fun close(): Unit = lock.withLock {
        if (closed) return@withLock

        stateListeners.clear()

        val listeners = closeListeners.toList()
        closeListeners.clear()

        (executor as? SingleThreadExecutor)?.close() // todo generify

        closed = true

        callbackExecutor.execute { listeners.forEach { it() } }
    }

    override fun addCloseListener(listener: () -> Unit): Unit = lock.withLock {
        if (closed) {
            listener()
            return@withLock
        }

        closeListeners.add(listener)
    }

    override fun removeCloseListener(listener: () -> Unit): Unit = lock.withLock {
        closeListeners.remove(listener)
    }

    private fun flushQueues(): Unit = lock.withLock {
        if (closed) return@withLock

        flushSetStateQueue()
        val block = jobs.dequeueGetStateBlock() ?: return
        block(state)

        flushQueues()
    }

    private fun flushSetStateQueue() {
        val blocks = jobs.dequeueAllSetStateBlocks() ?: return

        blocks
            .fold(state) { state, reducer -> state.reducer() }
            .also { setState(it) }
    }

    private fun setState(state: T) {
        // set the new state
        this.state = state

        // capture listeners
        val listeners = stateListeners.toList()

        // notify listeners
        callbackExecutor.execute {
            listeners.forEach { it(state) }
        }
    }

    private class Jobs<S> {

        private val getStateQueue = LinkedList<(state: S) -> Unit>()
        private var setStateQueue = LinkedList<S.() -> S>()

        fun enqueueGetStateBlock(block: (state: S) -> Unit) {
            getStateQueue.add(block)
        }

        fun enqueueSetStateBlock(block: S.() -> S) {
            setStateQueue.add(block)
        }

        fun dequeueGetStateBlock(): ((state: S) -> Unit)? {
            if (getStateQueue.isEmpty()) return null
            return getStateQueue.removeFirst()
        }

        fun dequeueAllSetStateBlocks(): List<(S.() -> S)>? {
            // do not allocate empty queue for no-op flushes
            if (setStateQueue.isEmpty()) return null

            val queue = setStateQueue
            setStateQueue = LinkedList()
            return queue
        }
    }
}