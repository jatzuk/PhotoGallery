package com.example.photogallery

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_photo_gallery.*
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

class PhotoGalleryFragment private constructor() : VisibleFragment() {
    private lateinit var thumbnailDownloader: ThumbnailDownloader<Int>
    private lateinit var recyclerView: RecyclerView
    private var items = ArrayList<GalleryItem>()
    private var isLoading = false
    private var currentPage = 1
    private var firstItemPosition = 1
    private var lastItemPosition = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        updateItems()

        val responseHandler = Handler()
        thumbnailDownloader = ThumbnailDownloader(responseHandler)
        thumbnailDownloader.apply {
            thumbnailDownloadListener =
                object : ThumbnailDownloader.ThumbnailDownloadListener<Int> {
                    override fun onThumbnailDownloaded(position: Int, bitmap: Bitmap) {
                        recyclerView.adapter?.notifyItemChanged(position)
                    }
                }
            start()
            getLooper()
        }
        Log.i(LOG_TAG, "background thread started")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        recyclerView = v.findViewById(R.id.photo_recycler_view)
        recyclerView.apply {
            viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        layoutManager = GridLayoutManager(activity, width / 300)
                    }
                })

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val lm = recyclerView.layoutManager as GridLayoutManager
                    val lastVisible = lm.findLastVisibleItemPosition()
                    val firstVisible = lm.findFirstVisibleItemPosition()

                    if (lastItemPosition != lastVisible || firstItemPosition != firstVisible) {
                        Log.i(LOG_TAG, "showing items from $firstVisible to $lastVisible")
                        lastItemPosition = lastVisible
                        firstItemPosition = firstVisible
                        val start = max(firstVisible - 10, 0)
                        val end = min(lastVisible + 10, items.size - 1)
                        for (i in start until end) {
                            val url = items[i].url
                            if (thumbnailDownloader.cache[url] == null) {
                                Log.i(LOG_TAG, "Requesting download at position: $i")
                                thumbnailDownloader.queueThumbnail(i, url)
                            }
                        }
                    }

                    if (dy > 0) {
                        if (lastVisible > 80 * currentPage && !isLoading) {
                            isLoading = true
                            FetchItemsTask(this@PhotoGalleryFragment, null).execute(++currentPage)
                        }
                    }

                    if (dy < 0) {
                        if (firstVisible < 80 * (currentPage - 1) && !isLoading) {
                            currentPage--
                            updatePageNumber()
                        }
                    }
                }
            })

            if (recyclerView.adapter == null) setupAdapter()
        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePageNumber()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem = menu?.findItem(R.id.menu_item_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(LOG_TAG, "QueryTextSubmit: $query")
                QueryPreferences.setStoredQuery(context!!, query)
                updateItems()
                searchView.onActionViewCollapsed()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.d(LOG_TAG, "QueryTextChange: $newText")
                return false
            }
        })

        searchView.setOnSearchClickListener {
            searchView.setQuery(QueryPreferences.getStoredQuery(context!!), false)
        }

        menu.findItem(R.id.menu_item_toggle_polling).title =
            if (PollService.isServiceAlarmOn(activity!!)) getString(R.string.stop_polling)
            else getString(R.string.start_polling)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_item_clear -> {
                QueryPreferences.setStoredQuery(context!!, null)
                updateItems()
                true
            }
            R.id.menu_item_toggle_polling -> {
                val shouldStartAlarm = !PollService.isServiceAlarmOn(activity!!)
                PollService.setServiceAlarm(activity!!, shouldStartAlarm)
                activity!!.invalidateOptionsMenu()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        with(thumbnailDownloader) {
            clearQueue()
            clearCache()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thumbnailDownloader.quit()
        Log.i(LOG_TAG, "Background thread destroyed")
    }

    private fun updateItems() {
        FetchItemsTask(this, QueryPreferences.getStoredQuery(context!!)).execute()
    }

    private fun setupAdapter() {
        if (isAdded) recyclerView.adapter = PhotoAdapter()
    }

    private fun updatePageNumber() {
        page_counter.text = getString(R.string.page_number, currentPage)
    }

    private inner class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private val itemImageView = itemView.findViewById(R.id.item_image_view) as ImageView
        private lateinit var galleryItem: GalleryItem

        init {
            itemView.setOnClickListener(this)
        }

        fun bindDrawable(drawable: Drawable) {
            itemImageView.setImageDrawable(drawable)
        }

        fun bindGalleryItem(galleryItem: GalleryItem) {
            this.galleryItem = galleryItem
        }

        override fun onClick(v: View?) {
            startActivity(PhotoPageActivity.newIntent(context!!, galleryItem.getPhotoPageUri()))
        }
    }

    private inner class PhotoAdapter : RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = LayoutInflater.from(activity).inflate(R.layout.gallery_item, parent, false)
            return PhotoHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            Log.i(LOG_TAG, "Binding item $position to ${holder.hashCode()}")
            val galleryItem = items[position]
            holder.bindGalleryItem(galleryItem)
            val bitmap = thumbnailDownloader.cache[galleryItem.url]
            val drawable = if (bitmap == null) {
                ContextCompat.getDrawable(context!!, R.drawable.cat)!!
            } else BitmapDrawable(resources, bitmap)
            holder.bindDrawable(drawable)
        }
    }

    class FetchItemsTask(
        fragment: PhotoGalleryFragment,
        private val query: String?
    ) : AsyncTask<Int, Unit, List<GalleryItem>>() {
        private val weakReference = WeakReference(fragment)

        override fun doInBackground(vararg params: Int?): List<GalleryItem> {
            return if (query.isNullOrEmpty()) {
                FlickrFetchr.fetchRecentPhotos(if (params.isEmpty()) 1 else params[0]!!)
            } else {
                Log.d(LOG_TAG, "query search")
                FlickrFetchr.searchPhotos(query)
            }
        }

        override fun onPostExecute(result: List<GalleryItem>) {
            with(weakReference.get() ?: return) {
                if (query.isNullOrEmpty()) {
                    fillItems(result)
                    recyclerView.adapter?.notifyItemRangeChanged(100 * currentPage, items.size)
                } else {
                    items.clear()
                    fillItems(result)
                    currentPage = 1
                    recyclerView.adapter?.notifyDataSetChanged()
                }
                updatePageNumber()
                isLoading = false
            }
        }

        private fun fillItems(result: List<GalleryItem>) {
            weakReference.get()?.items?.addAll(result)
        }
    }

    companion object {
        private val LOG_TAG = PhotoGalleryFragment::class.java.simpleName

        fun newInstance() = PhotoGalleryFragment()
    }
}
