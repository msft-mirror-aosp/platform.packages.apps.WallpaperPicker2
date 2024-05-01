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

import android.content.Context
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.model.wallpaper.PreviewPagerPage
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.DualPreviewViewPager
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.DualPreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout.Companion.getViewId
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch

/** Binds dual preview home screen and lock screen view pager. */
object DualPreviewPagerBinder {

    fun bind(
        dualPreviewView: DualPreviewViewPager,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        currentNavDestId: Int,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        navigate: (View) -> Unit,
    ) {
        var transitionDisposableHandle: DisposableHandle? = null
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                if (transitionConfig != null && transition != null) {
                    val listener =
                        object : TransitionListenerAdapter() {
                            override fun onTransitionStart(transition: Transition) {
                                super.onTransitionStart(transition)
                                // Full to small preview return transition is handled by small
                                // preview. Temporarily remove clip to padding to enable the scaled
                                // shared element to display fully.
                                dualPreviewView.clipToPadding = false
                            }

                            override fun onTransitionEnd(transition: Transition) {
                                super.onTransitionEnd(transition)
                                dualPreviewView.clipToPadding = true
                                transition.removeListener(this)
                                transitionDisposableHandle = null
                            }
                        }
                    transition.addListener(listener)
                    transitionDisposableHandle = DisposableHandle {
                        transition.removeListener(listener)
                    }
                }
            }
            // Remove transition listeners on destroy
            transitionDisposableHandle?.dispose()
            transitionDisposableHandle = null
        }
        // implement adapter for the dual preview pager
        dualPreviewView.adapter = DualPreviewPagerAdapter { view, position ->
            // Set tag to allow small to full preview transition to accurately identify view
            view.tag = position

            PreviewTooltipBinder.bindSmallPreviewTooltip(
                tooltipStub = view.requireViewById(R.id.tooltip_stub),
                viewModel = wallpaperPreviewViewModel.smallTooltipViewModel,
                lifecycleOwner = viewLifecycleOwner,
            )

            val dualDisplayAspectRatioLayout: DualDisplayAspectRatioLayout =
                view.requireViewById(R.id.dual_preview)

            val displaySizes =
                mapOf(
                    DeviceDisplayType.FOLDED to wallpaperPreviewViewModel.smallerDisplaySize,
                    DeviceDisplayType.UNFOLDED to
                        wallpaperPreviewViewModel.wallpaperDisplaySize.value,
                )
            dualDisplayAspectRatioLayout.setDisplaySizes(displaySizes)
            dualPreviewView.setDisplaySizes(displaySizes)

            DeviceDisplayType.FOLDABLE_DISPLAY_TYPES.forEach { display ->
                val previewDisplaySize = dualDisplayAspectRatioLayout.getPreviewDisplaySize(display)
                previewDisplaySize?.let {
                    SmallPreviewBinder.bind(
                        applicationContext = applicationContext,
                        view = dualDisplayAspectRatioLayout.requireViewById(display.getViewId()),
                        viewModel = wallpaperPreviewViewModel,
                        viewLifecycleOwner = viewLifecycleOwner,
                        screen = PreviewPagerPage.entries[position].screen,
                        displaySize = it,
                        deviceDisplayType = display,
                        currentNavDestId = currentNavDestId,
                        transition = transition,
                        transitionConfig = transitionConfig,
                        navigate = navigate,
                    )
                }
            }

            dualPreviewView.overScrollMode = OVER_SCROLL_NEVER
        }
    }
}
