package za.dev.viewbinding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import za.dev.viewbinding.databinding.RecycleviewItemBinding

class MainAdapter(val taskList: List<Task>):RecyclerView.Adapter<MainAdapter.MainViewHolder>() {

    inner class MainViewHolder(val itemBinding: RecycleviewItemBinding):RecyclerView.ViewHolder(itemBinding.root) {
        fun BindItem(task: Task)
        {
            itemBinding.titleTV.text = task.title
            itemBinding.timeTV.text = task.timeStamp
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {

        return MainViewHolder(RecycleviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {

        val task = taskList[position]
        holder.BindItem(task)
    }

    override fun getItemCount(): Int {
        return taskList.size
    }
}