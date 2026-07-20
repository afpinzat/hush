package com.pinza.hush.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pinza.hush.ui.library.AlbumsFragment
import com.pinza.hush.ui.library.ArtistsFragment
import com.pinza.hush.ui.library.SongsFragment
import com.pinza.hush.ui.playlist.PlaylistsFragment

class LibraryViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 5 // Listas, Favoritos, Pistas, Álbumes, Artistas

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SongsFragment()
            1 -> com.pinza.hush.ui.library.FavoritesFragment()
            2 -> PlaylistsFragment()
            3 -> AlbumsFragment()
            4 -> ArtistsFragment()
            else -> SongsFragment()
        }
    }
}
