/**
 * (@moh)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("unused")

package com.techsanelab.screen.utilcmnuse.intents

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import android.content.BroadcastReceiver as StockReceiver
import timber.log.Timber.d as log


class IntentReceivers<T>(
  context: T,
  constructor: IntentFBuilder.() -> Unit
) : LifecycleObserver where T : Context, T : LifecycleOwner {

  private val appContext = context.applicationContext
  private val filter: IntentFilter
  private val instructions: List<Instructions>

  init {
    val builder = IntentFBuilder()
    constructor(builder)
    filter = builder.filter()
    instructions = builder.instructions()

    context.lifecycle.addObserver(this)
  }

  private val broadcastReceiver = object : StockReceiver() {
    override fun onReceive(
      context: Context,
      intent: Intent
    ) {
      log("Received: ${intent.action}")
      for (ins in instructions) {
        if (ins.matches(intent)) {
          log("Intent matches $ins")
          ins.execution()
              .invoke(intent)
          return
        }
      }
      throw IllegalStateException("Shouldn't reach this point")
    }
  }

  @OnLifecycleEvent(ON_RESUME)
  fun start() {
    log("start()")
    appContext.registerReceiver(broadcastReceiver, filter)
  }

  @OnLifecycleEvent(ON_PAUSE)
  fun stop() {
    log("stop()")
    appContext.unregisterReceiver(broadcastReceiver)
  }
}
