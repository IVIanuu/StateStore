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

internal class RealStateStore<T>(
    initialState: T,
    internal val executor: Executor,
    internal val callbackExecutor: Executor
) : StateStore<T> {

    private var state = initialState

    private val listeners = mutableListOf<StateListener<T>>()

    private val jobs = Jobs<T>()

    override fun withState(consumer: Consumer<T>) {
        synchronized(this) { jobs.enqueueGet(consumer) }
        dispatchFlush()
    }

    override fun setState(reducer: Reducer<T>) {
        synchronized(this) { jobs.enqueueSet(reducer) }
        dispatchFlush()
    }

    override fun peekState(): T = synchronized(this) { state }

    override fun addListener(listener: StateListener<T>): Closeable {
        synchronized(this) { listeners.add(listener) }

        // send current state
        val state = synchronized(this) { state }
        callbackExecutor.execute { listener(state) }

        return Closeable { removeListener(listener) }
    }

    override fun removeListener(listener: StateListener<T>) {
        synchronized(this) { listeners.remove(listener) }
    }

    private fun dispatchFlush() {
        executor.execute { flushQueues() }
    }

    private fun flushQueues() {
        while (synchronized(this) { jobs.hasJobs }) {
            flushSetStateQueue()
            flushGetStateQueue()
        }
    }

    private fun flushSetStateQueue() {
        synchronized(this) {
            val reducer = jobs.dequeueSet() ?: return@flushSetStateQueue
            val newState = reducer(state)
            setState(newState)
        }
    }

    private fun flushGetStateQueue() {
        val consumer = synchronized(this) { jobs.dequeueGet() } ?: return
        val state = synchronized(this) { state }
        consumer(state)
    }

    private fun setState(state: T) {
        if (this.state == state) return

        // set the new state
        this.state = state

        // capture state listeners
        val listeners = listeners.toList()

        // notify state listeners
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