package com.opiumfive.ycupwars

import android.content.Context
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters

class UploadWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {
    override fun doWork(): Result {

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "mydb"
        ).build()

        val sharedPref = applicationContext.getSharedPreferences("mypfefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("email", "")

        if (email.isNullOrEmpty()) return Result.retry()

        val toSend = db.dataDao().getAll()

        if (toSend.isEmpty()) return Result.success()

        var success = false

        val mailService = MailService()
        //TODO body
        success = mailService.send(email, toSend)

        if (success) {
            db.dataDao().nukeTable()
            return Result.success()
        } else {
            return Result.retry()
        }
    }
}