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

package com.android.wallpaper.picker.category.domain.interactor.implementations

import com.android.wallpaper.picker.category.domain.interactor.ThirdPartyCategoryInteractor
import com.android.wallpaper.picker.data.category.CategoryModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class ThirdPartyCategoryInteractorImpl @Inject constructor() : ThirdPartyCategoryInteractor {
    override val categories: Flow<List<CategoryModel>> = flow {
        // TODO(b/355704785): to provide actual implementation
        emit(listOf())
    }
}
