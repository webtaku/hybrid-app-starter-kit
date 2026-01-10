package webtaku.hybridview

import android.content.Context
import android.graphics.Color
import android.util.Log
import org.json.JSONObject
import org.mozilla.geckoview.BuildConfig
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension

class GeckoViewManager(context: Context, view: GeckoView) {
    companion object {
        private const val TAG = "GeckoViewManager"
        private const val EXTENSION_ID = "nativebridge@hybridview"
        private const val EXTENSION_LOCATION = "resource://android/assets/extension/"
        private const val NATIVE_APP = "browser"

        private var runtime: GeckoRuntime? = null

        fun getRuntime(context: Context): GeckoRuntime {
            if (runtime == null) {
                val settings = GeckoRuntimeSettings.Builder().consoleOutput(true)
                    .debugLogging(BuildConfig.DEBUG).build()
                runtime = GeckoRuntime.create(context, settings)
            }
            return runtime!!
        }
    }

    private var session: GeckoSession = GeckoSession()
    private var messageDelegate = NativeMessageDelegate()
    private var port: WebExtension.Port? = null
    private val pendingToWeb = ArrayDeque<JSONObject>()

    init {
        var runtime = getRuntime(context)
        session.open(runtime)

        view.coverUntilFirstPaint(Color.BLACK)
        view.setSession(session)

        runtime.webExtensionController.ensureBuiltIn(EXTENSION_LOCATION, EXTENSION_ID)
            .accept({ extension ->
                view.post { extension?.setMessageDelegate(messageDelegate, NATIVE_APP) }
            }, { exception ->
                Log.e(TAG, "Failed to install WebExtension", exception)
            })

        session.loadUri("resource://android/assets/web/index.html")
    }

    fun sendToWeb(action: String, data: JSONObject? = null) {
        val message = JSONObject().apply {
            put("action", action)
            data?.let { put("data", it) }
        }
        val p = port
        if (p == null) {
            pendingToWeb.addLast(message)
        } else {
            p.postMessage(message)
        }
    }

    inner class NativeMessageDelegate : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            this@GeckoViewManager.port = port

            port.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val req = when (message) {
                        is JSONObject -> message
                        is String -> JSONObject(message)
                        else -> JSONObject(message.toString())
                    }
                    val action = req.optString("action", "")
                    val data = req.optJSONObject("data") ?: JSONObject()

                    Log.i(TAG, "Received message from web: $action($data)")
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@GeckoViewManager.port == port) {
                        this@GeckoViewManager.port = null
                    }
                }
            })

            while (pendingToWeb.isNotEmpty()) {
                port.postMessage(pendingToWeb.removeFirst())
            }
        }
    }
}
