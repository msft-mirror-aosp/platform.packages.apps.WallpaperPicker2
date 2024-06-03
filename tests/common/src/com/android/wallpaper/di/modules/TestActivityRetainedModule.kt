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

package com.android.wallpaper.di.modules

import com.android.wallpaper.picker.di.modules.SharedActivityRetainedModule
import com.android.wallpaper.picker.preview.data.repository.ImageEffectsRepository
import com.android.wallpaper.testing.FakeImageEffectsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SharedActivityRetainedModule::class]
)
internal abstract class TestActivityRetainedModule {
    @Binds
    abstract fun bindImageEffectsRepository(
        impl: FakeImageEffectsRepository
    ): ImageEffectsRepository
}
