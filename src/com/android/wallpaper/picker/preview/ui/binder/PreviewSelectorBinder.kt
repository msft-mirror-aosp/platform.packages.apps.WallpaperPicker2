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
import android.graphics.Point
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.transition.Transition
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.preview.ui.view.PreviewTabs
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel

/** Binds and synchronizes the tab and preview view pagers. */
object PreviewSelectorBinder {

    fun bind(
        tabs: PreviewTabs,
        previewsViewPager: ViewPager2,
        previewDisplaySize: Point,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        currentNavDestId: Int,
        transition: Transition?,
        transitionConfig: FullPreviewConfigViewModel?,
        isFirstBinding: Boolean,
        navigate: (View) -> Unit,
    ) {
        // set up previews view pager
        PreviewPagerBinder.bind(
            applicationContext,
            viewLifecycleOwner,
            previewsViewPager,
            wallpaperPreviewViewModel,
            previewDisplaySize,
            currentNavDestId,
            transition,
            transitionConfig,
            isFirstBinding,
            navigate,
        )

        previewsViewPager.currentItem = if (wallpaperPreviewViewModel.isViewAsHome) 1 else 0
        tabs.setTab(
            if (wallpaperPreviewViewModel.isViewAsHome) Screen.HOME_SCREEN else Screen.LOCK_SCREEN
        )
        synchronizeTabsWithPreviewPager(tabs, previewsViewPager)
    }

    private fun synchronizeTabsWithPreviewPager(
        tabs: PreviewTabs,
        previewsViewPager: ViewPager2,
    ) {
        tabs.setOnTabSelected {
            if (it == Screen.LOCK_SCREEN && previewsViewPager.currentItem != 0) {
                previewsViewPager.setCurrentItem(0, true)
            } else if (it == Screen.HOME_SCREEN && previewsViewPager.currentItem != 1) {
                previewsViewPager.setCurrentItem(1, true)
            }
        }

        previewsViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position == 0) {
                        tabs.transitionToTab(Screen.LOCK_SCREEN)
                    } else if (position == 1) {
                        tabs.transitionToTab(Screen.HOME_SCREEN)
                    }
                }
            }
        )
    }
}
