package com.example.photogallery

class PhotoGalleryActivity: SingleFragmentActivity() {
    override fun createFragment() = PhotoGalleryFragment.newInstance()
}
