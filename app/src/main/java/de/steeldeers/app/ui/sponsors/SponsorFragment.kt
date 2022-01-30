package de.steeldeers.app.ui.sponsors

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import de.steeldeers.app.ui.main.MainActivity
import de.steeldeers.app.ui.main.MainNavigator
import kotlinx.android.synthetic.main.fragment_entries.*
import kotlinx.android.synthetic.main.fragment_entries.toolbar
import kotlinx.android.synthetic.main.fragment_sponsors.*
import net.fred.feedex.R
import org.jetbrains.anko.*
import java.util.*

class SponsorFragment : Fragment(R.layout.fragment_sponsors) {

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

        (activity as MainActivity).setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_menu_24dp)
        toolbar.setNavigationContentDescription(R.string.navigation_button_content_description)
        toolbar.setNavigationOnClickListener { (activity as MainActivity).toggleDrawer() }

    }

    private fun setupRecyclerView() {

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
}