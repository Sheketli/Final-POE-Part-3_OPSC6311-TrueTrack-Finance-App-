package com.example.truetrackfinance.ui.achievements

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.truetrackfinance.R
import com.example.truetrackfinance.data.db.entity.Badge
import com.example.truetrackfinance.data.model.BadgeKey
import com.example.truetrackfinance.databinding.ItemBadgeAchievementBinding
import com.example.truetrackfinance.util.DateUtil

/**
 * BadgeAdapter displays the list of achievements on the Achievements screen.
 */
class BadgeAdapter : ListAdapter<Badge, BadgeAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(private val binding: ItemBadgeAchievementBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(badge: Badge) {
            val key = BadgeKey.entries.find { it.key == badge.badgeKey } ?: return
            
            binding.tvBadgeName.text = key.displayName
            binding.tvBadgeDescription.text = key.description
            
            val isEarned = badge.earnedAt != null
            if (isEarned) {
                binding.tvBadgeStatus.text = "Earned"
                binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.success))
                binding.tvEarnedDate.text = DateUtil.formatDisplay(badge.earnedAt!!)
                binding.tvEarnedDate.visibility = View.VISIBLE
                binding.cardBadgeIcon.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.forest_green_light))
                binding.ivBadgeIcon.alpha = 1.0f
            } else {
                binding.tvBadgeStatus.text = "Locked"
                binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_secondary))
                binding.tvEarnedDate.visibility = View.GONE
                binding.cardBadgeIcon.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.input_background))
                binding.ivBadgeIcon.alpha = 0.3f
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBadgeAchievementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Badge>() {
            override fun areItemsTheSame(a: Badge, b: Badge) = a.id == b.id
            override fun areContentsTheSame(a: Badge, b: Badge) = a == b
        }
    }
}
