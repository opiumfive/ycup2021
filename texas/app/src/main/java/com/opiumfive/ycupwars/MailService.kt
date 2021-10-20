package com.opiumfive.ycupwars

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody


class MailService {

    fun send(email: String, data: List<Data>): Boolean {

        if (data.isEmpty()) return true

        val client = OkHttpClient()

        val mediaType = "application/json".toMediaTypeOrNull()

        val mapData = data.map { SendData(Coordinates(it.lat ?: 0.0, it.lng ?: 0.0), Objects(Object(it.type ?: "", it.count ?: 0))) }

        if (mapData.isEmpty()) return true

        var toJsonString = Gson().toJson(mapData, object : TypeToken<List<SendData>>() {}.type)

        if (toJsonString == "[]") return true

        toJsonString = "Mail api к сожалению отказалось посылать json ${data.map { Type.valueOf(it.type!!).nn + " " + it.count + " шт. " + it.lat + " " + it.lng + "" }.joinToString(" ")}"
        toJsonString = toJsonString.replace(".", " ").replace(",", " ")

        val body =
            "{\r\"personalizations\": [\r{\r\"to\": [\r{\r\"email\": \"$email\"\r }\r],\r\"subject\": \"Показания\"\r }\r],\r\"from\": {\r\"email\": \"from_address@example.com\"\r },\r\"content\": [\r{\r\"type\": \"text/plain\",\r\"value\": \"$toJsonString\"\r }\r]\r}".toRequestBody(
                mediaType
            )

        val request = Request.Builder()
            .url("https://rapidprod-sendgrid-v1.p.rapidapi.com/mail/send")
            .post(body)
            .addHeader("content-type", "application/json")
            .addHeader("x-rapidapi-host", "rapidprod-sendgrid-v1.p.rapidapi.com")
            .addHeader("x-rapidapi-key", "ead04a3a00msh442ddf1888e702cp1a43ddjsn586a9083ebb8")
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            null
        }

        return response?.isSuccessful == true
    }
}