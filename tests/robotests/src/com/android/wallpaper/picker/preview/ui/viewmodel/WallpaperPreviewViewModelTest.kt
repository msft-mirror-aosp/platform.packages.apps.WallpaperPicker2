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

package com.android.wallpaper.picker.preview.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.net.Uri
import android.service.wallpaper.WallpaperService
import androidx.activity.viewModels
import androidx.test.core.app.ActivityScenario
import com.android.wallpaper.effects.FakeEffectsController
import com.android.wallpaper.model.wallpaper.DeviceDisplayType
import com.android.wallpaper.module.CustomizationSections
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.HomeScreenPreviewUtils
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.LockScreenPreviewUtils
import com.android.wallpaper.picker.preview.PreviewTestActivity
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository.EffectStatus
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import com.android.wallpaper.picker.preview.data.util.FakeLiveWallpaperDownloader
import com.android.wallpaper.picker.preview.shared.model.FullPreviewCropModel
import com.android.wallpaper.picker.preview.shared.model.ImageEffectsModel
import com.android.wallpaper.testing.FakeContentProvider
import com.android.wallpaper.testing.FakeDisplaysProvider
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.FOLDABLE_UNFOLDED_LAND
import com.android.wallpaper.testing.FakeDisplaysProvider.Companion.HANDHELD
import com.android.wallpaper.testing.FakeImageEffectsRepository
import com.android.wallpaper.testing.FakeWallpaperClient
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestWallpaperPreferences
import com.android.wallpaper.testing.WallpaperModelUtils
import com.android.wallpaper.testing.collectLastValue
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import com.google.common.truth.Truth.assertThat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowContentResolver

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WallpaperPreviewViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private val staticWallpaperModel =
        WallpaperModelUtils.getStaticWallpaperModel(
            wallpaperId = "testWallpaperId",
            collectionId = "testCollection",
            imageWallpaperUri = Uri.parse("content://com.test/image")
        )

    private lateinit var scenario: ActivityScenario<PreviewTestActivity>
    private lateinit var wallpaperPreviewViewModel: WallpaperPreviewViewModel
    private lateinit var staticWallpapaperPreviewViewModel: StaticWallpaperPreviewViewModel
    private lateinit var wallpaperPreviewRepository: WallpaperPreviewRepository
    @HomeScreenPreviewUtils private lateinit var homePreviewUtils: PreviewUtils
    @LockScreenPreviewUtils private lateinit var lockPreviewUtils: PreviewUtils

    @Inject @ApplicationContext lateinit var appContext: Context
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var testInjector: TestInjector
    @Inject lateinit var wallpaperDownloader: FakeLiveWallpaperDownloader
    @Inject lateinit var wallpaperPreferences: TestWallpaperPreferences
    @Inject lateinit var wallpaperClient: FakeWallpaperClient
    @Inject lateinit var contentProvider: FakeContentProvider
    @Inject lateinit var imageEffectsRepository: FakeImageEffectsRepository
    @Inject lateinit var displaysProvider: FakeDisplaysProvider

    @Before
    fun setUp() {
        hiltRule.inject()

        InjectorProvider.setInjector(testInjector)
        Dispatchers.setMain(testDispatcher)

        // Register our testing activity since it's not in the manifest
        val pm = Shadows.shadowOf(appContext.packageManager)
        val activityInfo =
            ActivityInfo().apply {
                name = PreviewTestActivity::class.java.name
                packageName = appContext.packageName
            }
        pm.addOrUpdateActivity(activityInfo)

        // Register a fake the content provider
        ShadowContentResolver.registerProviderInternal(
            FakeEffectsController.AUTHORITY,
            contentProvider,
        )

        // Provide resolution info for our fake content provider
        val packageName = FakeEffectsController.LIVE_WALLPAPER_COMPONENT_PKG_NAME
        val className = FakeEffectsController.LIVE_WALLPAPER_COMPONENT_CLASS_NAME
        val resolveInfo =
            ResolveInfo().apply {
                serviceInfo = ServiceInfo()
                serviceInfo.packageName = packageName
                serviceInfo.splitName = "effectsWallpaper"
                serviceInfo.name = className
                serviceInfo.flags = PackageManager.GET_META_DATA
            }
        val intent = Intent(WallpaperService.SERVICE_INTERFACE).setClassName(packageName, className)
        pm.addResolveInfoForIntent(intent, resolveInfo)

        // Launch the test activity to set remaining test objects
        scenario = ActivityScenario.launch(PreviewTestActivity::class.java)
        scenario.onActivity { setEverything(it) }
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface ActivityScopeEntryPoint {
        @HomeScreenPreviewUtils fun homePreviewUtils(): PreviewUtils
        @LockScreenPreviewUtils fun lockPreviewUtils(): PreviewUtils
        fun wallpaperPreviewRepository(): WallpaperPreviewRepository
    }

    private fun setEverything(activity: PreviewTestActivity) {
        val activityScopeEntryPoint =
            EntryPointAccessors.fromActivity(activity, ActivityScopeEntryPoint::class.java)
        homePreviewUtils = activityScopeEntryPoint.homePreviewUtils()
        lockPreviewUtils = activityScopeEntryPoint.lockPreviewUtils()
        wallpaperPreviewRepository = activityScopeEntryPoint.wallpaperPreviewRepository()

        wallpaperPreviewViewModel = activity.viewModels<WallpaperPreviewViewModel>().value
        staticWallpapaperPreviewViewModel =
            wallpaperPreviewViewModel.staticWallpaperPreviewViewModel
    }

    @Test
    fun smallPreviewClickable_byDefault() =
        testScope.runTest {
            val isClickable = collectLastValue(wallpaperPreviewViewModel.isSmallPreviewClickable)()

            assertThat(isClickable).isTrue()
        }

    @Test
    fun smallPreviewNotClickable_whenEffectInProgress() =
        testScope.runTest {
            imageEffectsRepository.imageEffectsModel.value =
                ImageEffectsModel(EffectStatus.EFFECT_APPLY_IN_PROGRESS)

            val isClickable = collectLastValue(wallpaperPreviewViewModel.isSmallPreviewClickable)()

            assertThat(isClickable).isFalse()
        }

    @Test
    fun clickSmallPreview_updatesFullWorkspacePreviewConfig() =
        testScope.runTest {
            wallpaperPreviewViewModel.onSmallPreviewClicked(
                CustomizationSections.Screen.HOME_SCREEN,
                DeviceDisplayType.UNFOLDED
            )

            val config =
                collectLastValue(wallpaperPreviewViewModel.fullWorkspacePreviewConfigViewModel)()

            assertThat(config?.deviceDisplayType).isEqualTo(DeviceDisplayType.UNFOLDED)
            assertThat(config?.previewUtils).isEqualTo(homePreviewUtils)
        }

    @Test
    fun clickSmallPreview_updatesFullWallpaperPreviewConfig() =
        testScope.runTest {
            val model = WallpaperModelUtils.getStaticWallpaperModel("testId", "testCollection")
            updateFullWallpaperFlow(
                model,
                WhichPreview.PREVIEW_CURRENT,
                listOf(HANDHELD),
            )

            wallpaperPreviewViewModel.onSmallPreviewClicked(
                CustomizationSections.Screen.LOCK_SCREEN,
                DeviceDisplayType.SINGLE
            )

            val fullWallpaper = collectLastValue(wallpaperPreviewViewModel.fullWallpaper)()
            assertThat(fullWallpaper).isNotNull()
            fullWallpaper?.run {
                assertThat(config.deviceDisplayType).isEqualTo(DeviceDisplayType.SINGLE)
                assertThat(config.screen).isEqualTo(CustomizationSections.Screen.LOCK_SCREEN)
                assertThat(wallpaper).isEqualTo(model)
                assertThat(displaySize).isEqualTo(HANDHELD.displaySize)
                assertThat(allowUserCropping).isTrue()
                assertThat(whichPreview).isEqualTo(WhichPreview.PREVIEW_CURRENT)
            }
        }

    @Test
    fun clickCropButton_updatesCropHintsInfo() =
        testScope.runTest {
            val newCropRect = Rect(10, 10, 10, 10)
            val model =
                WallpaperModelUtils.getStaticWallpaperModel(
                    wallpaperId = "testId",
                    collectionId = "testCollection",
                )
            updateFullWallpaperFlow(
                model,
                WhichPreview.PREVIEW_CURRENT,
                listOf(FOLDABLE_UNFOLDED_LAND),
            )
            wallpaperPreviewViewModel.setDefaultFullPreviewConfigViewModel(
                DeviceDisplayType.UNFOLDED
            )

            // Set a crop and confirm via clicking button
            wallpaperPreviewViewModel.staticWallpaperPreviewViewModel.fullPreviewCropModels[
                    FOLDABLE_UNFOLDED_LAND.displaySize] =
                FullPreviewCropModel(
                    cropHint = newCropRect,
                    cropSizeModel = null,
                )
            collectLastValue(wallpaperPreviewViewModel.onCropButtonClick)()?.invoke()

            val cropHintsInfo =
                wallpaperPreviewViewModel.staticWallpaperPreviewViewModel.cropHintsInfo.value
            assertThat(cropHintsInfo).containsKey(FOLDABLE_UNFOLDED_LAND.displaySize)
            assertThat(cropHintsInfo?.get(FOLDABLE_UNFOLDED_LAND.displaySize)?.cropHint)
                .isEqualTo(newCropRect)
        }

    /**
     * Updates all upstream flows of [WallpaperPreviewViewModel.fullWallpaper] except
     * [WallpaperPreviewViewModel.fullPreviewConfigViewModel].
     *
     * Restarting activity and view model to apply the new display.
     */
    private fun updateFullWallpaperFlow(
        model: WallpaperModel,
        whichPreview: WhichPreview,
        internalDisplays: List<FakeDisplaysProvider.FakeDisplayConfig>,
    ) {
        // Restart activity and view model to apply the new display.
        displaysProvider.setDisplays(internalDisplays)
        scenario = ActivityScenario.launch(PreviewTestActivity::class.java)
        scenario.onActivity { setEverything(it) }
        wallpaperPreviewRepository.setWallpaperModel(model)
        wallpaperPreviewViewModel.setWhichPreview(whichPreview)
    }
}
