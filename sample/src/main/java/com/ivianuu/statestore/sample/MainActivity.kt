package com.ivianuu.statestore.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.statestore.StateStore
import com.ivianuu.statestore.rx.observable
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private val state = StateStore("hello")

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        state.observable()
            .subscribe { Log.d("testt", "state changed -> $it, thread ${Thread.currentThread().name}") }
            .let { disposables.add(it) }

        state.withState {
            Log.d("testtt", "with state 1 $it thread ${Thread.currentThread().name}")
        }

        state.setState {
            Log.d("testtt", "set state 1 thread ${Thread.currentThread().name}")
            "$this world"
        }

        state.setState {
            Log.d("testtt", "set state 2 thread ${Thread.currentThread().name}")
            "$this whats up?"
        }

        state.withState {
            Log.d("testtt", "with state 2 $it thread ${Thread.currentThread().name}")
        }
    }

    override fun onDestroy() {
        disposables.clear()
        super.onDestroy()
    }
}
