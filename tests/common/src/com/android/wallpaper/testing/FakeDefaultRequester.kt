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

package com.android.wallpaper.testing

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import com.android.volley.Request
import com.android.wallpaper.network.Requester
import com.bumptech.glide.request.target.Target
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDefaultRequester @Inject constructor() : Requester {
    override fun <T : Any?> addToRequestQueue(request: Request<T>?) {
        // Do nothing intended
    }

    override fun loadImageFile(imageUrl: Uri?): File {
        return File("test_file.txt")
    }

    override fun loadImageFileWithActivity(
        activity: Activity?,
        imageUrl: Uri?,
        target: Target<File>?
    ) {
        // Do nothing intended
    }

    override fun loadImageBitmap(imageUrl: Uri?, target: Target<Bitmap>?) {
        // Do nothing intended
    }
}
