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

package com.android.wallpaper.util

import android.content.Context
import android.content.res.XmlResourceParser
import androidx.annotation.XmlRes
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.module.PartnerProvider
import com.android.wallpaper.testing.TestPartnerProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDisplayManager

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowDisplayManager::class])
class WallpaperXMLParserTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    var context: Context = ApplicationProvider.getApplicationContext<HiltTestApplication>()
    @Inject lateinit var partnerProvider: TestPartnerProvider
    @Inject lateinit var wallpaperXMLParser: WallpaperXMLParser
    private val testDispatcher: CoroutineDispatcher = StandardTestDispatcher()
    val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        wallpaperXMLParser = WallpaperXMLParser(context, partnerProvider)
    }

    /**
     * This test uses the file wallpapers.xml that is defined in the resources folder to make sure
     * that we parse categories correctly.
     */
    @Test
    fun parseXMLForSystemCategories_shouldReturnCategories() {
        val resources = context.resources
        partnerProvider.resources = resources
        val packageName = context.packageName
        partnerProvider.packageName = packageName
        @XmlRes
        val wallpapersResId: Int =
            resources.getIdentifier(PartnerProvider.WALLPAPER_RES_ID, "xml", packageName)
        assertThat(wallpapersResId).isNotEqualTo(0)
        val parser: XmlResourceParser = resources.getXml(wallpapersResId)

        val categories = wallpaperXMLParser.parseSystemCategories(parser)

        assertThat(categories).hasSize(1)
        assertThat(categories[0].collectionId).isEqualTo("category1")
    }

    /**
     * This test uses the file invalid_wallpapers.xml that is defined in the resources folder where
     * if incorrect tags are defined, we return empty categories.
     */
    @Test
    fun parseInvalidXMLForSystemCategories_shouldReturnEmptyCategories() {
        val resources = context.resources
        partnerProvider.resources = resources
        val packageName = context.packageName
        partnerProvider.packageName = packageName
        @XmlRes
        val wallpapersResId: Int = resources.getIdentifier("invalid_wallpapers", "xml", packageName)
        assertThat(wallpapersResId).isNotEqualTo(0)
        val parser: XmlResourceParser = resources.getXml(wallpapersResId)

        val categories = wallpaperXMLParser.parseSystemCategories(parser)

        assertThat(categories).hasSize(0)
    }

    /**
     * This test uses the file exception_wallpapers.xml that is defined in the resources folder
     * where if some mandatory attributes aren't defined, an exception will be thrown.
     */
    @Test
    fun parseInvalidXMLForSystemCategories_shouldThrowException() {
        val resources = context.resources
        partnerProvider.resources = resources
        val packageName = context.packageName
        partnerProvider.packageName = packageName
        @XmlRes
        val wallpapersResId: Int =
            resources.getIdentifier("exception_wallpapers", "xml", packageName)
        assertThat(wallpapersResId).isNotEqualTo(0)
        val parser: XmlResourceParser = resources.getXml(wallpapersResId)

        assertThat(
                assertThrows(NullPointerException::class.java) {
                    wallpaperXMLParser.parseSystemCategories(parser)
                }
            )
            .isNotNull()
    }
}
