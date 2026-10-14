/*
 * Copyright (C) 2026 samakshkambxj
 * SPDX-License-Identifier: Apache-2.0
 */

package com.daybreak.dolby

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.daybreak.dolby.audio.DolbyController
import kotlinx.coroutines.runBlocking

/** Settings-injected summary: On (profile) / Off. */
class SummaryProvider : ContentProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method != KEY_DOLBY) return null
        val summary = runBlocking {
            val controller = DolbyController.get(context!!)
            if (!controller.isEnabled()) {
                context!!.getString(R.string.level_off)
            } else {
                val profile = controller.currentProfile().toString()
                val values = context!!.resources.getStringArray(R.array.profile_values)
                val index = values.indexOf(profile)
                if (index == -1) {
                    context!!.getString(R.string.level_off)
                } else {
                    context!!.resources.getStringArray(R.array.profile_entries)[index]
                }
            }
        }
        return Bundle().apply {
            putString(META_DATA_PREFERENCE_SUMMARY, summary)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    companion object {
        private const val KEY_DOLBY = "dolby"
        private const val META_DATA_PREFERENCE_SUMMARY = "com.android.settings.summary"
    }
}
