package com.adgstudios.cloudstream3.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.adgstudios.cloudstream3.R
import com.adgstudios.cloudstream3.SearchResponse
import com.adgstudios.cloudstream3.ui.search.SearchClickCallback
import com.adgstudios.cloudstream3.ui.search.SearchResponseDiffCallback
import com.adgstudios.cloudstream3.ui.search.SearchResultBuilder
import kotlinx.android.synthetic.main.home_result_grid.view.*

class HomeChildItemAdapter(
    val cardList: MutableList<SearchResponse>,
    val layout: Int = R.layout.home_result_grid,
    private val nextFocusUp: Int? = null,
    private val nextFocusDown: Int? = null,
    private val clickCallback: (SearchClickCallback) -> Unit,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CardViewHolder(
            LayoutInflater.from(parent.context).inflate(layout, parent, false),
            clickCallback,
            itemCount,
            nextFocusUp,
            nextFocusDown
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CardViewHolder -> {
                holder.bind(cardList[position], position)
            }
        }
    }

    override fun getItemCount(): Int {
        return cardList.size
    }

    override fun getItemId(position: Int): Long {
        return (cardList[position].id ?: position).toLong()
    }

    fun updateList(newList: List<SearchResponse>) {
        val diffResult = DiffUtil.calculateDiff(
            SearchResponseDiffCallback(this.cardList, newList)
        )

        cardList.clear()
        cardList.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    class CardViewHolder
    constructor(
        itemView: View,
        private val clickCallback: (SearchClickCallback) -> Unit,
        private val itemCount: Int,
        private val nextFocusUp: Int? = null,
        private val nextFocusDown: Int? = null,
    ) :
        RecyclerView.ViewHolder(itemView) {

        fun bind(card: SearchResponse, position: Int) {

            // TV focus fixing
            val nextFocusBehavior = when (position) {
                0 -> true
                itemCount - 1 -> false
                else -> null
            }

            SearchResultBuilder.bind(
                clickCallback,
                card,
                position,
                itemView,
                nextFocusBehavior,
                nextFocusUp,
                nextFocusDown
            )
            itemView.tag = position

            if (position == 0) { // to fix tv
                itemView.backgroundCard?.nextFocusLeftId = R.id.nav_rail_view
            }
            //val ani = ScaleAnimation(0.9f, 1.0f, 0.9f, 1f)
            //ani.fillAfter = true
            //ani.duration = 200
            //itemView.startAnimation(ani)
        }
    }
}
