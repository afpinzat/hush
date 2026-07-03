package com.pinza.hush.ui.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.pinza.hush.databinding.ActivityQueueBinding
import com.pinza.hush.ui.adapter.QueueAdapter
import com.pinza.hush.ui.adapter.QueueItemTouchHelper
import com.pinza.hush.ui.viewmodel.QueueViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QueueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQueueBinding
    private val viewModel: QueueViewModel by viewModels()
    private lateinit var adapter: QueueAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQueueBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Cola de Reproducción"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = QueueAdapter(
            onSongClick = { item ->
                viewModel.playQueueItem(item)
                finish()
            },
            onRemoveClick = { item ->
                viewModel.removeFromQueue(item)
            },
            onStartDrag = { viewHolder ->
                touchHelper.startDrag(viewHolder)
            }
        )

        binding.rvQueue.apply {
            layoutManager = LinearLayoutManager(this@QueueActivity)
            adapter = this@QueueActivity.adapter
        }

        touchHelper = ItemTouchHelper(
            QueueItemTouchHelper(
                onItemMove = { from, to ->
                    viewModel.moveItem(from, to)
                    true
                },
                onItemDismiss = { position ->
                    // ✅ Usar getItemAt() en lugar de getItem()
                    val item = adapter.getItemAt(position)
                    item?.let { viewModel.removeFromQueue(it) }
                }
            )
        )
        touchHelper.attachToRecyclerView(binding.rvQueue)
    }

    private fun setupListeners() {
        binding.btnClearQueue.setOnClickListener {
            viewModel.clearQueue()
        }

        binding.btnReorder.setOnClickListener {
            viewModel.saveQueueOrder()
            android.widget.Toast.makeText(this, "Orden guardado", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.queueItems.collect { items ->
                adapter.submitList(items)
                binding.tvEmptyQueue.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Mostrar/ocultar loading
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { error ->
                error?.let {
                    android.widget.Toast.makeText(this@QueueActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}