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

package com.android.wallpaper.picker.category.data.repository

import android.content.Context
import android.util.Log
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.picker.category.client.DefaultWallpaperCategoryClient
import com.android.wallpaper.picker.data.category.CategoryModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.util.converter.category.CategoryFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
open class DefaultWallpaperCategoryRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val defaultWallpaperClient: DefaultWallpaperCategoryClient,
    private val categoryFactory: CategoryFactory,
    @BackgroundDispatcher private val backgroundScope: CoroutineScope
) : WallpaperCategoryRepository {

    private val _systemCategories = MutableStateFlow<List<CategoryModel>>(emptyList())
    override val systemCategories: StateFlow<List<CategoryModel>> = _systemCategories.asStateFlow()

    private val _myPhotosCategory = MutableStateFlow<CategoryModel?>(null)
    override val myPhotosCategory: StateFlow<CategoryModel?> = _myPhotosCategory.asStateFlow()

    private val _onDeviceCategory = MutableStateFlow<CategoryModel?>(null)
    override val onDeviceCategory: StateFlow<CategoryModel?> = _onDeviceCategory.asStateFlow()

    private val _isDefaultCategoriesFetched = MutableStateFlow(false)
    override val isDefaultCategoriesFetched: StateFlow<Boolean> =
        _isDefaultCategoriesFetched.asStateFlow()

    init {
        if (BaseFlags.get().isWallpaperCategoryRefactoringEnabled()) {
            backgroundScope.launch { fetchAllCategories() }
        }
    }

    private suspend fun fetchAllCategories() {
        try {
            fetchSystemCategories()
            fetchMyPhotosCategory()
            fetchOnDeviceCategory()
            _isDefaultCategoriesFetched.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching default categories", e)
            _isDefaultCategoriesFetched.value = false
        }
    }

    private suspend fun fetchSystemCategories() {
        try {
            val fetchedCategories = defaultWallpaperClient.getSystemCategories()
            val processedCategories =
                fetchedCategories.map { category ->
                    categoryFactory.getCategoryModel(context, category)
                }
            _systemCategories.value = processedCategories
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching system categories", e)
        }
    }

    private suspend fun fetchMyPhotosCategory() {
        try {
            val myPhotos = defaultWallpaperClient.getMyPhotosCategory()
            _myPhotosCategory.value = myPhotos.let { categoryFactory.getCategoryModel(context, it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching My Photos category", e)
        }
    }

    private suspend fun fetchOnDeviceCategory() {
        try {
            val onDevice =
                (defaultWallpaperClient as? DefaultWallpaperCategoryClient)?.getOnDeviceCategory()
            _onDeviceCategory.value =
                onDevice?.let { categoryFactory.getCategoryModel(context, it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching On Device category", e)
        }
    }

    companion object {
        private const val TAG = "DefaultWallpaperCategoryRepository"
    }
}