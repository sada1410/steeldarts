/*
 * Copyright (c) 2021 Daniel Saffer
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

package de.steeldeers.app.ui.about

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.vansuita.materialabout.builder.AboutBuilder
import net.fred.feedex.R
import de.steeldeers.app.utils.setupTheme

class AppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setupTheme()

        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val view = AboutBuilder.with(this)
                .setPhoto(R.drawable.steeldeersavatar)
                .addWebsiteLink("https://steeldeers.de/")
                .setCover(R.drawable.raum)
                .setName(R.string.app_name)
                .setBrief(R.string.about_screen_info)
                .setAppIcon(R.mipmap.ic_launcher_foreground)
                .setAppName(R.string.app_name)
                .addFacebookLink("steeldeershirschaid")
                .addInstagramLink("steeldeers")
                .addEmailLink("steeldeers.hirschaid@outlook.de")
                .setVersionNameAsAppSubTitle()
                .setWrapScrollView(true)
                .setLinksAnimated(false)
                .setShowAsCard(false)
                .build()

        setContentView(view)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return true
    }
}