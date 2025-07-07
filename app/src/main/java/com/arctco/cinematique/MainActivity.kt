package com.arctco.cinematique

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat // !!! Add this import !!!

class MainActivity : AppCompatActivity() {

    private lateinit var myWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myWebView = findViewById(R.id.webView)

        // !!! START OF NEW WEBVIEW BACKGROUND COLOR CODE !!!
        // Set the WebView's background color to match your app's dark theme
        myWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundapp))
        // !!! END OF NEW WEBVIEW BACKGROUND COLOR CODE !!!

        val webSettings: WebSettings = myWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // This prevents links from opening in the device's default browser
        myWebView.webViewClient = WebViewClient()

        // Load your website URL
        myWebView.loadUrl("https://cinematique.me/") // !!! REPLACE WITH YOUR ACTUAL WEBSITE URL !!!

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (myWebView.canGoBack()) {
                    myWebView.goBack() // Go back to the previous page in the WebView's history
                } else {
                    // If no more history in WebView, perform the default system back behavior
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }
}
