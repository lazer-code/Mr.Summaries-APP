package com.example.mrsummaries_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mrsummaries_app.R
import com.example.mrsummaries_app.models.Course

class CourseAdapter(
    private val courses: List<Course>,
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseIcon: ImageView = view.findViewById(R.id.course_icon)
        val courseName: TextView = view.findViewById(R.id.course_name)
        val courseCode: TextView = view.findViewById(R.id.course_code)
        val summariesCount: TextView = view.findViewById(R.id.summaries_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_course, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]

        holder.courseName.text = course.name
        holder.courseCode.text = course.courseCode
        holder.summariesCount.text = "${course.summariesCount} summaries"

        if (course.iconResId != 0) {
            holder.courseIcon.setImageResource(course.iconResId)
        } else {
            holder.courseIcon.setImageResource(R.drawable.ic_course_default)
        }

        holder.itemView.setOnClickListener {
            onCourseClick(course)
        }
    }

    override fun getItemCount() = courses.size
}