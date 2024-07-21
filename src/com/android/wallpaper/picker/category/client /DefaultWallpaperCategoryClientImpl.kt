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

package com.android.wallpaper.picker.category.client

import android.content.Context
import com.android.wallpaper.R
import com.android.wallpaper.model.Category
import com.android.wallpaper.model.DefaultWallpaperInfo
import com.android.wallpaper.model.ImageCategory
import com.android.wallpaper.model.LegacyPartnerWallpaperInfo
import com.android.wallpaper.model.WallpaperCategory
import com.android.wallpaper.model.WallpaperInfo
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.util.WallpaperParser
import com.android.wallpaper.util.converter.category.CategoryFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is responsible for fetching wallpaper categories, listed as follows:
 * 1. MyPhotos category that allows users to select custom photos
 * 2. OnDevice category that are pre-loaded wallpapers on device (legacy way of pre-loading
 *    wallpapers, modern way is described below)
 * 3. System categories on device (modern way of pre-loading wallpapers on device)
 */
@Singleton
class DefaultWallpaperCategoryClientImpl
@Inject
constructor(
    @ApplicationContext val context: Context,
    private val partnerProvider: PartnerProvider,
    private val wallpaperXMLParser: WallpaperParser
) : DefaultWallpaperCategoryClient {

    /** This method is used for fetching and creating the MyPhotos category tile. */
    override suspend fun getMyPhotosCategory(): Category {
        val imageCategory =
            ImageCategory(
                context.getString(R.string.my_photos_category_title),
                context.getString(R.string.image_wallpaper_collection_id),
                PRIORITY_MY_PHOTOS_WHEN_CREATIVE_WALLPAPERS_ENABLED,
                R.drawable.wallpaperpicker_emptystate /* overlayIconResId */
            )
        return imageCategory
    }

    /**
     * This method is used for fetching the on-device categories. This returns a category which
     * incorporates both GEL and bundled wallpapers.
     */
    override suspend fun getOnDeviceCategory(): Category? {
        val onDeviceWallpapers = mutableListOf<WallpaperInfo?>()

        if (!partnerProvider.shouldHideDefaultWallpaper()) {
            val defaultWallpaperInfo = DefaultWallpaperInfo()
            onDeviceWallpapers.add(defaultWallpaperInfo)
        }

        val partnerWallpaperInfos = wallpaperXMLParser.parsePartnerWallpaperInfoResources()
        onDeviceWallpapers.addAll(partnerWallpaperInfos)

        val legacyPartnerWallpaperInfos = LegacyPartnerWallpaperInfo.getAll(context)
        onDeviceWallpapers.addAll(legacyPartnerWallpaperInfos)

        val privateWallpapers = getPrivateDeviceWallpapers()
        privateWallpapers?.let { onDeviceWallpapers.addAll(it) }

        return onDeviceWallpapers
            .takeIf { it.isNotEmpty() }
            ?.let {
                val wallpaperCategory =
                    WallpaperCategory(
                        context.getString(R.string.on_device_wallpapers_category_title),
                        context.getString(R.string.on_device_wallpaper_collection_id),
                        it,
                        PRIORITY_ON_DEVICE
                    )
                wallpaperCategory
            }
    }

    /** This method is used for fetching the system categories. */
    override suspend fun getSystemCategories(): List<Category> {
        val partnerRes = partnerProvider.resources
        val packageName = partnerProvider.packageName
        if (partnerRes == null || packageName == null) {
            return listOf()
        }

        val wallpapersResId =
            partnerRes.getIdentifier(PartnerProvider.WALLPAPER_RES_ID, "xml", packageName)
        // Certain partner configurations don't have wallpapers provided, so need to check;
        // return early if they are missing.
        if (wallpapersResId == 0) {
            return listOf()
        }

        val categories =
            wallpaperXMLParser.parseSystemCategories(partnerRes.getXml(wallpapersResId))
        return categories
    }

    private fun getLocale(): Locale {
        return context.resources.configuration.locales.get(0)
    }

    private fun getPrivateDeviceWallpapers(): Collection<WallpaperInfo?>? {
        return null
    }

    companion object {
        private const val TAG = "DefaultWallpaperCategoryClientImpl"

        /**
         * Relative category priorities. Lower numbers correspond to higher priorities (i.e., should
         * appear higher in the categories list).
         */
        const val PRIORITY_MY_PHOTOS_WHEN_CREATIVE_WALLPAPERS_DISABLED = 1
        private const val PRIORITY_MY_PHOTOS_WHEN_CREATIVE_WALLPAPERS_ENABLED = 51
        private const val PRIORITY_SYSTEM = 100
        private const val PRIORITY_ON_DEVICE = 200
        private const val PRIORITY_LIVE = 300
        private const val PRIORITY_THIRD_PARTY = 400
        const val CREATIVE_CATEGORY_PRIORITY = 1
    }
}