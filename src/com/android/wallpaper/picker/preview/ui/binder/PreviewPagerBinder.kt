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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import androidx.lifecycle.LifecycleOwner
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.getScreenOrientation
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.SinglePreviewPagerAdapter
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.adapters.SinglePreviewPagerAdapter.Companion.LOCK_PREVIEW_POSITION
import com.android.wallpaper.picker.preview.ui.fragment.smallpreview.pagetransformers.PreviewCardPageTransformer
import com.android.wallpaper.picker.preview.ui.viewmodel.SmallPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import com.android.wallpaper.util.PreviewUtils
import kotlinx.coroutines.CoroutineScope

/** Binds single preview home screen and lock screen tabs view pager. */
object PreviewPagerBinder {

    @SuppressLint("WrongConstant")
    fun bind(
        applicationContext: Context,
        viewLifecycleOwner: LifecycleOwner,
        mainScope: CoroutineScope,
        previewsViewPager: ViewPager2,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        previewDisplaySize: Point,
        homePreviewUtils: PreviewUtils,
        lockPreviewUtils: PreviewUtils,
        navigate: (() -> Unit)? = null,
    ) {
        previewsViewPager.apply {
            adapter = SinglePreviewPagerAdapter { viewHolder, position ->
                SmallPreviewBinder.bind(
                    applicationContext = applicationContext,
                    view = viewHolder.itemView.requireViewById(R.id.preview),
                    viewModel = wallpaperPreviewViewModel,
                    smallPreviewConfig =
                        SmallPreviewConfigViewModel(
                            previewTab =
                                if (position == LOCK_PREVIEW_POSITION) Screen.LOCK_SCREEN
                                else Screen.HOME_SCREEN,
                            displaySize = previewDisplaySize,
                            screenOrientation = getScreenOrientation(previewDisplaySize),
                        ),
                    mainScope = mainScope,
                    viewLifecycleOwner = viewLifecycleOwner,
                    previewUtils =
                        if (position == LOCK_PREVIEW_POSITION) lockPreviewUtils
                        else homePreviewUtils,
                    navigate = navigate,
                )
            }
            offscreenPageLimit = SinglePreviewPagerAdapter.PREVIEW_PAGER_ITEM_COUNT
            clipChildren = false
            clipToPadding = false
            setPageTransformer(PreviewCardPageTransformer(previewDisplaySize))
        }
    }
}
