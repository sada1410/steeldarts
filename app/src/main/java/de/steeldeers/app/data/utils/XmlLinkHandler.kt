package de.steeldeers.app.data.utils

import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream

class XmlLinkHandler {
    private val links = ArrayList<Link>()
    private var link: Link? = null
    private var text: String = ""

    fun parse(inputStream: InputStream): ArrayList<Link> {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagname = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> if (tagname.equals("link", ignoreCase = true)) {
                        // create a new instance of employee
                        link = Link()
                    }
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> if (tagname.equals("link", ignoreCase = true)) {
                        // add employee object to list
                        link?.let { links.add(it) }
                    } else if (tagname.equals("title", ignoreCase = true)) {
                        link!!.title = text
                    } else if (tagname.equals("url", ignoreCase = true)) {
                        link!!.url = text
                    } else if (tagname.equals("blog", ignoreCase = true)) {
                        link!!.isBlock = if (text.toInt() == 1) true else false
                    }

                    else -> {
                    }
                }
                eventType = parser.next()
            }

        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return links
    }
}