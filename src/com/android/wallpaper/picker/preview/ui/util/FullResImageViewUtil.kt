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
package com.android.wallpaper.picker.preview.ui.util

import android.graphics.Point
import android.graphics.PointF
import com.android.wallpaper.util.WallpaperCropUtils

object FullResImageViewUtil {

    private const val DEFAULT_WALLPAPER_MAX_ZOOM = 8f
    fun getScaleAndCenter(
        viewWidth: Int,
        viewHeight: Int,
        offsetToStart: Boolean,
        rawWallpaperSize: Point,
        isSingleDisplayOrUnfoldedHorizontalHinge: Boolean,
        isRtl: Boolean,
    ): ScaleAndCenter {
        // Determine minimum zoom to fit maximum visible area of wallpaper on crop surface.
        val crop = Point(viewWidth, viewHeight)
        val visibleRawWallpaperRect =
            WallpaperCropUtils.calculateVisibleRect(rawWallpaperSize, crop)
        if (offsetToStart && isSingleDisplayOrUnfoldedHorizontalHinge) {
            if (isRtl) {
                visibleRawWallpaperRect.offsetTo(
                    rawWallpaperSize.x - visibleRawWallpaperRect.width(),
                    visibleRawWallpaperRect.top,
                )
            } else {
                visibleRawWallpaperRect.offsetTo(/* newLeft= */ 0, visibleRawWallpaperRect.top)
            }
        }
        val centerPosition =
            PointF(
                visibleRawWallpaperRect.centerX().toFloat(),
                visibleRawWallpaperRect.centerY().toFloat()
            )
        val visibleRawWallpaperSize =
            Point(visibleRawWallpaperRect.width(), visibleRawWallpaperRect.height())
        val defaultWallpaperZoom =
            WallpaperCropUtils.calculateMinZoom(visibleRawWallpaperSize, crop)

        return ScaleAndCenter(
            defaultWallpaperZoom,
            defaultWallpaperZoom.coerceAtLeast(DEFAULT_WALLPAPER_MAX_ZOOM),
            defaultWallpaperZoom,
            centerPosition,
        )
    }

    data class ScaleAndCenter(
        val minScale: Float,
        val maxScale: Float,
        val defaultScale: Float,
        val center: PointF
    )
}
