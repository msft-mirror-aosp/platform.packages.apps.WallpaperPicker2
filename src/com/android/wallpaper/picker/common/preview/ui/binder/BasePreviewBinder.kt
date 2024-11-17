/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Common base preview binder that is only responsible for binding the workspace and wallpaper, and
 * uses the [CustomizationPickerViewModel2].
 */
// Based on SmallPreviewBinder, except cleaned up to only bind bind wallpaper and workspace
// (workspace binding to be added). Also we enable a screen to be defined during binding rather than
// reading from viewModel.isViewAsHome.
// TODO (b/348462236): bind workspace
object BasePreviewBinder {
    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        workspaceCallbackBinder: WorkspaceCallbackBinder,
        screen: Screen,
        deviceDisplayType: DeviceDisplayType,
        displaySize: Point,
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        onLaunchPreview: ((WallpaperModel) -> Unit)? = null,
    ) {
        if (onLaunchPreview != null) {
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch { viewModel.isPreviewClickable.collect { view.isClickable = it } }

                    launch {
                        viewModel.basePreviewViewModel.wallpapers
                            .filterNotNull()
                            .map {
                                if (screen == HOME_SCREEN) it.homeWallpaper
                                else it.lockWallpaper ?: it.homeWallpaper
                            }
                            .collect { wallpaper ->
                                view.setOnClickListener { onLaunchPreview.invoke(wallpaper) }
                            }
                    }
                }
            }
        }

        WallpaperPreviewBinder.bind(
            applicationContext = applicationContext,
            surfaceView = view.requireViewById(R.id.wallpaper_surface),
            viewModel = viewModel.basePreviewViewModel,
            screen = screen,
            displaySize = displaySize,
            deviceDisplayType = deviceDisplayType,
            mainScope = mainScope,
            viewLifecycleOwner = lifecycleOwner,
            wallpaperConnectionUtils = wallpaperConnectionUtils,
            isFirstBindingDeferred = isFirstBindingDeferred,
        )

        WorkspacePreviewBinder.bind(
            surfaceView = view.requireViewById(R.id.workspace_surface),
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            workspaceCallbackBinder = workspaceCallbackBinder,
            screen = screen,
            deviceDisplayType = deviceDisplayType,
            lifecycleOwner = lifecycleOwner,
        )
    }
}
