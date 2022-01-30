/*
 * Copyright (c) 2012-2018 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package de.steeldeers.app.ui.main

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.rometools.opml.feed.opml.Attribute
import com.rometools.opml.feed.opml.Opml
import com.rometools.opml.feed.opml.Outline
import com.rometools.opml.io.impl.OPML20Generator
import com.rometools.rome.io.WireFeedInput
import com.rometools.rome.io.WireFeedOutput
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.view_main_drawer_header.*
import net.fred.feedex.R
import de.steeldeers.app.App
import de.steeldeers.app.data.entities.Feed
import de.steeldeers.app.data.entities.FeedWithCount
import de.steeldeers.app.data.utils.PrefConstants
import de.steeldeers.app.service.AutoRefreshJobService
import de.steeldeers.app.service.FetcherService
import de.steeldeers.app.ui.about.AppActivity
import de.steeldeers.app.ui.about.LicenseActivity
import de.steeldeers.app.ui.entries.EntriesFragment
import de.steeldeers.app.ui.entrydetails.EntryDetailsActivity
import de.steeldeers.app.ui.entrydetails.EntryDetailsFragment
import de.steeldeers.app.ui.feeds.FeedAdapter
import de.steeldeers.app.ui.feeds.FeedGroup
import de.steeldeers.app.ui.settings.SettingsActivity
import de.steeldeers.app.ui.sponsors.SponsorFragment
import de.steeldeers.app.utils.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk21.listeners.onClick
import pub.devrel.easypermissions.EasyPermissions
import java.io.*
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity(), MainNavigator, AnkoLogger {

    companion object {
        const val EXTRA_FROM_NOTIF = "EXTRA_FROM_NOTIF"

        val reqFeeds = arrayOf(
                //       Menu name     Link to feed
                arrayOf("Steeldeers", "https://steeldeers.de/feed"),
                arrayOf("Medtech-Ingenieur Software", "https://medtech-ingenieur.de/category/software/feed"),
                arrayOf("Medtech-Ingenieur Hardware", "https://medtech-ingenieur.de/category/hardware/feed"),
                arrayOf("Medtech-Ingenieur Mechanik", "https://medtech-ingenieur.de/category/mechanik/feed")
        )

        var isInForeground = false

        private const val TAG_DETAILS = "TAG_DETAILS"
        private const val TAG_MASTER = "TAG_MASTER"

        private const val OLD_GNEWS_TO_IGNORE = "http://news.google.com/news?"

        private const val WRITE_OPML_REQUEST_CODE = 2
        private const val READ_OPML_REQUEST_CODE = 3
        private const val RETRIEVE_FULLTEXT_OPML_ATTR = "retrieveFullText"

        private const val INTENT_UNREADS = "de.steeldeers.app.intent.UNREADS"
        private const val INTENT_ALL = "de.steeldeers.app.intent.ALL"
        private const val INTENT_FAVORITES = "de.steeldeers.app.intent.FAVORITES"
    }

    private val feedGroups = mutableListOf<FeedGroup>()
    private val feedAdapter = FeedAdapter(feedGroups)

    override fun onCreate(savedInstanceState: Bundle?) {
        setupNoActionBarTheme()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        more.onClick {
            it?.let { view ->
                PopupMenu(this@MainActivity, view).apply {
                    menuInflater.inflate(R.menu.menu_drawer_header, menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.menu_entries__license -> goToLicense()
                            R.id.menu_entries__settings -> goToSettings()
                            R.id.menu_entries__steeldeers -> goToSteeldeers()
                        }
                        true
                    }
                    show()
                }
            }
        }
        nav.layoutManager = LinearLayoutManager(this)
        nav.adapter = feedAdapter

        App.db.feedDao().observeAllWithCount.observe(this@MainActivity, { nullableFeeds ->
            nullableFeeds?.let { feeds ->
                val newFeedGroups = mutableListOf<FeedGroup>()
                val socialFeedGroup = mutableListOf<FeedGroup>()

                val all = FeedWithCount(feed = Feed().apply {
                    id = Feed.ALL_ENTRIES_ID
                    title = getString(R.string.all_entries)
                    groupId = 0
                    isGroup = true
                }, entryCount = feeds.sumBy { it.entryCount })
                newFeedGroups.add(FeedGroup(all, listOf()))

                val subFeedMap = feeds.groupBy { it.feed.groupId }
//                for(feed in subFeedMap) {
//                    feed.grou
//                }

                newFeedGroups.addAll(
                        subFeedMap[null]?.map { FeedGroup(it, subFeedMap[it.feed.id].orEmpty()) }.orEmpty()
                )

                val sponsors = FeedWithCount(feed = Feed().apply {
                    id = Feed.SPONSORS
                    title = "Sponsoren"
                    groupId = 1
                    isGroup = true
                }, entryCount = 0)

                val test1 = FeedWithCount(feed = Feed().apply {
                    id = Feed.SPONSORS-1
                    title = "test1"
                    groupId = 1
                    isGroup = true
                }, entryCount = 0)

                val test2 = FeedWithCount(feed = Feed().apply {
                    id = Feed.SPONSORS-2
                    title = "test2"
                    groupId = 1
                    isGroup = true
                }, entryCount = 0)

                val test = arrayListOf<FeedWithCount>()
                test.add(sponsors)
                test.add(test1)
                test.add(test2)
                socialFeedGroup.add(FeedGroup(sponsors,test))

                // Do not always call notifyParentDataSetChanged to avoid selection loss during refresh
                if (hasFeedGroupsChanged(feedGroups, newFeedGroups)) {
                    feedGroups.clear()
                    feedGroups += newFeedGroups
                    feedGroups += socialFeedGroup

                    feedAdapter.notifyParentDataSetChanged(true)

                    if (hasFetchingError()) {
                        drawer_hint.textColor = Color.RED
                        drawer_hint.textResource = R.string.drawer_fetch_error_explanation
                        toolbar.setNavigationIcon(R.drawable.ic_menu_red_highlight_24dp)
                    } else {
                        drawer_hint.textColor = Color.WHITE
                        drawer_hint.textResource = R.string.drawer_explanation
                        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp)
                    }
                }

                feedAdapter.onFeedClick { _, feedWithCount ->
                    if(feedWithCount.feed.id == Feed.SPONSORS) {
                        goToSponsors()
                        closeDrawer()
                    }
                    else {
                        goToEntriesList(feedWithCount.feed)
                        closeDrawer()
                    }
                }

                feedAdapter.onFeedLongClick { view, feedWithCount ->
                    PopupMenu(this, view).apply {
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.mark_all_as_read -> doAsync {
                                    when {
                                        feedWithCount.feed.id == Feed.ALL_ENTRIES_ID -> App.db.entryDao().markAllAsRead()
                                        feedWithCount.feed.isGroup -> App.db.entryDao().markGroupAsRead(feedWithCount.feed.id)
                                        else -> App.db.entryDao().markAsRead(feedWithCount.feed.id)
                                    }
                                }
                                R.id.enable_full_text_retrieval -> doAsync { App.db.feedDao().enableFullTextRetrieval(feedWithCount.feed.id) }
                                R.id.disable_full_text_retrieval -> doAsync { App.db.feedDao().disableFullTextRetrieval(feedWithCount.feed.id) }
                            }
                            true
                        }
                        inflate(R.menu.menu_drawer_feed)

                        when {
                            feedWithCount.feed.id == Feed.ALL_ENTRIES_ID -> {
                                menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                                menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                            }
                            feedWithCount.feed.isGroup -> {
                                menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                                menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                            }
                            feedWithCount.feed.retrieveFullText -> menu.findItem(R.id.enable_full_text_retrieval).isVisible = false
                            else -> menu.findItem(R.id.disable_full_text_retrieval).isVisible = false
                        }

                        show()
                    }
                }
            }
        })

        if (savedInstanceState == null) {
            // First open => we open the drawer for you
            if (getPrefBoolean(PrefConstants.FIRST_OPEN, true)) {
                putPrefBoolean(PrefConstants.FIRST_OPEN, false)
//                openDrawer()
//               showAlertDialog(R.string.welcome_title) { goToFeedSearch() }
                App.db.feedDao().deleteAll()
                closeDrawer()
            }

              goToEntriesList(null)
        }

        doAsync {
            if( !allFeedsAvailable() ) {
                addMissingFeeds()
            }
            removeDeprecatedFeeds()
        }

        if (getPrefBoolean(PrefConstants.REFRESH_ON_STARTUP, defValue = true)) {
            try { // Some people seems to sometimes have a IllegalStateException on this
                startService(Intent(this, FetcherService::class.java)
                        .setAction(FetcherService.ACTION_REFRESH_FEEDS)
                        .putExtra(FetcherService.FROM_AUTO_REFRESH, true))
            } catch (t: Throwable) {
                // Nothing to do, the refresh can still be triggered manually
            }
        }

        AutoRefreshJobService.initAutoRefresh(this)

        handleImplicitIntent(intent)
    }

    /**
     * @brief adds missing feeds to the database
     */
    private fun addMissingFeeds() {
        for(i in reqFeeds.indices) {
            if ( App.db.feedDao().findByLink(reqFeeds[i][1]) == null ) {
                val feedToAdd = Feed(0, reqFeeds[i][1], reqFeeds[i][0])
                App.db.feedDao().insert(feedToAdd)
            }
        }
    }

    /**
     * @brief checks if old feeds are in the database and removes them
     */
    private fun removeDeprecatedFeeds() {
        var allFeeds = App.db.feedDao().all
        for(feed in allFeeds) {
            var found = false
            for(i in reqFeeds.indices) {
                if(Arrays.stream(reqFeeds[i]).anyMatch { t -> t == feed.link }) {
                    found = true
                    break
                }
            }
            if(!found) {
                App.db.feedDao().delete(feed)
            }
        }
    }

    /**
     * @brief checks if all required feeds are in the database
     * @return true if all required feeds are available, else false
     */
    private fun allFeedsAvailable():Boolean {
        var available = true
        for(i in reqFeeds.indices) {
            if ( App.db.feedDao().findByLink(reqFeeds[i][1]) == null ) {
                available = false
            }
        }
        return available
    }

    override fun onStart() {
        super.onStart()

        if (getPrefBoolean(PrefConstants.HIDE_NAVIGATION_ON_SCROLL, false)) {
            ViewCompat.setOnApplyWindowInsetsListener(nav) { _, insets ->
                val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                nav.updatePadding(bottom = systemInsets.bottom)
                drawer.updatePadding(left = systemInsets.left, right = systemInsets.right)
                guideline.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    guideBegin = systemInsets.top
                }
                drawer_content.updatePadding(left = systemInsets.left)
                drawer_content.updateLayoutParams<DrawerLayout.LayoutParams> {
                    width = resources.getDimensionPixelSize(R.dimen.nav_drawer_width) + systemInsets.left
                }
                insets
            }
        } else {
            ViewCompat.setOnApplyWindowInsetsListener(nav, null)
            nav.updatePadding(bottom = 0)
            drawer.updatePadding(left = 0, right = 0)
            guideline.updateLayoutParams<ConstraintLayout.LayoutParams> {
                guideBegin = 0
            }
            drawer_content.updatePadding(left = 0)
            drawer_content.updateLayoutParams<DrawerLayout.LayoutParams> {
                width = resources.getDimensionPixelSize(R.dimen.nav_drawer_width)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)

        handleImplicitIntent(intent)
    }

    private fun handleImplicitIntent(intent: Intent?) {
        // Has to be called on onStart (when the app is closed) and on onNewIntent (when the app is in the background)

        // TODO delete these lines, if not required
        // Add feed urls from Open with
//        if (intent?.action.equals(Intent.ACTION_VIEW)) {
//            val search: String = intent?.data.toString()
//            DiscoverActivity.newInstance(this, search)
//            setIntent(null)
//        }
//        // Add feed urls from Share menu
//        if (intent?.action.equals(Intent.ACTION_SEND)) {
//            if (intent?.hasExtra(Intent.EXTRA_TEXT) == true) {
//                val search = intent.getStringExtra(Intent.EXTRA_TEXT)
//                if (search != null) {
//                    DiscoverActivity.newInstance(this, search)
//                }
//            }
//            setIntent(null)
//        }
//
//        // If we just clicked on the notification, let's go back to the default view
//        if (intent?.getBooleanExtra(EXTRA_FROM_NOTIF, false) == true && feedGroups.isNotEmpty()) {
//            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
//            goToEntriesList(feedGroups[0].feedWithCount.feed)
//            bottom_navigation.selectedItemId = R.id.unreads
//        }
    }

    private fun handleResumeOnlyIntents(intent: Intent?) {

        // If it comes from the All feeds App Shortcuts, select the right view
        if (intent?.action.equals(INTENT_ALL) && bottom_navigation.selectedItemId != R.id.all) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            bottom_navigation.selectedItemId = R.id.all
        }

        // If it comes from the Favorites feeds App Shortcuts, select the right view
        if (intent?.action.equals(INTENT_FAVORITES) && bottom_navigation.selectedItemId != R.id.favorites) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            bottom_navigation.selectedItemId = R.id.favorites
        }

        // If it comes from the Unreads feeds App Shortcuts, select the right view
        if (intent?.action.equals(INTENT_UNREADS) && bottom_navigation.selectedItemId != R.id.unreads) {
            feedAdapter.selectedItemId = Feed.ALL_ENTRIES_ID
            bottom_navigation.selectedItemId = R.id.unreads
        }
    }

    override fun onResume() {
        super.onResume()

        isInForeground = true
        notificationManager.cancel(0)

        handleResumeOnlyIntents(intent)
    }

    override fun onPause() {
        super.onPause()
        isInForeground = false
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        feedAdapter.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        feedAdapter.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onBackPressed() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
        } else if (toolbar.hasExpandedActionView()) {
            toolbar.collapseActionView()
        } else if (!goBack()) {
            super.onBackPressed()
        }
    }

    private fun goToSponsors() {
        clearDetails()
        containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_master)
        if (currentFragment is SponsorFragment) {
            // nothing to do
        } else {
            val master = SponsorFragment.newInstance()
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.frame_master, master, TAG_MASTER)
                    .commitAllowingStateLoss()
        }
    }

    override fun goToEntriesList(feed: Feed?) {
        clearDetails()
        containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY

        // We try to reuse the fragment to avoid loosing the bottom tab position
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frame_master)
        if (currentFragment is EntriesFragment) {
            currentFragment.feed = feed
        } else {
            val master = EntriesFragment.newInstance(feed)
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.frame_master, master, TAG_MASTER)
                    .commitAllowingStateLoss()
        }
    }
