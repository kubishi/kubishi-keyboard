// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.content.ClipDescription
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.FileProvider
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Log
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.inputmethod.InputContentInfoCompat
import helium314.keyboard.latin.database.ClipboardDao
import java.io.File

class ClipboardHistoryEntry(
    val id: Long,
    var timeStamp: Long,
    var isPinned: Boolean,
    val text: String?,
    val filename: String?,
    val mimeTypes: List<String>?
) : Comparable<ClipboardHistoryEntry> {
    // for display order
    override fun compareTo(other: ClipboardHistoryEntry): Int {
        val result = other.isPinned.compareTo(isPinned)
        if (result == 0) return other.timeStamp.compareTo(timeStamp)
        if (Settings.getValues()?.mClipboardHistoryPinnedFirst == false) return -result
        return result
    }

    fun getContentInfo(context: Context): InputContentInfoCompat {
        return InputContentInfoCompat(getContentUri(context)!!, ClipDescription(text, mimeTypes?.toTypedArray()), null)
    }

    fun getContentUri(context: Context) = filename?.let { FileProvider.getUriForFile(
        context,
        context.getString(R.string.clipboard_provider_authority),
        File(ClipboardDao.clipFilesDir, it)
    ) }

    fun getImageAndDescription(context: Context): Pair<Drawable?, String> {
        if (mimeTypes == null || filename == null) return null to "" // should never happen
        try {
            val path = File(ClipboardDao.clipFilesDir, filename).absolutePath
            // looks like decoded size is adjusted automatically to fit screen
            return BitmapFactory.decodeFile(path).toDrawable(context.resources) to ""
        } catch (e: Exception) {
            Log.w("ClipboardHistoryEntry", "could not load image for clip $id", e)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val info = context.contentResolver.getTypeInfo(mimeTypes[0])
            val drawable = info.icon.loadDrawable(context)
            if (drawable != null)
                Settings.getValues().mColors.setColor(drawable, ColorType.EMOJI_CATEGORY) // todo: colorType?
            return drawable to (info.label.toString() + if (text.isNullOrBlank()) "" else "\n$text")
        }
        return null to mimeTypes.first() + if (text.isNullOrBlank()) "" else "\n$text"
    }
}
