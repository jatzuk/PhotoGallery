package com.example.photogallery

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class PhotoPageFragment : VisibleFragment() {
    private lateinit var uri: Uri
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = arguments?.getParcelable(ARG_URI)!!
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_photo_page, container, false)
        webView = v.findViewById(R.id.web_view)
        webView.apply {
            settings.javaScriptEnabled = true
            webChromeClient = object: WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    (activity as AppCompatActivity).supportActionBar?.subtitle = title
                }
            }
            webViewClient = WebViewClient()
            loadUrl(uri.toString())
        }
        return v
    }

    companion object {
        private const val ARG_URI = "photo_page_url"

        fun newInstance(uri: Uri) =
            PhotoPageFragment().apply { arguments = Bundle().apply { putParcelable(ARG_URI, uri) } }
    }
}
