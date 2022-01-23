package de.steeldeers.app.ui.discover

import android.view.View
import de.steeldeers.app.data.entities.SearchFeedResult

interface FeedManagementInterface {

    fun searchForFeed(query: String)

    fun addFeed(title: String, link: String)

    fun deleteFeed(view: View, feed: SearchFeedResult)

    fun previewFeed(view: View, feed: SearchFeedResult)
}