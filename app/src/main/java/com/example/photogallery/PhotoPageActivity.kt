package com.example.photogallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView

class PhotoPageActivity : SingleFragmentActivity() {
    private lateinit var webView: WebView

    override fun createFragment() = PhotoPageFragment.newInstance(intent.data!!)

    override fun onBackPressed() {
        webView = findViewById(R.id.web_view)
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    companion object {
        fun newIntent(context: Context, photoPageUri: Uri): Intent {
            val intent = Intent(context, PhotoPageActivity::class.java)
            intent.data = photoPageUri
            return intent
        }
    }
}
