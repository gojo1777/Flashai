package com.flashai.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewAssetLoader

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var cameraManager: CameraManager
    private var rearTorchCameraId: String? = null
    private var webPermissionRequest: PermissionRequest? = null

    companion object {
        private const val CAMERA_PERMISSION = 100
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        findRearTorchCamera()

        webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false

        WebView.setWebContentsDebuggingEnabled(true)

        webView.addJavascriptInterface(TorchBridge(), "AndroidTorch")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    webPermissionRequest = request
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else {
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            CAMERA_PERMISSION
                        )
                    }
                }
            }
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }
        }
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION
            )
        }
    }

    private fun findRearTorchCamera() {
        try {
            for (id in cameraManager.cameraIdList) {
                val c = cameraManager.getCameraCharacteristics(id)
                val facing = c.get(CameraCharacteristics.LENS_FACING)
                val flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (facing == CameraCharacteristics.LENS_FACING_BACK && flash) {
                    rearTorchCameraId = id
                    break
                }
            }
        } catch (_: Exception) {
            rearTorchCameraId = null
        }
    }

    inner class TorchBridge {
        @JavascriptInterface
        fun isSupported(): Boolean = rearTorchCameraId != null

        @JavascriptInterface
        fun setTorch(enabled: Boolean): Boolean {
            return try {
                val id = rearTorchCameraId ?: return false
                cameraManager.setTorchMode(id, enabled)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    override fun onDestroy() {
        try {
            rearTorchCameraId?.let { cameraManager.setTorchMode(it, false) }
        } catch (_: Exception) {}
        webView.destroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                webPermissionRequest?.grant(
                    arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                )
                webPermissionRequest = null
            } else {
                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
