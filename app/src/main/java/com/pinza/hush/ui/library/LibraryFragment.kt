package com.pinza.hush.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.core.widget.addTextChangedListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.pinza.hush.R
import com.pinza.hush.databinding.FragmentLibraryBinding
import com.pinza.hush.ui.adapter.LibraryViewPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LibraryFragment : Fragment(R.layout.fragment_library) {

    private val viewModel: LibraryViewModel by activityViewModels()
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private var isSearchVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLibraryBinding.bind(view)

        setupInsets()
        checkPermissionsAndScan()
        setupViewPager()
        setupListeners()
        observeUiState()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemBars.top)
            insets
        }
    }

    private fun setupListeners() {
        binding.btnSearch.setOnClickListener {
            toggleSearch()
        }

        binding.etSearch.addTextChangedListener { text ->
            // El debounce ya se hace en LibraryViewModel (compartido entre songs/
            // favoriteSongs/albums/artists), así que aquí solo se propaga el valor
            // tal cual, sin duplicar el delay.
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
        }

        binding.btnGrantPermission.setOnClickListener {
            checkPermissionsAndScan()
        }
    }

    private fun toggleSearch() {
        isSearchVisible = !isSearchVisible
        binding.etSearch.isVisible = isSearchVisible
        binding.tvTitle.isVisible = !isSearchVisible

        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (isSearchVisible) {
            binding.etSearch.requestFocus()
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.etSearch.setText("")
            viewModel.onSearchQueryChanged("")
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }

    private fun checkPermissionsAndScan() {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissionsToRequest = mutableListOf(storagePermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Optimización: si ya tenemos todos los permisos, evitamos el overhead de
        // invocar el launcher del sistema (IPC) en cada creación de la vista
        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            binding.permissionStateLayout.isVisible = false
            viewModel.scanMusic()
            return
        }

        requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }

        if (storagePermission) {
            binding.permissionStateLayout.isVisible = false
            viewModel.scanMusic()
        } else {
            binding.permissionStateLayout.isVisible = true
        }
    }

    private fun setupViewPager() {
        val adapter = LibraryViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1 // Prevent all fragments from running at once

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Pistas"
                1 -> "Favoritos"
                2 -> "Playlists"
                3 -> "Álbumes"
                4 -> "Artistas"
                else -> null
            }
        }.attach()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Solo observamos el estado de carga y si hay canciones para el empty state
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressBar.isVisible = loading
                    }
                }

                launch {
                    viewModel.songs.collect { songs ->
                        binding.emptyStateText.isVisible = songs.isEmpty() && !binding.progressBar.isVisible && !binding.permissionStateLayout.isVisible
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}