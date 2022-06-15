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

package com.techsanelab.screen

import android.app.Application
import com.techsanelab.screen.BuildConfig.DEBUG
import com.techsanelab.screen.utilcmnuse.commonModule
import com.techsanelab.screen.utilcmnuse.prefModule
import com.techsanelab.screen.main.mainModule
import com.techsanelab.screen.main.viewModelModule
import com.techsanelab.screen.capturescrn.engineModule
import com.techsanelab.screen.lng.FabricLng
import com.techsanelab.screen.notifictn.Notifications
import com.techsanelab.screen.notifictn.notificationsModule
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber
import timber.log.Timber.DebugTree


class SRApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (DEBUG) {
            Timber.plant(DebugTree())
        }

        FirebaseAnalytics.getInstance(this);
        Timber.plant(FabricLng())
        MobileAds.initialize(this) {}

        startKoin {
            //            listOf(
//                commonModule,
//                notificationsModule,
//                prefModule,
//                engineModule,
//                mainModule,
//                viewModelModule
//            )
            modules(
                listOf(
                    commonModule,
                    notificationsModule,
                    prefModule,
                    engineModule,
                    mainModule,
                    viewModelModule
                )
            )
            androidLogger()
            androidContext(this@SRApp)

        }

        val notifications by inject<Notifications>()
        notifications.createChannels()
    }

}
