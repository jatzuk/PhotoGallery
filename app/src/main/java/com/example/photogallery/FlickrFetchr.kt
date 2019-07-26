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

object FlickrFetchr {
    private const val API_KEY = "3df4bf25938544544d17ee089e0979a3"
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

    fun fetchItems(page: Int): List<GalleryItem> {
        try {
            val jsonString = getUrlString(
                Uri.parse("https://www.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("page", page.toString())
                    .build().toString()
            )
            Log.i(LOG_TAG, jsonString)
            return parseItems(JSONObject(jsonString))
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Failed to fetch items", e)
        } catch (e: JSONException) {
            Log.e(LOG_TAG, "Failed to parse JSON", e)
        }
        return emptyList()
    }

    private fun parseItems(jsonBody: JSONObject): List<GalleryItem> {
        val photoJsonArray = jsonBody.getJSONObject("photos").getJSONArray("photo")
        val type = object : TypeToken<ArrayList<GalleryItem>>() {}.type
        return ArrayList(Gson().fromJson(photoJsonArray.toString(), type) as ArrayList<GalleryItem>)
    }
}
