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

package com.android.wallpaper.picker.preview.domain.interactor

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.android.wallpaper.asset.Asset
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@ActivityRetainedScoped
class WallpaperPreviewInteractor
@Inject
constructor(
    private val wallpaperPreviewRepository: WallpaperPreviewRepository,
    private val wallpaperRepository: WallpaperRepository,
) {
    val wallpaperModel: StateFlow<WallpaperModel?> = wallpaperPreviewRepository.wallpaperModel

    val hasSmallPreviewTooltipBeenShown: StateFlow<Boolean> =
        wallpaperPreviewRepository.hasSmallPreviewTooltipBeenShown
    fun hideSmallPreviewTooltip() = wallpaperPreviewRepository.hideSmallPreviewTooltip()

    val hasFullPreviewTooltipBeenShown: StateFlow<Boolean> =
        wallpaperPreviewRepository.hasFullPreviewTooltipBeenShown
    fun hideFullPreviewTooltip() = wallpaperPreviewRepository.hideFullPreviewTooltip()

    suspend fun setStaticWallpaper(
        @UserEventLogger.SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: StaticWallpaperModel,
        bitmap: Bitmap,
        wallpaperSize: Point,
        asset: Asset,
        fullPreviewCropModels: Map<Point, FullPreviewCropModel>? = null,
    ) {
        wallpaperRepository.setStaticWallpaper(
            setWallpaperEntryPoint,
            destination,
            wallpaperModel,
            bitmap,
            wallpaperSize,
            asset,
            fullPreviewCropModels,
        )
    }

    suspend fun setLiveWallpaper(
        @UserEventLogger.SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: WallpaperModel.LiveWallpaperModel,
    ) {
        wallpaperRepository.setLiveWallpaper(
            setWallpaperEntryPoint,
            destination,
            wallpaperModel,
        )
    }

    suspend fun getWallpaperColors(bitmap: Bitmap, cropHints: Map<Point, Rect>?): WallpaperColors? =
        wallpaperRepository.getWallpaperColors(bitmap, cropHints)
}
