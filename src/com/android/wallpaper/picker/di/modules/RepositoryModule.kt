/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.wallpaper.picker.di.modules

import android.app.WallpaperManager
import android.content.Context
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.picker.customization.data.content.WallpaperClient
import com.android.wallpaper.picker.customization.data.content.WallpaperClientImpl
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

@InstallIn(SingletonComponent::class)
@Module
internal object RepositoryModule {

    @Provides
    @Singleton
    fun provideWallpaperRepository(
        @BackgroundDispatcher bgDispatcher: CoroutineDispatcher,
        @MainDispatcher mainScope: CoroutineScope,
        wallpaperPreferences: WallpaperPreferences,
        wallpaperClient: WallpaperClient,
    ): WallpaperRepository {
        return WallpaperRepository(
            mainScope,
            wallpaperClient,
            wallpaperPreferences,
            bgDispatcher,
        )
    }

    @Provides
    fun provideWallpaperClient(
        @ApplicationContext appContext: Context,
        wallpaperManager: WallpaperManager,
        wallpaperPreferences: WallpaperPreferences,
    ): WallpaperClient {
        return WallpaperClientImpl(
            appContext,
            wallpaperManager,
            wallpaperPreferences,
        )
    }
}
