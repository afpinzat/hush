package com.pinza.hush.ui.queue

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.pinza.hush.R
import com.pinza.hush.databinding.FragmentQueueBinding
import com.pinza.hush.ui.adapter.QueueAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QueueFragment : Fragment(R.layout.fragment_queue) {

    private val viewModel: QueueViewModel by activityViewModels()
    private var _binding: FragmentQueueBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: QueueAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentQueueBinding.bind(view)

        setupRecyclerView()
        observeQueue()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        
        binding.btnSaveQueue.setOnClickListener {
            // Lógica para guardar el nuevo orden si fuera persistente
            // Por ahora ExoPlayer ya lo tiene en memoria tras el movimiento
            findNavController().popBackStack()
        }
    }

    private fun setupRecyclerView() {
        adapter = QueueAdapter(
            onSongClick = { song -> viewModel.playSpecificSong(song) },
            onRemoveClick = { song -> viewModel.removeFromQueue(song) }
        )

        binding.rvQueue.adapter = adapter
        binding.rvQueue.layoutManager = LinearLayoutManager(requireContext())

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = vh.adapterPosition
                val toPos = target.adapterPosition
                
                // 1. Mover visualmente en el adapter
                adapter.onItemMove(fromPos, toPos)
                
                // 2. Mover físicamente en ExoPlayer
                viewModel.moveItem(fromPos, toPos)

                return true
            }
            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) {}
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.rvQueue)
    }

    private fun observeQueue() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.songs)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
