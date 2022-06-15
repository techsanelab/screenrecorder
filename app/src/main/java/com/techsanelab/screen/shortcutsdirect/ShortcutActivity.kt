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
package com.techsanelab.screen.shortcutsdirect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.techsanelab.screen.capturescrn.captureengine.CaptureEngine
import com.techsanelab.screen.capturescrn.captureservice.ServiceController
import org.koin.android.ext.android.inject

/**
 * A relay for app shortcuts - has no UI, just links to the background service.
 *
 * (@moh)
 */
class ShortcutActivity : AppCompatActivity() {

  private val captureEngine by inject<CaptureEngine>()
  private val serviceController by inject<ServiceController>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (!captureEngine.isStarted()) {
      serviceController.startRecording()
    }
    finish()
  }
}
