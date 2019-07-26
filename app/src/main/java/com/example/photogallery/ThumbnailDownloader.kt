package com.example.photogallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import android.util.LruCache
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class ThumbnailDownloader<T>(private val responseHandler: Handler) : HandlerThread(LOG_TAG) {
    private var hasQuit = false
    private val requestMap = ConcurrentHashMap<T, String>()
    private lateinit var requestHandler: Handler
    lateinit var thumbnailDownloadListener: ThumbnailDownloadListener<T>
    var cache = LruCache<String, Bitmap>(4 * 1024 * 1024)

    override fun onLooperPrepared() {
        requestHandler = RequestHandler(this)
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    fun queueThumbnail(position: T, url: String) {
        Log.i(LOG_TAG, "Got a URL: $url")
        if (url.isEmpty()) requestMap.remove(position)
        else {
            requestMap[position] = url
            requestHandler.obtainMessage(MESSAGE_DOWNLOAD, position).sendToTarget()
        }
    }

    fun clearQueue() {
        requestHandler.removeMessages(MESSAGE_DOWNLOAD)
        requestMap.clear()
    }

    fun clearCache() {
        cache.evictAll()
    }

    companion object {
        private val LOG_TAG = ThumbnailDownloader::class.java.simpleName
        private const val MESSAGE_DOWNLOAD = 0
    }

    class RequestHandler<T>(handler: ThumbnailDownloader<T>) : Handler() {
        private val weakReference = WeakReference(handler).get()!!
        private val requestMap = weakReference.requestMap
        private val responseHandler = weakReference.responseHandler
        private val hasQuit = weakReference.hasQuit
        private val thumbnailDownloadListener = weakReference.thumbnailDownloadListener
        private val cache = weakReference.cache

        override fun handleMessage(msg: Message?) {
            if (msg?.what == MESSAGE_DOWNLOAD) {
                @Suppress("UNCHECKED_CAST")
                val target = msg.obj as T
                Log.i(LOG_TAG, "Got a request for url: ${requestMap[target]}")
                handleRequest(target)
            }
        }

        private fun handleRequest(position: T) {
            try {
                val url = requestMap[position]
                if (url.isNullOrEmpty()) return
                val bitmapBytes = FlickrFetchr.getUrlBytes(url)
                val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
                cache.put(url, bitmap)
                Log.i(LOG_TAG, "Bitmap size: ${bitmapBytes.size / 1024}kb")
                responseHandler.post(Runnable {
                    if (requestMap[position] != url && hasQuit) return@Runnable
                    requestMap.remove(position)
                    thumbnailDownloadListener.onThumbnailDownloaded(position, bitmap)
                })
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error downloading image", e)
            }
        }
    }

    interface ThumbnailDownloadListener<T> {
        fun onThumbnailDownloaded(position: T, bitmap: Bitmap)
    }
}
