package webtaku.hybridview

import android.content.Context
import android.graphics.Color
import org.mozilla.geckoview.BuildConfig
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class GeckoViewManager(context: Context, geckoView: GeckoView) {
    companion object {
        private const val TAG = "GeckoViewManager"
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

    private var geckoSession: GeckoSession = GeckoSession()

    init {
        geckoSession.open(getRuntime(context))
        geckoView.setSession(geckoSession)
        geckoView.coverUntilFirstPaint(Color.BLACK)
        geckoSession.loadUri("resource://android/assets/web/index.html")
    }
}
