package com.devdiaz.orderless

import android.app.Application
import androidx.room.Room
import com.devdiaz.orderless.data.local.BubbleDatabase
import com.devdiaz.orderless.data.repository.BubbleRepository

class BubbleApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(applicationContext, BubbleDatabase::class.java, "bubble_database")
                .build()
    }

    val repository by lazy { BubbleRepository(database.bubbleDao()) }
}
