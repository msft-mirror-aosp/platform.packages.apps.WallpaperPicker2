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

package com.android.wallpaper.picker.category.interactor

import android.content.Context
import com.android.wallpaper.picker.category.domain.interactor.implementations.CategoryInteractorImpl
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.data.category.CommonCategoryData
import com.android.wallpaper.testing.FakeDefaultWallpaperCategoryRepository
import com.google.common.truth.Truth
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class CategoryInteractorImplTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context
    @Inject
    lateinit var fakeDefaultWallpaperCategoryRepository: FakeDefaultWallpaperCategoryRepository
    private lateinit var categoryInteractorImpl: CategoryInteractorImpl

    @Before
    fun setup() {
        hiltRule.inject()
        categoryInteractorImpl = CategoryInteractorImpl(fakeDefaultWallpaperCategoryRepository)
    }

    @Test
    fun testFetchCategoriesWithValidThirdPartyCategory() = runBlocking {
        val categories = categoryInteractorImpl.categories.first()

        Truth.assertThat(categories.size).isEqualTo(3)
        Truth.assertThat(
            categories.contains(
                CategoryModel(
                    commonCategoryData = CommonCategoryData("ThirdParty-2", "downloads_id", 3),
                    thirdPartyCategoryData = null,
                    imageCategoryData = null,
                    collectionCategoryData = null
                )
            )
        )
    }
}