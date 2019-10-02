package com.example.photogallery

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

object FlickrFetchr {
    private const val API_KEY = "3df4bf25938544544d17ee089e0979a3"
    private const val FETCH_RECENT_METHOD = "flickr.photos.getRecent"
    private const val SEARCH_METHOD = "flickr.photos.search"
    private val ENDPOINT = Uri
        .parse("https://www.flickr.com/services/rest/")
        .buildUpon()
        .appendQueryParameter("api_key", API_KEY)
        .appendQueryParameter("format", "json")
        .appendQueryParameter("nojsoncallback", "1")
        .appendQueryParameter("extras", "url_s")
        .build()
    private val LOG_TAG = this::class.java.simpleName

    fun getUrlBytes(urlSpec: String): ByteArray {
        val url = URL(urlSpec)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.inputStream.use {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("${connection.responseCode}: with $urlSpec")
                }
                return it.readBytes()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getUrlString(urlSpec: String) = String(getUrlBytes(urlSpec))

    fun fetchRecentPhotos(page: Int) =
        downloadGalleryItems(buildUrl(FETCH_RECENT_METHOD, page.toString()))

    fun searchPhotos(query: String) = downloadGalleryItems(buildUrl(SEARCH_METHOD, query))

    private fun downloadGalleryItems(url: String): List<GalleryItem> {
        try {
            val jsonString = getUrlString(url)
            Log.i(LOG_TAG, jsonString)
            return parseItems(JSONObject(jsonString))
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Failed to fetch items", e)
        } catch (e: JSONException) {
            Log.e(LOG_TAG, "Failed to parse JSON", e)
        }
        return emptyList()
    }

    private fun buildUrl(method: String, query: String?): String {
        val builder = ENDPOINT.buildUpon().appendQueryParameter("method", method)
        when (method) {
            FETCH_RECENT_METHOD -> builder.appendQueryParameter("page", query)
            SEARCH_METHOD -> builder.appendQueryParameter("text", query)
        }
        return builder.build().toString()
    }

    private fun parseItems(jsonBody: JSONObject): List<GalleryItem> {
        val photoJsonArray = jsonBody.getJSONObject("photos").getJSONArray("photo")
        val type = object : TypeToken<ArrayList<GalleryItem>>() {}.type
        return ArrayList(Gson().fromJson(photoJsonArray.toString(), type) as ArrayList<GalleryItem>)
    }
}
