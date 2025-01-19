package im.dacer.aifeeder.utils

import im.dacer.aifeeder.BuildConfig
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

fun makeRequestFromHar(harContent: String) {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Parse the HAR content using JSONObject
    val harJson = JSONObject(harContent)
    val requestJson = harJson
        .getJSONObject("log")
        .getJSONArray("entries")
        .getJSONObject(0)
        .getJSONObject("request")

    // Extract HTTP method, URL, headers, and body
    val method = requestJson.getString("method")
    val url = requestJson.getString("url")

    // Build headers
    val headers = Headers.Builder()
    val headersArray = requestJson.getJSONArray("headers")
    for (i in 0 until headersArray.length()) {
        val header = headersArray.getJSONObject(i)
        headers.add(header.getString("name"), header.getString("value"))
    }

    // Get the request body if it exists
    val postData = requestJson.optJSONObject("postData")
    val requestBody = if (postData != null) {
        val mimeType = postData.getString("mimeType")
        val bodyText = postData.getString("text")
        RequestBody.create(mimeType.toMediaType(), bodyText)
    } else {
        null
    }

    // Create the request
    val requestBuilder = Request.Builder()
        .url(url)
        .headers(headers.build())

    if (method.equals("POST", ignoreCase = true)) {
        requestBuilder.post(requestBody!!)
    } else if (method.equals("GET", ignoreCase = true)) {
        requestBuilder.get()
    } // Handle other HTTP methods if necessary

    val request = requestBuilder.build()

    // Execute the request
    Thread {
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                println("Response: ${response.body?.string()}")
            } else {
                println("Error: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}

fun feedPet() {
    makeRequestFromHar(BuildConfig.FEED_PET_HAR)
}
