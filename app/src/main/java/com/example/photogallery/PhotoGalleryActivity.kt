package com.example.photogallery

import android.content.Context
import android.content.Intent

class PhotoGalleryActivity : SingleFragmentActivity() {
    override fun createFragment() = PhotoGalleryFragment.newInstance()

    companion object {
        fun newIntent(context: Context) = Intent(context, PhotoGalleryActivity::class.java)
    }
}
