package com.example.photogallery

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class ThumbnailDownloader<T> : HandlerThread(LOG_TAG) {
    private var hasQuit = false
    private val requestMap = ConcurrentHashMap<T, String>()
    private lateinit var requestHandler: Handler

    override fun onLooperPrepared() {
        requestHandler = RequestHandler(this)
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    fun queueThumbnail(target: T, url: String) {
        Log.i(LOG_TAG, "Got a URL: $url")
        if (url.isEmpty()) requestMap.remove(target)
        else {
            requestMap[target] = url
            requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()
        }
    }

    companion object {
        private val LOG_TAG = ThumbnailDownloader::class.java.simpleName
        private const val MESSAGE_DOWNLOAD = 0
    }

    class RequestHandler<T>(handler: ThumbnailDownloader<T>): Handler() {
        private val requestMap = WeakReference(handler).get()!!.requestMap

        override fun handleMessage(msg: Message?) {
            if (msg?.what == MESSAGE_DOWNLOAD) {
                val target = msg.obj as T
                Log.i(LOG_TAG, "Got a request for url: ${requestMap[target]}")
                handleRequest(target)
            }
        }

        private fun handleRequest(target: T) {
            try {
                val url = requestMap[target]
                if (url.isNullOrEmpty()) return
                val bitmapBytes = FlickrFetchr.getUrlBytes(url)
                val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
                Log.i(LOG_TAG, "Bitmap created")
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error downloading image", e)
            }
        }
    }
}
