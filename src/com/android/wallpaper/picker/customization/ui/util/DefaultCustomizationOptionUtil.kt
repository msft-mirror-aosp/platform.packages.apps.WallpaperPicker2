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

package com.android.wallpaper.picker.customization.ui.util

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class DefaultCustomizationOptionUtil @Inject constructor() : CustomizationOptionUtil {

    enum class DefaultLockCustomizationOption : CustomizationOptionUtil.CustomizationOption {
        WALLPAPER,
    }

    enum class DefaultHomeCustomizationOption : CustomizationOptionUtil.CustomizationOption {
        WALLPAPER,
    }

    private var viewMap: Map<CustomizationOptionUtil.CustomizationOption, View>? = null

    override fun initBottomSheetContent(
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater
    ) {
        viewMap = mapOf()
    }

    override fun getBottomSheetContent(option: CustomizationOptionUtil.CustomizationOption): View? {
        return viewMap?.get(option)
    }

    override fun onDestroy() {
        viewMap = null
    }
}