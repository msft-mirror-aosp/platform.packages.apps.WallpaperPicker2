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
 *
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.android.wallpaper.picker.option.ui.binder

import android.animation.ValueAnimator
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.picker.common.icon.ui.viewbinder.ContentDescriptionViewBinder
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.option.ui.view.OptionItemBackground
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

object OptionItemBinder2 {
    /**
     * Binds the given [View] to the given [OptionItemViewModel].
     *
     * The child views of [view] must be named and arranged in the following manner, from top of the
     * z-axis to the bottom:
     * - [R.id.foreground] is the foreground drawable ([ImageView]).
     * - [R.id.background] is the view in the background ([OptionItemBackground]).
     *
     * In order to show the animation when an option item is selected, you may need to disable the
     * clipping of child views across the view-tree using:
     * ```
     * android:clipChildren="false"
     * ```
     *
     * Optionally, there may be an [R.id.text] [TextView] to show the text from the view-model. If
     * one is not supplied, the text will be used as the content description of the icon.
     *
     * @param view The view; it must contain the child views described above.
     * @param viewModel The view-model.
     * @param lifecycleOwner The [LifecycleOwner].
     * @param animationfSpec The specification for the animation.
     * @return A [DisposableHandle] that must be invoked when the view is recycled.
     */
    fun bind(
        view: View,
        viewModel: OptionItemViewModel<*>,
        lifecycleOwner: LifecycleOwner,
        animationSpec: AnimationSpec = AnimationSpec(),
    ): DisposableHandle {
        val backgroundView: OptionItemBackground = view.requireViewById(R.id.background)
        val foregroundView: ImageView = view.requireViewById(R.id.foreground)
        val textView: TextView? = view.findViewById(R.id.text)

        if (textView != null && viewModel.isTextUserVisible) {
            TextViewBinder.bind(view = textView, viewModel = viewModel.text)
        } else {
            // Use the text as the content description of the foreground if we don't have a TextView
            // dedicated to for the text.
            ContentDescriptionViewBinder.bind(view = foregroundView, viewModel = viewModel.text)
        }
        textView?.isVisible = viewModel.isTextUserVisible

        textView?.alpha =
            if (viewModel.isEnabled) {
                animationSpec.enabledAlpha
            } else {
                animationSpec.disabledTextAlpha
            }

        backgroundView.alpha =
            if (viewModel.isEnabled) {
                animationSpec.enabledAlpha
            } else {
                animationSpec.disabledBackgroundAlpha
            }

        foregroundView.alpha =
            if (viewModel.isEnabled) {
                animationSpec.enabledAlpha
            } else {
                animationSpec.disabledForegroundAlpha
            }

        view.onLongClickListener =
            if (viewModel.onLongClicked != null) {
                View.OnLongClickListener {
                    viewModel.onLongClicked.invoke()
                    true
                }
            } else {
                null
            }
        view.isLongClickable = viewModel.onLongClicked != null

        val job =
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        // We only want to animate if the view-model is updating in response to a
                        // selection or deselection of the same exact option. For that, we save the
                        // last value of isSelected.
                        var lastSelected: Boolean? = null

                        viewModel.key
                            .flatMapLatest {
                                // If the key changed, then it means that this binding is no longer
                                // rendering the UI for the same option as before, we nullify the
                                // last  selected value to "forget" that we've ever seen a value for
                                // isSelected, effectively starting a new so the first update
                                // doesn't animate.
                                lastSelected = null
                                viewModel.isSelected
                            }
                            .collect { isSelected ->
                                val shouldAnimate =
                                    lastSelected != null && lastSelected != isSelected
                                if (shouldAnimate) {
                                    animatedSelection(
                                        backgroundView = backgroundView,
                                        isSelected = isSelected,
                                        animationSpec = animationSpec,
                                    )
                                } else {
                                    backgroundView.setProgress(if (isSelected) 1f else 0f)
                                }

                                foregroundView.setColorFilter(
                                    if (isSelected) view.context.getColor(R.color.system_on_primary)
                                    else view.context.getColor(R.color.system_on_surface)
                                )

                                view.isSelected = isSelected
                                lastSelected = isSelected
                            }
                    }

                    launch {
                        viewModel.onClicked.collect { onClicked ->
                            view.setOnClickListener(
                                if (onClicked != null) {
                                    View.OnClickListener { onClicked.invoke() }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                }
            }

        return DisposableHandle { job.cancel() }
    }

    private fun animatedSelection(
        backgroundView: OptionItemBackground,
        isSelected: Boolean,
        animationSpec: AnimationSpec,
    ) {
        if (isSelected) {
            val springForce =
                SpringForce().apply {
                    stiffness = SpringForce.STIFFNESS_MEDIUM
                    dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
                    finalPosition = 1f
                }

            SpringAnimation(backgroundView, SpringAnimation.SCALE_X, 1f)
                .apply {
                    setStartVelocity(5f)
                    spring = springForce
                }
                .start()

            SpringAnimation(backgroundView, SpringAnimation.SCALE_Y, 1f)
                .apply {
                    setStartVelocity(5f)
                    spring = springForce
                }
                .start()

            ValueAnimator.ofFloat(0f, 1f)
                .apply {
                    duration = animationSpec.durationMs
                    addUpdateListener {
                        val progress = it.animatedValue as Float
                        backgroundView.setProgress(progress)
                    }
                }
                .start()
        } else {
            ValueAnimator.ofFloat(1f, 0f)
                .apply {
                    duration = animationSpec.durationMs
                    addUpdateListener {
                        val progress = it.animatedValue as Float
                        backgroundView.setProgress(progress)
                    }
                }
                .start()
        }
    }

    data class AnimationSpec(
        /** Opacity of the option when it's enabled. */
        val enabledAlpha: Float = 1f,
        /** Opacity of the option background when it's disabled. */
        val disabledBackgroundAlpha: Float = 0.5f,
        /** Opacity of the option foreground when it's disabled. */
        val disabledForegroundAlpha: Float = 0.5f,
        /** Opacity of the option text when it's disabled. */
        val disabledTextAlpha: Float = 0.61f,
        /** Duration of the animation, in milliseconds. */
        val durationMs: Long = 333L,
    )
}
