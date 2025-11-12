package com.example.skaner_kodow.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import com.example.skaner_kodow.BuildConfig

object ImgurUploader {

    private const val CLIENT_ID = BuildConfig.IMGUR_CLIENT_ID


    fun uploadImage(imageFile: File, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/jpeg".toMediaType()))
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/upload")
            .addHeader("Authorization", "Client-ID $CLIENT_ID")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val imageUrl = JSONObject(responseBody).getJSONObject("data").getString("link")
                    onSuccess(imageUrl) // Zwraca link do przesłanego obrazu
                } else {
                    onError(Exception("Upload failed: ${response.message}"))
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                onError(e)
            }
        })
    }
}
