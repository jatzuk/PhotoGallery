package com.example.photogallery

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_photo_gallery.*
import java.lang.ref.WeakReference

class PhotoGalleryFragment private constructor() : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private var items = ArrayList<GalleryItem>()
    private var isLoading = false
    private var currentPage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        FetchItemsTask(this).execute()
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
                        setupAdapter()
                    }
                })

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val lm = recyclerView.layoutManager as GridLayoutManager
                    if (dy > 0) {
                        if (lm.findLastVisibleItemPosition() > 80 * (currentPage - 1) && !isLoading) {
                            isLoading = true
                            FetchItemsTask(this@PhotoGalleryFragment).execute(++currentPage)
                        }
                    }
                }
            })
        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        page_counter.text = currentPage.toString()
    }

    private fun setupAdapter() {
        if (isAdded) recyclerView.adapter = PhotoAdapter()
    }

    private inner class PhotoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView = itemView as TextView

        fun bindGalleryItem(item: GalleryItem) {
            titleTextView.text = item.toString()
        }
    }

    private inner class PhotoAdapter : RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PhotoHolder(TextView(activity))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            holder.bindGalleryItem(items[position])
        }
    }

    class FetchItemsTask(
        fragment: PhotoGalleryFragment
    ) : AsyncTask<Int, Unit, List<GalleryItem>>() {
        private val weakReference = WeakReference(fragment)

        override fun doInBackground(vararg params: Int?) =
            FlickrFetchr.fetchItems(if (params.isEmpty()) 1 else params[0]!!)

        override fun onPostExecute(result: List<GalleryItem>) {
            with(weakReference.get() ?: return) {
                items.addAll(result as ArrayList)
                page_counter.text = currentPage.toString()
                isLoading = false
                recyclerView.adapter?.notifyItemRangeChanged(100 * currentPage, items.size)
            }
        }
    }

    companion object {
        private val LOG_TAG = PhotoGalleryFragment::class.java.simpleName

        fun newInstance() = PhotoGalleryFragment()
    }
}
