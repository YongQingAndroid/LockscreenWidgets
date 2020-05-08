package tk.zwander.lockscreenwidgets.util

import android.annotation.DimenRes
import android.annotation.IntegerRes
import android.content.Context
import android.content.ContextWrapper
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import tk.zwander.lockscreenwidgets.data.WidgetData
import tk.zwander.systemuituner.lockscreenwidgets.R

class PrefManager private constructor(context: Context) : ContextWrapper(context) {
    companion object {
        const val KEY_CURRENT_WIDGETS = "current_widgets"
        const val KEY_FRAME_WIDTH = "frame_width"
        const val KEY_FRAME_HEIGHT = "frame_height"
        const val KEY_POS_X = "position_x"
        const val KEY_POS_Y = "position_y"

        private var instance: PrefManager? = null

        fun getInstance(context: Context): PrefManager {
            return instance ?: run {
                instance = PrefManager(context.applicationContext)
                instance!!
            }
        }
    }

    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val gson = GsonBuilder()
        .create()

    var currentWidgets: HashSet<WidgetData>
        get() = gson.fromJson(
            getString(KEY_CURRENT_WIDGETS),
            object : TypeToken<HashSet<WidgetData>>() {}.type
        ) ?: HashSet()
        set(value) {
            putString(
                KEY_CURRENT_WIDGETS,
                gson.toJson(value)
            )
        }

    var frameWidthDp: Float
        get() = getFloat(KEY_FRAME_WIDTH, getResourceFloat(R.integer.def_frame_width))
        set(value) {
            putFloat(KEY_FRAME_WIDTH, value)
        }

    var frameHeightDp: Float
        get() = getFloat(KEY_FRAME_HEIGHT, getResourceFloat(R.integer.def_frame_height))
        set(value) {
            putFloat(KEY_FRAME_HEIGHT, value)
        }

    var posX: Int
        get() = getInt(KEY_POS_X, 0)
        set(value) {
            putInt(KEY_POS_X, value)
        }

    var posY: Int
        get() = getInt(KEY_POS_Y, 0)
        set(value) {
            putInt(KEY_POS_Y, value)
        }


    fun getString(key: String, def: String? = null) = prefs.getString(key, def)
    fun getFloat(key: String, def: Float) = prefs.getFloat(key, def)
    fun getInt(key: String, def: Int) = prefs.getInt(key, def)

    fun putString(key: String, value: String?) = prefs.edit { putString(key, value) }
    fun putFloat(key: String, value: Float) = prefs.edit { putFloat(key, value) }
    fun putInt(key: String, value: Int) = prefs.edit { putInt(key, value) }

    fun getResourceFloat(@IntegerRes resource: Int): Float {
        return resources.getInteger(resource).toFloat()
    }
}