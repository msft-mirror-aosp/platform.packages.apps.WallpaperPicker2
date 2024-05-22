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
package com.android.wallpaper.picker.preview.ui.binder

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import androidx.core.view.doOnLayout
import com.android.app.tracing.TraceUtils.trace
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.ui.util.FullResImageViewUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.StaticWallpaperPreviewViewModel
import com.android.wallpaper.util.RtlUtils
import com.android.wallpaper.util.WallpaperCropUtils
import com.android.wallpaper.util.WallpaperSurfaceCallback.LOW_RES_BITMAP_BLUR_RADIUS
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object StaticWallpaperPreviewBinder {

    private val ALPHA_OUT: Interpolator = PathInterpolator(0f, 0f, 0.8f, 1f)
    private const val CROSS_FADE_DURATION: Long = 200

    fun bind(
        lowResImageView: ImageView,
        fullResImageView: SubsamplingScaleImageView,
        viewModel: StaticWallpaperPreviewViewModel,
        displaySize: Point,
        parentCoroutineScope: CoroutineScope,
        isFullScreen: Boolean = false,
    ) {
        lowResImageView.initLowResImageView()
        fullResImageView.initFullResImageView()

        parentCoroutineScope.launch {
            launch { viewModel.lowResBitmap.collect { lowResImageView.setImageBitmap(it) } }

            launch {
                viewModel.subsamplingScaleImageViewModel.collect { imageModel ->
                    trace(TAG) {
                        val cropHint = imageModel.fullPreviewCropModels?.get(displaySize)?.cropHint
                        fullResImageView.setFullResImage(
                            ImageSource.cachedBitmap(imageModel.rawWallpaperBitmap),
                            imageModel.rawWallpaperSize,
                            displaySize,
                            cropHint,
                            RtlUtils.isRtl(lowResImageView.context),
                            isFullScreen,
                        )

                        // Fill in the default crop region if the displaySize for this preview
                        // is missing.
                        viewModel.updateDefaultPreviewCropModel(
                            displaySize,
                            FullPreviewCropModel(
                                cropHint =
                                    WallpaperCropUtils.calculateVisibleRect(
                                        imageModel.rawWallpaperSize,
                                        Point(
                                            fullResImageView.measuredWidth,
                                            fullResImageView.measuredHeight
                                        )
                                    ),
                                cropSizeModel = null,
                            ),
                        )

                        crossFadeInFullResImageView(lowResImageView, fullResImageView)
                    }
                }
            }
        }
    }

    private fun ImageView.initLowResImageView() {
        setRenderEffect(
            RenderEffect.createBlurEffect(
                LOW_RES_BITMAP_BLUR_RADIUS,
                LOW_RES_BITMAP_BLUR_RADIUS,
                Shader.TileMode.CLAMP
            )
        )
    }

    private fun SubsamplingScaleImageView.initFullResImageView() {
        setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM)
        setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
    }

    private fun SubsamplingScaleImageView.setFullResImage(
        imageSource: ImageSource,
        rawWallpaperSize: Point,
        displaySize: Point,
        cropHint: Rect?,
        isRtl: Boolean,
        isFullScreen: Boolean,
    ) {
        // Set the full res image
        setImage(imageSource)
        // Calculate the scale and the center point for the full res image
        doOnLayout {
            FullResImageViewUtil.getScaleAndCenter(
                    Point(measuredWidth, measuredHeight),
                    rawWallpaperSize,
                    displaySize,
                    cropHint,
                    isRtl,
                )
                .let { scaleAndCenter ->
                    // For full screen, the preview image container size has already been adjusted
                    // to preserve a boundary beyond the visible crop per comment at
                    // FullWallpaperPreviewBinder#adjustSizesForCropping.
                    // For small screen preview, we need to apply additional scaling since the
                    // container is the same size as the preview.
                    val scale =
                        if (isFullScreen) 1f
                        else
                            WallpaperCropUtils.getSystemWallpaperMaximumScale(
                                context.applicationContext
                            )
                    minScale = scaleAndCenter.minScale * scale
                    maxScale = scaleAndCenter.maxScale
                    setScaleAndCenter(scaleAndCenter.defaultScale * scale, scaleAndCenter.center)
                }
        }
    }

    private fun crossFadeInFullResImageView(lowResImageView: ImageView, fullResImageView: View) {
        fullResImageView.alpha = 0f
        fullResImageView
            .animate()
            .alpha(1f)
            .setInterpolator(ALPHA_OUT)
            .setDuration(CROSS_FADE_DURATION)
            .setListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        lowResImageView.setImageBitmap(null)
                    }
                }
            )
    }

    private const val TAG = "StaticWallpaperPreviewBinder"
}
