package de.steeldeers.app.ui.sponsors

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.view.*
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.steeldeers.app.ui.main.MainActivity
import de.steeldeers.app.ui.main.MainNavigator
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.fragment_entries.toolbar
import kotlinx.android.synthetic.main.fragment_sponsors.*
import net.fred.feedex.R
import org.jetbrains.anko.*
import java.util.*
import android.widget.Toast
import org.jetbrains.anko.sdk21.listeners.onClick
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.content.Intent
import android.net.Uri


class SponsorFragment : Fragment(R.layout.fragment_sponsors) {

    var url: String? = null

    companion object {

        fun newInstance(): SponsorFragment {
            return SponsorFragment().apply {

            }
        }
    }

    private val navigator: MainNavigator by lazy { activity as MainNavigator }

    init {
        setHasOptionsMenu(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupRecyclerView()
        setupTitle()

        (activity as MainActivity).setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp)
        toolbar.setNavigationContentDescription(R.string.navigation_button_content_description)
        toolbar.setNavigationOnClickListener { (activity as MainActivity).toggleDrawer() }

    }

    private fun startWebView(url: String?) {
        val webView = WebView(requireContext())
        val settings: WebSettings = webView.getSettings()
        settings.javaScriptEnabled = true
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY)
        webView.getSettings().setBuiltInZoomControls(false)
        webView.getSettings().setUseWideViewPort(true)
        webView.getSettings().setLoadWithOverviewMode(true)
        val progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Lade...")
        progressDialog.show()
        webView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val clickUrl = request!!.url.toString()
                return if (clickUrl != null && (clickUrl.startsWith("http://") || clickUrl.startsWith("https://"))) {
                    view!!.context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl)))
                    true
                } else {
                    false
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                sponsors_layout.removeView(webView)
                val text = TextView(context)
                val button = Button(context)
                button.setText("Wiederholen...")
                button.onClick {
                    sponsors_layout.removeAllViews()
                    startWebView(url)
                }
                if(errorCode == ERROR_HOST_LOOKUP) {
                    text.setText("Fehler: Keine Internet Verbindung!")
                }
                else {
                    val stringCode = errorCode.toString()
                    text.setText("Fehler: $description $stringCode")
                }
                sponsors_layout.addView(text)
                sponsors_layout.addView(button)
            }
        })
        sponsors_layout.addView(webView)
        if (url != null) {
            webView.loadUrl(url)
        }

    }

    private fun setupRecyclerView() {
        startWebView(this.url)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_fragment_entries, menu)

        menu.findItem(R.id.menu_entries__share).isVisible = false
        menu.findItem(R.id.menu_entries__search).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_entries__license -> {
                navigator.goToLicense()
            }
            R.id.menu_entries__settings -> {
                navigator.goToSettings()
            }
            R.id.menu_entries__app -> {
                navigator.goToSteeldeers()
            }
        }

        return true
    }

    private fun setupTitle() {
        activity?.toolbar?.apply {
            title = ""
        }
    }
}