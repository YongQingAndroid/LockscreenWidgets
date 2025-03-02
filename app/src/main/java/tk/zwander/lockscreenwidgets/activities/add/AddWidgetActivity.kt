package tk.zwander.lockscreenwidgets.activities.add

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import kotlinx.coroutines.*
import tk.zwander.lockscreenwidgets.R
import tk.zwander.lockscreenwidgets.activities.DismissOrUnlockActivity
import tk.zwander.lockscreenwidgets.adapters.AppAdapter
import tk.zwander.lockscreenwidgets.data.AppInfo
import tk.zwander.lockscreenwidgets.data.list.ShortcutListInfo
import tk.zwander.lockscreenwidgets.data.WidgetData
import tk.zwander.lockscreenwidgets.data.list.WidgetListInfo
import tk.zwander.lockscreenwidgets.databinding.ActivityAddWidgetBinding
import tk.zwander.lockscreenwidgets.host.WidgetHostCompat
import tk.zwander.lockscreenwidgets.util.ShortcutIdManager
import tk.zwander.lockscreenwidgets.util.prefManager
import tk.zwander.lockscreenwidgets.util.toBase64

/**
 * Manage the widget addition flow: selection, permissions, configurations, etc.
 */
open class AddWidgetActivity : BaseBindWidgetActivity(), CoroutineScope by MainScope() {
    protected open val showShortcuts = true

    private val adapter by lazy {
        AppAdapter(this) {
            if (it is WidgetListInfo) {
                tryBindWidget(it.providerInfo)
            } else if (it is ShortcutListInfo) {
                tryBindShortcut(it)
            }
        }
    }

    private var searchItem: MenuItem? = null
    private val searchView: SearchView?
        get() = (searchItem?.actionView as SearchView?)

    private var doneLoading = false

    private val binding by lazy { ActivityAddWidgetBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**
         * We want the user to unlock the device when adding a widget, since potential configuration Activities
         * won't show on the lock screen.
         */
        val intent = Intent(this, DismissOrUnlockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

        setContentView(binding.root)

        binding.selectionList.adapter = adapter

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        populateAsync()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_search, menu)
        searchItem = menu?.findItem(R.id.search)

        searchItem?.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                adapter.currentFilter = null
                return true
            }

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }
        })

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.currentFilter = newText
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })

        if (doneLoading) {
            searchItem?.isVisible = true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }



    /**
     * Populate the selection list with the available widgets.
     * Lockscreen Widgets checks for both home screen and keyguard
     * widgets.
     *
     * This method runs asynchronously to avoid hanging the UI thread.
     */
    private fun populateAsync() = launch {
        val apps = withContext(Dispatchers.Main) {
            val apps = HashMap<String, AppInfo>()

            (appWidgetManager.installedProviders + appWidgetManager.getInstalledProviders(
                AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
            )).forEach {
                val appInfo = packageManager.getApplicationInfo(it.provider.packageName, 0)

                val appName = packageManager.getApplicationLabel(appInfo)
                val widgetName = it.loadLabel(packageManager)

                var app = apps[appInfo.packageName]
                if (app == null) {
                    apps[appInfo.packageName] = AppInfo(appName.toString(), appInfo)
                    app = apps[appInfo.packageName]!!
                }

                app.widgets.add(
                    WidgetListInfo(
                        widgetName,
                        it.previewImage.run { if (this != 0) this else appInfo.icon },
                        it,
                        appInfo
                    )
                )
            }

            if (showShortcuts) {
                packageManager.queryIntentActivities(
                    Intent(Intent.ACTION_CREATE_SHORTCUT),
                    PackageManager.GET_RESOLVED_FILTER
                ).forEach {
                    val appInfo = packageManager.getApplicationInfo(it.activityInfo.packageName, 0)

                    val appName = appInfo.loadLabel(packageManager)
                    val shortcutName = it.loadLabel(packageManager)

                    var app = apps[appInfo.packageName]
                    if (app == null) {
                        apps[appInfo.packageName] = AppInfo(appName.toString(), appInfo)
                        app = apps[appInfo.packageName]!!
                    }

                    app!!.shortcuts.add(
                        ShortcutListInfo(
                            shortcutName.toString(),
                            it.iconResource,
                            it,
                            appInfo
                        )
                    )
                }
            }

            apps
        }

        adapter.setItems(apps.values.toList())
        binding.progress.visibility = View.GONE
        binding.selectionList.visibility = View.VISIBLE
        searchItem?.isVisible = true

        doneLoading = true
    }
}