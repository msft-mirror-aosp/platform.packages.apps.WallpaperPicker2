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
import android.view.SurfaceView
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.preview.ui.fragment.SmallPreviewFragment
import com.android.wallpaper.picker.preview.ui.viewmodel.FullPreviewConfigViewModel
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.launch

object SmallPreviewBinder {

    fun bind(
        applicationContext: Context,
        view: View,
        viewModel: WallpaperPreviewViewModel,
        screen: Screen,
        displaySize: Point,
        deviceDisplayType: DeviceDisplayType,
        viewLifecycleOwner: LifecycleOwner,
        currentNavDestId: Int,
        navigate: ((View) -> Unit)? = null,
        transition: Transition? = null,
        transitionConfig: FullPreviewConfigViewModel? = null,
        isFirstBinding: Boolean,
    ) {

        val previewCard: CardView = view.requireViewById(R.id.preview_card)
        val descriptionString = previewCard.contentDescription

        val updatedDescription =
            when (deviceDisplayType) {
                DeviceDisplayType.FOLDED -> {
                    val foldedStateDescription =
                        view.context.getString(R.string.folded_device_state_description)
                    "$descriptionString $foldedStateDescription"
                }
                DeviceDisplayType.UNFOLDED -> {
                    val unfoldedStateDescription =
                        view.context.getString(R.string.unfolded_device_state_description)
                    "$descriptionString $unfoldedStateDescription"
                }
                else -> descriptionString
            }
        previewCard.contentDescription = updatedDescription
        val wallpaperSurface: SurfaceView = view.requireViewById(R.id.wallpaper_surface)
        val workspaceSurface: SurfaceView = view.requireViewById(R.id.workspace_surface)
        var transitionDisposableHandle: DisposableHandle? = null

        // Set transition names to enable the small to full preview enter and return shared
        // element transitions.
        val transitionName =
            when (screen) {
                Screen.LOCK_SCREEN ->
                    when (deviceDisplayType) {
                        DeviceDisplayType.SINGLE ->
                            SmallPreviewFragment.SMALL_PREVIEW_LOCK_SHARED_ELEMENT_ID
                        DeviceDisplayType.FOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_LOCK_FOLDED_SHARED_ELEMENT_ID
                        DeviceDisplayType.UNFOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_LOCK_UNFOLDED_SHARED_ELEMENT_ID
                    }
                Screen.HOME_SCREEN ->
                    when (deviceDisplayType) {
                        DeviceDisplayType.SINGLE ->
                            SmallPreviewFragment.SMALL_PREVIEW_HOME_SHARED_ELEMENT_ID
                        DeviceDisplayType.FOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_HOME_FOLDED_SHARED_ELEMENT_ID
                        DeviceDisplayType.UNFOLDED ->
                            SmallPreviewFragment.SMALL_PREVIEW_HOME_UNFOLDED_SHARED_ELEMENT_ID
                    }
            }
        ViewCompat.setTransitionName(previewCard, transitionName)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                // All surface views are initially hidden in the XML to enable smoother
                // transitions. Only show the surface view used in the shared element transition
                // until the transition ends to avoid issues with multiple surface views
                // overlapping.
                if (transition == null || transitionConfig == null) {
                    // If no enter or re-enter transition, show child surfaces.
                    wallpaperSurface.isVisible = true
                    workspaceSurface.isVisible = true
                } else {
                    if (
                        transitionConfig.screen == screen &&
                            transitionConfig.deviceDisplayType == deviceDisplayType
                    ) {
                        // If transitioning to the current small preview, show child surfaces when
                        // transition starts.
                        val listener =
                            object : TransitionListenerAdapter() {
                                override fun onTransitionStart(transition: Transition) {
                                    super.onTransitionStart(transition)
                                    wallpaperSurface.isVisible = true
                                    workspaceSurface.isVisible = true
                                    transition.removeListener(this)
                                    transitionDisposableHandle = null
                                }
                            }
                        transition.addListener(listener)
                        transitionDisposableHandle = DisposableHandle {
                            transition.removeListener(listener)
                        }
                    } else {
                        // If transitioning to another small preview, keep child surfaces hidden
                        // until transition ends.
                        val listener =
                            object : TransitionListenerAdapter() {
                                override fun onTransitionEnd(transition: Transition) {
                                    super.onTransitionEnd(transition)
                                    wallpaperSurface.isVisible = true
                                    workspaceSurface.isVisible = true
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

                if (R.id.smallPreviewFragment == currentNavDestId) {
                    viewModel.isSmallPreviewClickable.collect {
                        if (it) {
                            view.setOnClickListener {
                                viewModel.onSmallPreviewClicked(screen, deviceDisplayType)
                                navigate?.invoke(previewCard)
                            }
                        } else {
                            view.setOnClickListener(null)
                        }
                    }
                } else if (R.id.setWallpaperDialog == currentNavDestId) {
                    previewCard.radius =
                        previewCard.resources.getDimension(
                            R.dimen.set_wallpaper_dialog_preview_corner_radius
                        )
                }
            }
            // Remove transition listeners on destroy
            transitionDisposableHandle?.dispose()
            transitionDisposableHandle = null
            // Remove on click listener when on destroyed
            view.setOnClickListener(null)
        }

        val config = viewModel.getWorkspacePreviewConfig(screen, deviceDisplayType)
        WorkspacePreviewBinder.bind(
            workspaceSurface,
            config,
            viewModel,
            viewLifecycleOwner,
        )

        SmallWallpaperPreviewBinder.bind(
            surface = wallpaperSurface,
            viewModel = viewModel,
            displaySize = displaySize,
            applicationContext = applicationContext,
            viewLifecycleOwner = viewLifecycleOwner,
            deviceDisplayType = deviceDisplayType,
            isFirstBinding = isFirstBinding,
        )
    }
}
