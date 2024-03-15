/*
 * Copyright 2023 The Android Open Source Project
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

import android.view.View
import android.view.ViewStub
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object PreviewTooltipBinder {
    interface TooltipViewModel {
        val shouldShowTooltip: Flow<Boolean>
        val enableClickToDismiss: Boolean
        fun dismissTooltip()
    }

    fun bind(
        tooltipStub: ViewStub,
        viewModel: TooltipViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        var tooltip: View? = null
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shouldShowTooltip.collect { shouldShowTooltip ->
                        if (shouldShowTooltip && tooltip == null) {
                            tooltip = tooltipStub.inflate()
                            if (viewModel.enableClickToDismiss) {
                                tooltip?.setOnClickListener { viewModel.dismissTooltip() }
                            }
                        }
                        // TODO (b/303318205): animate tooltip
                        // Only show tooltip if it has not been shown before.
                        tooltip?.isVisible = shouldShowTooltip
                    }
                }
            }
        }
    }
}
