package tk.zwander.lockscreenwidgets.adapters

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.app_item.view.*
import tk.zwander.lockscreenwidgets.R
import tk.zwander.lockscreenwidgets.data.AppInfo
import tk.zwander.lockscreenwidgets.data.WidgetListInfo
import tk.zwander.lockscreenwidgets.util.matchesFilter

/**
 * Handle the hosting of the list of apps that have widgets that can be added to the widget frame.
 */
class AppAdapter(context: Context, private val selectionCallback: (provider: WidgetListInfo) -> Unit) : RecyclerView.Adapter<AppAdapter.AppVH>() {
    var currentFilter: String? = null
        set(value) {
            field = value
            refreshFilter()
        }

    private val items = AsyncListDiffer(this, object : DiffUtil.ItemCallback<AppInfo>() {
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return false
        }

        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.appInfo.packageName == newItem.appInfo.packageName
        }
    })

    private val orig = HashMap<String, AppInfo>()

    private val picasso = Picasso.Builder(context)
        .addRequestHandler(AddWidgetAdapter.AppIconRequestHandler(context))
        .addRequestHandler(AddWidgetAdapter.RemoteResourcesIconHandler(context))
        .build()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AppVH(
            LayoutInflater.from(parent.context).inflate(
                R.layout.app_item,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: AppVH, position: Int) {
        holder.parseInfo(items.currentList[position])
        holder.setIsRecyclable(false)
    }

    override fun getItemCount() = items.currentList.size

    fun setItems(items: List<AppInfo>) {
        orig.clear()
        items.forEach {
            orig[it.appInfo.packageName] = it
        }
        refreshFilter()
    }

    private fun refreshFilter() {
        val newItems = orig.values
            .filter { it.matchesFilter(currentFilter) }
            .sorted()

        items.submitList(newItems) {
            notifyDataSetChanged()
        }
    }

    /**
     * Handle loading and displaying app info, as well as list of widgets
     */
    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        private val adapter = AddWidgetAdapter(picasso, selectionCallback)

        fun parseInfo(info: AppInfo) {
            itemView.widget_holder.adapter = adapter
            itemView.widget_holder.addItemDecoration(DividerItemDecoration(itemView.context, RecyclerView.HORIZONTAL))

            itemView.app_name.text = info.appName
            adapter.setItems(info.widgets)

            picasso.cancelRequest(itemView.app_icon)

            picasso
                .load(Uri.parse("${AddWidgetAdapter.AppIconRequestHandler.SCHEME}:${info.appInfo.packageName}"))
                .fit()
                .into(itemView.app_icon)
        }
    }
}