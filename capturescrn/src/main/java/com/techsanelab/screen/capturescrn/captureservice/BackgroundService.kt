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
package com.techsanelab.screen.capturescrn.captureservice

import android.app.Service
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.hardware.SensorManager
import android.os.IBinder
import android.os.Vibrator
import androidx.lifecycle.LifecycleOwner
import com.techsanelab.screen.utilcmnuse.intents.IntentReceivers
import com.techsanelab.screen.utilcmnuse.forwords.startActivity
import com.techsanelab.screen.utilcmnuse.permissions.PermissionChecker
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_ALWAYS_SHOW_CONTROLS
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_STOP_ON_SCREEN_OFF
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_STOP_ON_SHAKE
import com.techsanelab.screen.utilcmnuse.rxdata.attachLifecycle
import com.techsanelab.screen.capturescrn.captureengine.CaptureEngine
import com.techsanelab.screen.capturescrn.capturegesture.ShakeListenercheck
import com.techsanelab.screen.capturescrn.captureoverlay.OverlayManager
import com.techsanelab.screen.capturescrn.capturepermission.OverlaysPermissionActivity
import com.techsanelab.screen.capturescrn.capturepermission.StoragePermissionActivity
import com.techsanelab.screen.capturescrn.capturerecordings.Recording
import com.techsanelab.screen.capturescrn.capturerecordings.RecordingManager
import com.techsanelab.screen.capturescrn.capturerecordings.RecordingScanner
import com.techsanelab.screen.notifictn.DELETE_ACTION
import com.techsanelab.screen.notifictn.EXIT_ACTION
import com.techsanelab.screen.notifictn.EXTRA_RECORDING
import com.techsanelab.screen.notifictn.EXTRA_STOP_FOREGROUND
import com.techsanelab.screen.notifictn.Notifications
import com.techsanelab.screen.notifictn.RECORD_ACTION
import com.techsanelab.screen.notifictn.STOP_ACTION
import com.afollestad.rxkprefs.Pref
import com.techsanelab.screen.utilcmnuse.lifecyclevA.LifecycleA
import io.reactivex.Observable.merge
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import timber.log.Timber.d as log

/**
 * The background service which foregrounds itself with a persistent notification to do screen
 * capture, even if the app isn't visible.
 *
 * (@moh)
 */
class BackgroundService : Service(), LifecycleOwner {

  companion object {
    private const val ID = 77

    const val PERMISSION_DENIED =
      "com.techsanelab.screen.service.PERMISSION_DENIED"
    const val MAIN_ACTIVITY_CLASS = "main_activity_class"
  }

  private val lifecycle = LifecycleA(this)
  private val overlayManager by inject<OverlayManager>()
  private val notifications by inject<Notifications>()
  private val captureEngine by inject<CaptureEngine>()
  private val recordingScanner by inject<RecordingScanner>()
  private val recordingManager by inject<RecordingManager>()
  private val mainActivityClass by inject<Class<*>>(named(MAIN_ACTIVITY_CLASS))
  private val sensorManager by inject<SensorManager>()
  private val vibrator by inject<Vibrator>()
  private val permissionChecker by inject<PermissionChecker>()

  private val stopOnScreenOffPref by inject<Pref<Boolean>>(named(PREF_STOP_ON_SCREEN_OFF))
  private val alwaysShowNotificationPref by inject<Pref<Boolean>>(
      named(PREF_ALWAYS_SHOW_CONTROLS)
  )
  private val stopOnShakePref by inject<Pref<Boolean>>(named(PREF_STOP_ON_SHAKE))

  private val shakeListener = ShakeListenercheck(sensorManager, vibrator) {
    stopRecording(false)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(
    intent: Intent?,
    flags: Int,
    startId: Int
  ): Int {
    log("onStartCommand(${intent?.action})")

    when (intent?.action) {
      RECORD_ACTION -> {
        startRecording()
      }
      DELETE_ACTION -> {
        val recording: Recording = intent.getParcelableExtra(EXTRA_RECORDING)
        log("Delete: $recording")
        recordingManager.deleteRecording(recording)
        notifications.cancelPostRecordNotification()
        stopForeground(true)
        stopSelf()
      }
      else -> if (!captureEngine.isStarted()) {
        updateForeground(false)
      }
    }
    return START_STICKY
  }

  override fun onCreate() {
    super.onCreate()
    log("onCreate()")

    // Intent broadcasts
    IntentReceivers(this) {
      onAction(PERMISSION_DENIED) {
        captureEngine.cancel()
        updateForeground(false)
      }
      onAction(ACTION_SCREEN_OFF) {
        if (stopOnScreenOffPref.get()) {
          captureEngine.stop()
        }
      }
      onAction(STOP_ACTION) {
        stopRecording(it.getBooleanExtra(EXTRA_STOP_FOREGROUND, false))
      }
      onAction(EXIT_ACTION) {
        captureEngine.cancel()
        stopForeground(true)
        stopSelf()
      }
    }

    lifecycle.onCreate()

    merge(stopOnShakePref.observe(), captureEngine.onStart())
        .subscribe { maybeStartShakeListener() }
        .attachLifecycle(this)

    captureEngine.onStop()
        .subscribe { file ->
          shakeListener.stop()
          updateForeground(false)
          recordingScanner.scan(file) { recording ->
            notifications.showPostRecordNotification(recording, this@BackgroundService::class.java)
          }
        }
        .attachLifecycle(this)

    captureEngine.onError()
        .subscribe { ex -> ErrorDialogActivity.show(this@BackgroundService, ex) }
        .attachLifecycle(this)
  }

  private fun startRecording() {
    if (captureEngine.isStarted() || overlayManager.isCountingDown()) {
      return
    } else if (!permissionChecker.hasStoragePermission()) {
      startActivity<StoragePermissionActivity>()
      return
    } else if (!permissionChecker.hasOverlayPermission() && overlayManager.willCountdown()) {
      startActivity<OverlaysPermissionActivity>()
      return
    }
    overlayManager.countdown {
      captureEngine.start(this@BackgroundService)
      updateForeground(true)
    }
  }

  private fun stopRecording(forceStopForeground: Boolean) {
    captureEngine.stop()
    if (!alwaysShowNotificationPref.get() &&
        (notifications.isAppOpen() || forceStopForeground)
    ) {
      stopForeground(true)
      stopSelf()
    }
  }

  override fun onDestroy() {
    log("onDestroy()")
    shakeListener.stop()
    captureEngine.stop()
    lifecycle.onDestroy()
    super.onDestroy()
  }

  override fun getLifecycle() = lifecycle

  private fun updateForeground(recording: Boolean) {
    val action = if (recording) {
      STOP_ACTION
    } else {
      EXIT_ACTION
    }
    startForeground(
        ID,
        notifications.createWidgetServiceNotification(
            mainActivity = mainActivityClass,
            backgroundService = this::class.java,
            action = action,
            isRecording = recording
        )
    )
  }

  private fun maybeStartShakeListener() {
    if (stopOnShakePref.get() && captureEngine.isStarted()) {
      shakeListener.start()
    } else {
      shakeListener.stop()
    }
  }
}
