package com.opiumfive.plank.list

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.opiumfive.plank.MainActivity
import com.opiumfive.plank.Prefs
import com.opiumfive.plank.R

const val CAMERA_REQ_CODE = 200

class ListActivity: AppCompatActivity() {

    val adapter = ExAdapter()
    var prefs: Prefs? = null
    var empty: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        prefs = Prefs(getSharedPreferences("prefs", 0))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivityForResult(intent, CAMERA_REQ_CODE)
        }

        val list = findViewById<RecyclerView>(R.id.list)
        empty = findViewById<View>(R.id.empty)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        val lst = getCurrentInfo()
        empty?.visibility = if (lst.isNotEmpty()) View.GONE else View.VISIBLE
        adapter.setList(lst)
    }

    fun getCurrentInfo(): List<Ex> {
        val str = prefs?.stickers ?: "[]"
        val listType = object : TypeToken<List<Ex>>() {}.type
        return Gson().fromJson(str, listType)
    }

    fun saveCurrentInfo() {
        val sticks = adapter.bitmapList
        val sticksJson = Gson().toJson(sticks)
        prefs?.stickers = sticksJson
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CAMERA_REQ_CODE -> {
                val item = data?.getParcelableExtra<Ex>("item")
                if (item != null) {
                    adapter.addItem(item)
                    empty?.visibility = View.GONE
                    saveCurrentInfo()
                }
            }
        }
    }
}