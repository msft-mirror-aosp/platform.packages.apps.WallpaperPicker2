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

package com.android.wallpaper.picker.preview.ui.binder

import android.content.Context
import android.graphics.Point
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.preview.shared.model.SmallPreviewPagerStateModel
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.wallpaperconnection.WallpaperConnectionUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SmallPreviewScreenBinder {
    fun bind(
        applicationContext: Context,
        mainScope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        fragmentLayout: MotionLayout,
        viewModel: WallpaperPreviewViewModel,
        previewDisplaySize: Point,
        currentNavDestId: Int,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        wallpaperConnectionUtils: WallpaperConnectionUtils,
        isFirstBindingDeferred: CompletableDeferred<Boolean>,
        onBackPressedCallback: OnBackPressedCallback,
        navigate: (View) -> Unit,
    ) {
        val previewPager = fragmentLayout.requireViewById<MotionLayout>(R.id.preview_pager)
        val smallPreview =
            fragmentLayout.requireViewById<MotionLayout>(R.id.small_preview_container)

        PreviewPagerBinder2.bind(
            applicationContext,
            mainScope = mainScope,
            lifecycleOwner,
            smallPreview,
            viewModel,
            previewDisplaySize,
            currentNavDestId,
            transition,
            transitionConfig,
            wallpaperConnectionUtils,
            isFirstBindingDeferred,
            navigate,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.smallPreviewPagerState.collect {
                        viewModel.handleSmallPreviewBackPressed(false)

                        when (it) {
                            SmallPreviewPagerStateModel.TRANS_TO_APPLY_WALLPAPER_SCREEN -> {
                                previewPager.transitionToState(R.id.apply_wallpaper_all)
                            }
                            null -> {
                                val initialTab =
                                    if (viewModel.getSmallPreviewTabIndex() == 0)
                                        R.id.lock_preview_selected
                                    else R.id.home_preview_selected
                                previewPager.transitionToState(initialTab)
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.applyWallpaperBackPressedScreen.collect {
                        val selectedTab =
                            if (it == Screen.LOCK_SCREEN) R.id.lock_preview_selected
                            else R.id.home_preview_selected
                        previewPager.transitionToState(selectedTab)
                        viewModel.dismissSetWallpaperDialog()
                        smallPreview.transitionToState(
                            if (viewModel.previewActionsViewModel.isAnyActionChecked()) {
                                R.id.floating_sheet_visible
                            } else {
                                R.id.floating_sheet_gone
                            }
                        )
                        fragmentLayout.transitionToState(R.id.show_full_fragment)
                    }
                }
            }
        }
    }
}