// TODO delete this line, if not required
//    override fun goToFeedSearch() = DiscoverActivity.newInstance(this)

    override fun goToEntryDetails(entryId: String, allEntryIds: List<String>) {
        closeKeyboard()

        if (containers_layout.hasTwoColumns()) {
            containers_layout.state = MainNavigator.State.TWO_COLUMNS_WITH_DETAILS
            val fragment = EntryDetailsFragment.newInstance(entryId, allEntryIds)
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(R.id.frame_details, fragment, TAG_DETAILS)
                    .commitAllowingStateLoss()

            val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as EntriesFragment
            listFragment.setSelectedEntryId(entryId)
        } else {
            if (getPrefBoolean(PrefConstants.OPEN_BROWSER_DIRECTLY, false)) {
                openInBrowser(entryId)
            } else {
                startActivity<EntryDetailsActivity>(EntryDetailsFragment.ARG_ENTRY_ID to entryId, EntryDetailsFragment.ARG_ALL_ENTRIES_IDS to allEntryIds.take(500)) // take() to avoid TransactionTooLargeException
            }
        }
    }

    override fun setSelectedEntryId(selectedEntryId: String) {
        val listFragment = supportFragmentManager.findFragmentById(R.id.frame_master) as EntriesFragment
        listFragment.setSelectedEntryId(selectedEntryId)
    }

    override fun goToLicense() {
        startActivity<LicenseActivity>()
    }

    override fun goToSteeldeers() {
        startActivity<AppActivity>()
    }

    override fun goToSettings() {
        startActivity<SettingsActivity>()
    }

    private fun openInBrowser(entryId: String) {
        doAsync {
            App.db.entryDao().findByIdWithFeed(entryId)?.entry?.link?.let { url ->
                App.db.entryDao().markAsRead(listOf(entryId))
                browse(url)
            }
        }
    }

    private fun hasFeedGroupsChanged(feedGroups: List<FeedGroup>, newFeedGroups: List<FeedGroup>): Boolean {
        if (feedGroups != newFeedGroups) {
            return true
        }

        // Also need to check all sub groups (can't be checked in FeedGroup's equals)
        feedGroups.forEachIndexed { index, feedGroup ->
            if (feedGroup.feedWithCount != newFeedGroups[index].feedWithCount || feedGroup.subFeeds != newFeedGroups[index].subFeeds) {
                return true
            }
        }

        return false
    }

    private fun hasFetchingError(): Boolean {
        // Also need to check all sub groups (can't be checked in FeedGroup's equals)
        feedGroups.forEach { feedGroup ->
            if (feedGroup.feedWithCount.feed.fetchError || feedGroup.subFeeds.any { it.feed.fetchError }) {
                return true
            }
        }

        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

//        if (requestCode == READ_OPML_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            resultData?.data?.also { uri -> importOpml(uri) }
//        } else if (requestCode == WRITE_OPML_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            resultData?.data?.also { uri -> exportOpml(uri) }
//        }
    }

    private fun closeDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.postDelayed({ drawer.closeDrawer(GravityCompat.START) }, 100)
        }
    }

    private fun openDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == false) {
            drawer?.openDrawer(GravityCompat.START)
        }
    }

    fun toggleDrawer() {
        if (drawer?.isDrawerOpen(GravityCompat.START) == true) {
            drawer?.closeDrawer(GravityCompat.START)
        } else {
            drawer?.openDrawer(GravityCompat.START)
        }
    }

    private fun goBack(): Boolean {
        if (containers_layout.state != MainNavigator.State.TWO_COLUMNS_WITH_DETAILS || containers_layout.hasTwoColumns()) return false
        if (clearDetails()) {
            containers_layout.state = MainNavigator.State.TWO_COLUMNS_EMPTY
            return true
        }
        return false
    }

    private fun clearDetails(): Boolean {
        supportFragmentManager.findFragmentByTag(TAG_DETAILS)?.let {
            supportFragmentManager
                    .beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .remove(it)
                    .commitAllowingStateLoss()
            return true
        }
        return false
    }
}
