package com.example.photogallery

import com.google.gson.annotations.SerializedName

class GalleryItem {
    lateinit var title: String
    lateinit var id: String

    @SerializedName("url_s")
    var url: String = ""

    override fun toString() = title
}
