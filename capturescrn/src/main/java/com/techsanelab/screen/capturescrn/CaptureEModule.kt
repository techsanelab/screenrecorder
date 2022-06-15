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
package com.techsanelab.screen.capturescrn

import android.app.Application
import android.content.Context
import android.content.Context.MEDIA_PROJECTION_SERVICE
import android.content.Context.SENSOR_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.hardware.SensorManager
import android.media.projection.MediaProjectionManager
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import com.techsanelab.screen.utilcmnuse.forwords.systemService
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_AUDIO_BIT_RATE
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_COUNTDOWN
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_FRAME_RATE
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_RECORDINGS_FOLDER
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_RECORD_AUDIO
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_RESOLUTION_HEIGHT
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_RESOLUTION_WIDTH
import com.techsanelab.screen.utilcmnuse.prefrencies.PrefNames.PREF_VIDEO_BIT_RATE
import com.techsanelab.screen.capturescrn.captureengine.CaptureEngine
import com.techsanelab.screen.capturescrn.captureengine.RealCaptureEngine
import com.techsanelab.screen.capturescrn.captureoverlay.OverlayManager
import com.techsanelab.screen.capturescrn.captureoverlay.RealOverlayManager
import com.techsanelab.screen.capturescrn.capturerecordings.RealRecordingManager
import com.techsanelab.screen.capturescrn.capturerecordings.RealRecordingScanner
import com.techsanelab.screen.capturescrn.capturerecordings.RecordingManager
import com.techsanelab.screen.capturescrn.capturerecordings.RecordingScanner
import com.techsanelab.screen.capturescrn.captureservice.RealServiceController
import com.techsanelab.screen.capturescrn.captureservice.ServiceController
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module


val engineModule = module {

  factory<LayoutInflater> {
    val themedContext = ContextThemeWrapper(get(), R.style.AppTheme)
    LayoutInflater.from(themedContext)
  }

  factory<WindowManager> {
    get<Application>().systemService(Context.WINDOW_SERVICE)
  }

  single<MediaProjectionManager> {
    get<Application>().systemService(MEDIA_PROJECTION_SERVICE)
  }

  factory<SensorManager> { get<Application>().systemService(SENSOR_SERVICE) }

  factory<Vibrator> { get<Application>().systemService(VIBRATOR_SERVICE) }

  factory {
    RealRecordingManager(get(), get(named(PREF_RECORDINGS_FOLDER)))
  } bind RecordingManager::class

  single { RealRecordingScanner(get(), get()) } bind RecordingScanner::class

  single {
    RealCaptureEngine(
        get(),
        get(),
        get(named(PREF_RECORDINGS_FOLDER)),
        get(named(PREF_VIDEO_BIT_RATE)),
        get(named(PREF_FRAME_RATE)),
        get(named(PREF_RECORD_AUDIO)),
        get(named(PREF_AUDIO_BIT_RATE)),
        get(named(PREF_RESOLUTION_WIDTH)),
        get(named(PREF_RESOLUTION_HEIGHT))
    )
  } bind CaptureEngine::class

  factory {
    RealOverlayManager(get(), get(), get(named(PREF_COUNTDOWN)), get())
  } bind OverlayManager::class

  factory { RealServiceController(get()) } bind ServiceController::class
}
