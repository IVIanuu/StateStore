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

package com.ivianuu.statestore.coroutines

import com.ivianuu.statestore.StateListener
import com.ivianuu.statestore.StateStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowViaChannel

/**
 * Returns a [Flow] which emits on changes
 */
fun <T> StateStore<T>.asFlow(): Flow<T> = flowViaChannel { channel ->
    val listener: StateListener<T> = { channel.offer(it) }
    addListener(listener)
    //todo Remove when invokeOnClose is no longer experimental, or use replacement.
    channel.invokeOnClose { removeListener(listener) }
}