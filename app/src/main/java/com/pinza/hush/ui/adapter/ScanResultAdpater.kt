package com.pinza.hush.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinza.hush.data.local.model.ScanResult
import com.pinza.hush.databinding.ItemScanResultBinding
import java.text.SimpleDateFormat
import java.util.*

class ScanResultAdapter : ListAdapter<ScanResult, ScanResultAdapter.ScanResultViewHolder>(
    ScanResultDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultViewHolder {
        val binding = ItemScanResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanResultViewHolder, position: Int) {
        val result = getItem(position)
        holder.bind(result)
    }

    inner class ScanResultViewHolder(
        private val binding: ItemScanResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: ScanResult) {
            binding.apply {
                tvScanDate.text = formatDate(result.scanDate)
                tvTotalFound.text = "${result.totalFound} canciones"
                tvStatus.text = result.status
            }
        }

        private fun formatDate(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return format.format(date)
        }
    }

    class ScanResultDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem == newItem
        }
    }
}