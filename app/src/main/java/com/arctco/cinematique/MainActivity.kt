package com.arctco.cinematique

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import android.webkit.WebChromeClient
import android.webkit.ValueCallback
import android.net.Uri
import android.content.ActivityNotFoundException
import android.content.Intent
import android.app.DownloadManager
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.widget.Toast
import android.os.Message
import android.app.Dialog
import android.view.ViewGroup.LayoutParams
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError // Import for onReceivedError
import android.webkit.WebResourceResponse // Import for onReceivedHttpError
import android.util.Log // Import for logging

// Permissions related imports
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AlertDialog // Import for AlertDialog
import android.provider.Settings // Import for opening app settings

// Java I/O imports for handling data URIs
import java.io.FileOutputStream
import java.io.IOException
import java.io.File
import android.util.Base64 // For Base64 decoding
import java.net.URLDecoder // For more robust URL decoding
import java.text.SimpleDateFormat // For generating timestamped filenames
import java.util.Date // For generating timestamped filenames
import java.util.Locale // For generating timestamped filenames


class MainActivity : AppCompatActivity() {

    private lateinit var myWebView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null // For Android 5.0+ file chooser
    private var uploadMessage: ValueCallback<Uri>? = null // For Android 4.1-4.4 file chooser

    companion object {
        const val FILECHOOSER_RESULT_CODE = 1
        const val PERMISSION_REQUEST_CODE = 1001 // For READ/WRITE_EXTERNAL_STORAGE (legacy)
        const val MANAGE_EXTERNAL_STORAGE_REQUEST_CODE = 1002 // For MANAGE_EXTERNAL_STORAGE
        private const val TAG = "CinematiqueWebView" // Tag for logging
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myWebView = findViewById(R.id.webView)

        // Set the WebView's background color to match your app's dark theme
        myWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.backgroundapp))

        val webSettings: WebSettings = myWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true // Allow access to file system
        webSettings.allowContentAccess = true // Allow access to content URIs
        webSettings.setSupportMultipleWindows(true) // Enable support for new windows/pop-ups
        webSettings.javaScriptCanOpenWindowsAutomatically = true // Allow JS to open new windows
        webSettings.databaseEnabled = true // Enable HTML5 database storage
        webSettings.loadWithOverviewMode = true // Load pages in overview mode, and zoom out to fit the screen
        webSettings.useWideViewPort = true // Enable viewport meta tag
        webSettings.builtInZoomControls = true // Enable built-in zoom mechanisms
        webSettings.displayZoomControls = false // Hide the zoom controls
        webSettings.setSupportZoom(true) // Allow zooming

        // Set a common User-Agent string to mimic a mobile browser
        webSettings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Mobile Safari/537.36"

        // Allow mixed content (HTTP and HTTPS) for older Android versions, if needed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Enable cookies
        CookieManager.getInstance().setAcceptCookie(true)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(myWebView, true)
        }

        // Enable WebView remote debugging (for Chrome DevTools)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // This prevents links from opening in the device's default browser
        myWebView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView Error: ${error?.description} on URL: ${request?.url}")
                Toast.makeText(applicationContext, "Web page error: ${error?.description}", Toast.LENGTH_LONG).show()
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(TAG, "WebView HTTP Error: ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase} on URL: ${request?.url}")
                Toast.makeText(applicationContext, "Web page HTTP error: ${errorResponse?.statusCode}", Toast.LENGTH_LONG).show()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
                // You could potentially inject JavaScript here if needed after page load
            }

            // IMPORTANT CHANGE: Removed the broad external browser redirection for Google URLs here.
            // This allows onCreateWindow to handle them as pop-ups.
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d(TAG, "Main WebView loading URL: $url")

                // If it's a deep link or custom scheme, handle it externally if needed
                // Example: if (url.startsWith("yourappscheme://")) { /* handle intent */ return true }

                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        // Handle file uploads/imports and new windows (like Google Drive authentication)
        myWebView.webChromeClient = object : WebChromeClient() {
            // For Android 5.0+ (Lollipop)
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                val intent = fileChooserParams?.createIntent()

                if (intent != null) { // Add this null check
                    try {
                        startActivityForResult(intent, FILECHOOSER_RESULT_CODE)
                    } catch (e: ActivityNotFoundException) {
                        this@MainActivity.filePathCallback = null
                        Toast.makeText(applicationContext, "Cannot open file chooser", Toast.LENGTH_LONG).show()
                        return false
                    }
                } else {
                    // Handle the case where the intent is null (e.g., fileChooserParams was null)
                    this@MainActivity.filePathCallback = null
                    Toast.makeText(applicationContext, "Could not create file chooser intent", Toast.LENGTH_LONG).show()
                    return false
                }
                return true
            }

            // For Android 4.1 to 4.4 (KitKat)
            @Suppress("unused")
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String?, capture: String?) {
                openFileChooser(uploadMsg)
            }

            @Suppress("unused")
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String?) {
                openFileChooser(uploadMsg)
            }

            @Suppress("unused")
            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                this@MainActivity.uploadMessage = uploadMsg
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*" // Allow all file types, or specify "application/json"
                }
                startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILECHOOSER_RESULT_CODE)
            }

            // Handle new windows/pop-ups (e.g., for Google Drive OAuth)
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                val newWebView = WebView(view.context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportMultipleWindows(true)
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    // Set User-Agent for pop-up WebView as well
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Mobile Safari/537.36"
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT) // Set layout params
                }

                val dialog = Dialog(this@MainActivity).apply {
                    setContentView(newWebView)
                    // Set dialog properties to make it full screen or cover most of the screen
                    window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    show()
                }

                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        Log.d(TAG, "Pop-up WebView loading URL: $url")

                        // IMPORTANT CHANGE: Handle the redirect back to your main app/website
                        // This assumes your web app redirects back to cinematique.me after successful Google Auth
                        if (url.startsWith("https://cinematique.me")) { // Or a more specific redirect URL from your web app
                            this@MainActivity.myWebView.loadUrl(url) // Load the final URL in the main WebView
                            dialog.dismiss() // Dismiss the pop-up dialog
                            Log.d(TAG, "OAuth flow completed, redirecting to main WebView: $url")
                            return true // Indicate that we handled the URL
                        }

                        // Allow all other URLs to load within this pop-up WebView
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "Pop-up WebView finished loading: $url")
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.e(TAG, "Pop-up WebView Error: ${error?.description} on URL: ${request?.url}")
                        Toast.makeText(applicationContext, "Pop-up web page error: ${error?.description}", Toast.LENGTH_LONG).show()
                    }
                }

                newWebView.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        Log.d(TAG, "Pop-up WebView closed.")
                        dialog.dismiss()
                        super.onCloseWindow(window)
                    }
                }

                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        // Handle file downloads (export)
        myWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            if (url.startsWith("data:")) {
                // Generate a timestamped filename for JSON exports
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val exportFileName = "cinematique_export_${timestamp}.json"
                handleDataUriDownload(url, exportFileName)
            } else {
                // Use DownloadManager for HTTP/HTTPS URIs
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    // Set cookies if necessary for download to work
                    val cookies = CookieManager.getInstance().getCookie(url)
                    addRequestHeader("cookie", cookies)
                    addRequestHeader("User-Agent", userAgent)
                    setDescription("Downloading file...")
                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                    allowScanningByMediaScanner() // Allow media scanner to find the file
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED) // Show notification
                    // Set destination to public Downloads directory
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType))
                }
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                try {
                    dm.enqueue(request)
                    Toast.makeText(applicationContext, "Downloading File...", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Failed to download file: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }

        // Load your website URL
        myWebView.loadUrl("https://cinematique.me/")

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

        // Request storage permissions at runtime
        checkAndRequestPermissions()
    }

    // Handle the result of the file chooser intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILECHOOSER_RESULT_CODE) {
            // For Android 5.0+
            if (filePathCallback != null) {
                filePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
                filePathCallback = null
            }
            // For Android 4.1-4.4
            else if (uploadMessage != null) {
                // Corrected line: Handle nullable data and data.data explicitly
                uploadMessage?.onReceiveValue(if (data == null || data.data == null) null else data.data)
                uploadMessage = null
            }
        } else if (requestCode == MANAGE_EXTERNAL_STORAGE_REQUEST_CODE) {
            // This is the result from the "All files access" settings screen
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Permission granted
                    Toast.makeText(this, "All files access granted.", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied
                    Toast.makeText(this, "All files access denied. File operations may not work.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Function to check and request permissions
    private fun checkAndRequestPermissions() {
        // Check for MANAGE_EXTERNAL_STORAGE (All files access) first for API 30+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) { // API 30+
            if (Environment.isExternalStorageManager()) {
                // If MANAGE_EXTERNAL_STORAGE is granted, no need to check legacy permissions.
                // This is the key change to prevent the redundant popup.
                return
            } else {
                showManageExternalStorageRationaleDialog()
                return // Exit as we are handling this special permission
            }
        }

        // For older Android versions or if MANAGE_EXTERNAL_STORAGE is not applicable/granted,
        // proceed with READ/WRITE_EXTERNAL_STORAGE (if still relevant for your app's older API target)
        val readPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val writePermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        val shouldShowReadRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        val shouldShowWriteRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        val permissionsToRequest = mutableListOf<String>()

        if (!readPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!writePermissionGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            // If we should show a rationale (user previously denied but didn't check "don't ask again")
            if (shouldShowReadRationale || shouldShowWriteRationale) {
                showPermissionRationaleDialog()
            } else {
                // Directly request permissions (first time, or user checked "don't ask again" and we're trying again)
                ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            }
        }
    }

    // Dialog to explain why MANAGE_EXTERNAL_STORAGE is needed
    private fun showManageExternalStorageRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("All Files Access Needed")
            .setMessage("Cinematique requires 'All files access' to import and export your movie/TV data (JSON files) from anywhere on your device, and to integrate with services like Google Drive. This is a special permission on Android 11+.")
            .setPositiveButton("Go to Settings") { dialog, which ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_REQUEST_CODE)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                Toast.makeText(this, "All files access denied. File operations may not work.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    // Dialog to explain why READ/WRITE_EXTERNAL_STORAGE is needed (for older Android versions)
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Needed")
            .setMessage("Cinematique needs storage access to import and export your movie/TV data, and to handle Google Drive integrations. Please grant the permission.")
            .setPositiveButton("Grant Permission") { dialog, which ->
                // Request permissions after user agrees to rationale
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("No Thanks") { dialog, which ->
                Toast.makeText(this, "Storage permissions denied. File operations may not work.", Toast.LENGTH_LONG).show()
            }
            .show()
    }


    // Handle permission request results (for READ/WRITE_EXTERNAL_STORAGE)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "Storage permissions granted.", Toast.LENGTH_SHORT).show()
            } else {
                // Check if the user denied permanently (checked "Don't ask again")
                val showRationaleRead = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                val showRationaleWrite = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

                if (!showRationaleRead || !showRationaleWrite) {
                    // User denied with "Don't ask again" or denied multiple times.
                    // Direct them to app settings.
                    showSettingsDialog() // This dialog now points to general app settings
                } else {
                    // User denied without "Don't ask again"
                    Toast.makeText(this, "Storage permissions denied. File operations may not work.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Dialog to direct user to general app settings if permissions are permanently denied
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Storage permissions are essential for Cinematique to function fully. Please enable them in app settings.")
            .setPositiveButton("Go to Settings") { dialog, which ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, which ->
                Toast.makeText(this, "File operations will not work without storage permissions.", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    // New helper function to handle data: URIs for downloads
    private fun handleDataUriDownload(dataUri: String, fileName: String) {
        try {
            // Extract MIME type and data part
            val dataUriParts = dataUri.split(",", limit = 2)
            if (dataUriParts.size < 2) {
                Toast.makeText(this, "Invalid data URI format.", Toast.LENGTH_LONG).show()
                return
            }

            val header = dataUriParts[0] // e.g., "data:text/json;charset=utf-8" or "data:text/json;base64"
            val rawData = dataUriParts[1]

            val decodedContent: ByteArray

            if (header.contains(";base64")) {
                // If it explicitly says base64, then decode as base64
                decodedContent = Base64.decode(rawData, Base64.DEFAULT)
            } else {
                // Otherwise, assume it's URL-encoded text and decode it
                // Using URLDecoder.decode for more robust URL decoding than Uri.decode
                val urlDecodedString = URLDecoder.decode(rawData, "UTF-8")
                decodedContent = urlDecodedString.toByteArray(Charsets.UTF_8) // Convert string to bytes
            }


            // Define the download directory
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // Essential checks for directory existence and writability
            if (downloadsDir == null) {
                Toast.makeText(this, "Downloads directory not available on this device.", Toast.LENGTH_LONG).show()
                return
            }
            if (!downloadsDir.exists()) {
                if (!downloadsDir.mkdirs()) { // Attempt to create directory
                    Toast.makeText(this, "Failed to create Downloads directory.", Toast.LENGTH_LONG).show()
                    return
                }
            }
            if (!downloadsDir.canWrite()) {
                Toast.makeText(this, "Cannot write to Downloads directory. Check 'All files access' permission.", Toast.LENGTH_LONG).show()
                return
            }

            val file = File(downloadsDir, fileName)

            // If the file already exists, consider a strategy (e.g., overwrite, append unique number)
            // For now, FileOutputStream will overwrite if it exists.
            FileOutputStream(file).use { fos ->
                fos.write(decodedContent)
            }
            Toast.makeText(this, "File saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()

            // Optional: Make the file visible in gallery/file managers
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            sendBroadcast(mediaScanIntent)

        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Error decoding data URI (bad format or base64): ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } catch (e: IOException) {
            // Provide more specific error message from the IOException
            Toast.makeText(this, "Error saving file (IO Exception): ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } catch (e: SecurityException) {
            // Catch SecurityException specifically for permission issues
            Toast.makeText(this, "Security error saving file. Ensure 'All files access' is granted: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(this, "An unexpected error occurred during download: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
