/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.service.wallpaper.WallpaperSettingsActivity
import com.android.wallpaper.picker.data.CreativeWallpaperData
import com.android.wallpaper.picker.data.DownloadableWallpaperData
import com.android.wallpaper.picker.data.LiveWallpaperData
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.preview.domain.interactor.PreviewActionsInteractor
import com.android.wallpaper.picker.preview.ui.util.LiveWallpaperDeleteUtil
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.CUSTOMIZE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.DELETE
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EDIT
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.EFFECTS
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.INFORMATION
import com.android.wallpaper.picker.preview.ui.viewmodel.Action.SHARE
import com.android.wallpaper.picker.preview.ui.viewmodel.floatingSheet.InformationFloatingSheetViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** View model for the preview action buttons */
@ViewModelScoped
class PreviewActionsViewModel
@Inject
constructor(
    private val interactor: PreviewActionsInteractor,
    liveWallpaperDeleteUtil: LiveWallpaperDeleteUtil,
) {
    /** [INFORMATION] */
    private val _informationFloatingSheetViewModel: Flow<InformationFloatingSheetViewModel?> =
        interactor.wallpaperModel.map { wallpaperModel ->
            if (wallpaperModel == null || !wallpaperModel.shouldShowInformationFloatingSheet()) {
                null
            } else {
                InformationFloatingSheetViewModel(
                    wallpaperModel.commonWallpaperData.attributions,
                    wallpaperModel.commonWallpaperData.exploreActionUrl,
                )
            }
        }

    val isInformationVisible: Flow<Boolean> = _informationFloatingSheetViewModel.map { it != null }

    private val _isInformationChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isInformationChecked: Flow<Boolean> = _isInformationChecked.asStateFlow()

    // Floating sheet contents for the bottom sheet dialog. If content is null, the bottom sheet
    // should collapse, otherwise, expended.
    val informationFloatingSheetViewModel: Flow<InformationFloatingSheetViewModel?> =
        combine(isInformationChecked, _informationFloatingSheetViewModel) { checked, viewModel ->
                if (checked && viewModel != null) {
                    viewModel
                } else {
                    null
                }
            }
            .distinctUntilChanged()

    val onInformationClicked: Flow<(() -> Unit)?> =
        combine(isInformationVisible, isInformationChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(INFORMATION)
                    }
                    _isInformationChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    /** [DOWNLOAD] */
    private val downloadableWallpaperData: Flow<DownloadableWallpaperData?> =
        interactor.wallpaperModel.map {
            (it as? WallpaperModel.StaticWallpaperModel)?.downloadableWallpaperData
        }
    val isDownloadVisible: Flow<Boolean> = downloadableWallpaperData.map { it != null }

    val isDownloading: Flow<Boolean> = interactor.isDownloadingWallpaper

    val isDownloadButtonEnabled: Flow<Boolean> =
        combine(downloadableWallpaperData, isDownloading) { downloadableData, isDownloading ->
            downloadableData != null && !isDownloading
        }

    suspend fun downloadWallpaper() {
        interactor.downloadWallpaper()
    }

    /** [DELETE] */
    private val liveWallpaperDeleteIntent: Flow<Intent?> =
        interactor.wallpaperModel.map {
            if (it is LiveWallpaperModel && it.creativeWallpaperData == null && it.canBeDeleted()) {
                liveWallpaperDeleteUtil.getDeleteActionIntent(
                    it.liveWallpaperData.systemWallpaperInfo
                )
            } else {
                null
            }
        }
    private val creativeWallpaperDeleteUri: Flow<Uri?> =
        interactor.wallpaperModel.map {
            val deleteUri = (it as? LiveWallpaperModel)?.creativeWallpaperData?.deleteUri
            if (deleteUri != null && it.canBeDeleted()) {
                deleteUri
            } else {
                null
            }
        }
    val isDeleteVisible: Flow<Boolean> =
        combine(liveWallpaperDeleteIntent, creativeWallpaperDeleteUri) { intent, uri ->
            intent != null || uri != null
        }

    private val _isDeleteChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isDeleteChecked: Flow<Boolean> = _isDeleteChecked.asStateFlow()

    // View model for delete confirmation dialog. Note that null means the dialog should show;
    // otherwise, the dialog should hide.
    val deleteConfirmationDialogViewModel: Flow<DeleteConfirmationDialogViewModel?> =
        combine(isDeleteChecked, liveWallpaperDeleteIntent, creativeWallpaperDeleteUri) {
            isChecked,
            intent,
            uri ->
            if (isChecked && (intent != null || uri != null)) {
                DeleteConfirmationDialogViewModel(
                    onDismiss = { _isDeleteChecked.value = false },
                    liveWallpaperDeleteIntent = intent,
                    creativeWallpaperDeleteUri = uri,
                )
            } else {
                null
            }
        }

    val onDeleteClicked: Flow<(() -> Unit)?> =
        combine(isDeleteVisible, isDeleteChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(DELETE)
                    }
                    _isDeleteChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    /** [EDIT] */
    val editIntent: Flow<Intent?> =
        interactor.wallpaperModel.map {
            (it as? WallpaperModel.LiveWallpaperModel)?.liveWallpaperData?.getEditActivityIntent()
        }
    val isEditVisible: Flow<Boolean> = editIntent.map { it != null }

    private val _isEditChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEditChecked: Flow<Boolean> = _isEditChecked.asStateFlow()

    /** [CUSTOMIZE] */
    private val _isCustomizeVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCustomizeVisible: Flow<Boolean> = _isCustomizeVisible.asStateFlow()

    private val _isCustomizeChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isCustomizeChecked: Flow<Boolean> = _isCustomizeChecked.asStateFlow()

    val onCustomizeClicked: Flow<(() -> Unit)?> =
        combine(isCustomizeVisible, isCustomizeChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(CUSTOMIZE)
                    }
                    _isCustomizeChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    /** [EFFECTS] */
    private val _isEffectsVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEffectsVisible: Flow<Boolean> = _isEffectsVisible.asStateFlow()

    private val _isEffectsChecked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isEffectsChecked: Flow<Boolean> = _isEffectsChecked.asStateFlow()

    val onEffectsClicked: Flow<(() -> Unit)?> =
        combine(isEffectsVisible, isEffectsChecked) { show, isChecked ->
            if (show) {
                {
                    if (!isChecked) {
                        uncheckAllOthersExcept(EFFECTS)
                    }
                    _isEffectsChecked.value = !isChecked
                }
            } else {
                null
            }
        }

    /** [SHARE] */
    val shareIntent: Flow<Intent?> =
        interactor.wallpaperModel.map {
            (it as? LiveWallpaperModel)?.creativeWallpaperData?.getShareIntent()
        }
    val isShareVisible: Flow<Boolean> = shareIntent.map { it != null }

    fun onFloatingSheetCollapsed() {
        // When floating collapsed, we should look for those actions that expand the floating sheet
        // and see which is checked, and uncheck it.
        if (_isInformationChecked.value) {
            _isInformationChecked.value = false
        }
    }

    private fun uncheckAllOthersExcept(action: Action) {
        if (action != INFORMATION) {
            _isInformationChecked.value = false
        }
        if (action != DELETE) {
            _isDeleteChecked.value = false
        }
        if (action != EDIT) {
            _isEditChecked.value = false
        }
        if (action != CUSTOMIZE) {
            _isCustomizeChecked.value = false
        }
        if (action != EFFECTS) {
            _isEffectsChecked.value = false
        }
    }

    companion object {
        private fun WallpaperModel.shouldShowInformationFloatingSheet(): Boolean {
            return if (
                commonWallpaperData.attributions.isNullOrEmpty() &&
                    commonWallpaperData.exploreActionUrl.isNullOrEmpty()
            ) {
                // If neither of the attributes nor the action url exists, do not show the
                // information floating sheet.
                false
            } else if (
                this is LiveWallpaperModel &&
                    !liveWallpaperData.systemWallpaperInfo.showMetadataInPreview
            ) {
                // If the live wallpaper's flag of showMetadataInPreview is false, do not show the
                // information floating sheet.
                false
            } else {
                true
            }
        }

        private fun CreativeWallpaperData.getShareIntent(): Intent {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri)
            shareIntent.setType("image/*")
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.clipData = ClipData.newRawUri(null, shareUri)
            return Intent.createChooser(shareIntent, null)
        }

        private fun LiveWallpaperModel.canBeDeleted(): Boolean {
            return if (creativeWallpaperData != null) {
                !liveWallpaperData.isApplied &&
                    !creativeWallpaperData.isCurrent &&
                    creativeWallpaperData.deleteUri.toString().isNotEmpty()
            } else {
                !liveWallpaperData.isApplied
            }
        }

        fun LiveWallpaperData.getEditActivityIntent(): Intent? {
            val settingsActivity = systemWallpaperInfo.settingsActivity
            if (settingsActivity.isNullOrEmpty()) {
                return null
            }
            val intent =
                Intent().apply {
                    component = ComponentName(systemWallpaperInfo.packageName, settingsActivity)
                    putExtra(WallpaperSettingsActivity.EXTRA_PREVIEW_MODE, true)
                }
            return intent
        }

        fun LiveWallpaperModel.isNewCreativeWallpaper(): Boolean {
            return creativeWallpaperData?.deleteUri?.toString()?.isEmpty() == true
        }
    }
}

enum class Action {
    INFORMATION,
    DOWNLOAD,
    DELETE,
    EDIT,
    CUSTOMIZE,
    EFFECTS,
    SHARE,
}
