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

package com.android.wallpaper.picker.category.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.model.PartnerWallpaperInfo
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClient
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import com.android.wallpaper.testing.FakeDefaultCategoryFactory
import com.android.wallpaper.testing.FakeWallpaperParser
import com.android.wallpaper.testing.TestInjector
import com.android.wallpaper.testing.TestPartnerProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultWallpaperCategoryClientTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    lateinit var context: Context
    @Inject lateinit var partnerProvider: TestPartnerProvider
    @Inject lateinit var defaultCategoryFactory: FakeDefaultCategoryFactory
    @Inject lateinit var wallpaperXMLParser: FakeWallpaperParser
    val testDispatcher: CoroutineDispatcher = StandardTestDispatcher()
    val testScope = TestScope(testDispatcher)

    private lateinit var defaultWallpaperCategoryClient: DefaultWallpaperCategoryClient
    @Inject lateinit var testInjector: TestInjector

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        defaultWallpaperCategoryClient =
            DefaultWallpaperCategoryClient(
                context,
                partnerProvider,
                defaultCategoryFactory,
                wallpaperXMLParser
            )
        InjectorProvider.setInjector(testInjector)
        val resources = context.resources
        partnerProvider.resources = resources
        val packageName = context.packageName
        partnerProvider.packageName = packageName
    }

    @Test
    fun getMyPhotosCategory() =
        testScope.runTest {
            val commonCategoryData = CommonCategoryData("My photos", "image_wallpapers", 51)
            val expectedCategoryModel = CategoryModel(commonCategoryData)

            val result = defaultWallpaperCategoryClient.getMyPhotosCategory()

            assertThat(expectedCategoryModel.commonCategoryData.collectionId)
                .isEqualTo(result.commonCategoryData.collectionId)

            assertThat(expectedCategoryModel.commonCategoryData.priority)
                .isEqualTo(result.commonCategoryData.priority)

            assertThat(expectedCategoryModel.commonCategoryData.title)
                .isEqualTo(result.commonCategoryData.title)
        }

    @Test
    fun getValidOnDeviceCategory() =
        testScope.runTest {
            val fakePartnerWallpaperInfo = PartnerWallpaperInfo(1, 1)
            wallpaperXMLParser.wallpapers = listOf(fakePartnerWallpaperInfo)
            val categoryModel =
                testScope.async { defaultWallpaperCategoryClient.getOnDeviceCategory() }.await()

            assertThat(categoryModel).isNotNull()
            assertThat(categoryModel?.commonCategoryData?.title).isEqualTo("On-device wallpapers")
            assertThat(categoryModel?.commonCategoryData?.collectionId)
                .isEqualTo("on_device_wallpapers")
        }

    @Test
    fun getNullOnDeviceCategory() =
        testScope.runTest {
            wallpaperXMLParser.wallpapers = emptyList()
            val categoryModel =
                testScope.async { defaultWallpaperCategoryClient.getOnDeviceCategory() }.await()

            assertThat(categoryModel).isNull()
        }

    @Test
    fun getSystemCategories() =
        testScope.runTest {
            val categoryModel =
                testScope.async { defaultWallpaperCategoryClient.getCategories() }.await()

            assertThat(categoryModel).isNotNull()
            assertThat(categoryModel[0].commonCategoryData.title).isEqualTo("sample-title-1")
            assertThat(categoryModel[0].commonCategoryData.collectionId)
                .isEqualTo("sample-collection-id")
        }
}
