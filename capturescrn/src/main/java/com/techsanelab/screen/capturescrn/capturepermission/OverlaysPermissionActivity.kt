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
package com.techsanelab.screen.capturescrn.capturepermission

import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import androidx.appcompat.app.AppCompatActivity
import com.techsanelab.screen.utilcmnuse.forwords.toUri
import com.techsanelab.screen.utilcmnuse.forwords.toast
import com.techsanelab.screen.utilcmnuse.permissions.PermissionChecker
import com.techsanelab.screen.capturescrn.R
import com.techsanelab.screen.capturescrn.captureservice.ServiceController
import org.koin.android.ext.android.inject


class OverlaysPermissionActivity : AppCompatActivity(), OverlayExplanationCallback {
  companion object {
    const val OVERLAY_REQUEST = 99
  }

  private val serviceController by inject<ServiceController>()
  private val permissionChecker by inject<PermissionChecker>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    OverlayExplanationDialog.show(this)
  }

  override fun onShouldAskForOverlayPermission() {
    val intent = Intent(
        ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:$packageName".toUri()
    )
    startActivityForResult(
        intent,
        OVERLAY_REQUEST
    )
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == OVERLAY_REQUEST) {
      if (!permissionChecker.hasOverlayPermission()) {
        toast(R.string.permission_denied_note_capture)
      } else {
        serviceController.startRecording()
      }
      finish()
    }
  }
}
