package com.ivianuu.statestore.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.statestore.StateStore
import com.ivianuu.statestore.rx.observable
import com.ivianuu.statestore.rx.onClose
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private val state = StateStore("hello")

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        state.onClose()
            .subscribe { Log.d("testt", "closed thread  ${Thread.currentThread().name}") }
            .let { disposables.add(it) }

        state.observable()
            .subscribe { Log.d("testt", "state changed -> $it, thread ${Thread.currentThread().name}") }
            .let { disposables.add(it) }

        state.withState {
            Log.d("testtt", "with state 1 $it thread ${Thread.currentThread().name}")
        }

        state.setState {
            Log.d("testtt", "set state thread ${Thread.currentThread().name}")
            "$this world"
        }

        state.setState {
            Log.d("testtt", "set state thread ${Thread.currentThread().name}")
            "$this whats up?"
        }

        state.withState {
            Log.d("testtt", "with state 2 $it thread ${Thread.currentThread().name}")
        }
    }

    override fun onDestroy() {
        state.close()
        disposables.clear()
        super.onDestroy()
    }
}
