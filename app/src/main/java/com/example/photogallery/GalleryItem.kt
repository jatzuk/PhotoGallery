package com.example.photogallery

class GalleryItem {
    lateinit var title: String
    lateinit var id: String
    var url: String = ""

    override fun toString() = title
}
