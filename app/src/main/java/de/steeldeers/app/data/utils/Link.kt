package de.steeldeers.app.data.utils

class Link {
    var title: String = ""
    var url: String = ""
    var isBlock: Boolean = false

    override fun toString(): String {
        return "$title"
    }
}