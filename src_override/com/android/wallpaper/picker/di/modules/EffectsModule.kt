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
package com.android.wallpaper.picker.di.modules

import com.android.wallpaper.effects.DefaultEffectsController
import com.android.wallpaper.effects.EffectsController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** This class provides the singleton scoped effects controller for wallpaper picker. */
@InstallIn(SingletonComponent::class)
@Module
abstract class EffectsModule {

    @Binds
    @Singleton
    abstract fun bindEffectsController(impl: DefaultEffectsController): EffectsController
}
