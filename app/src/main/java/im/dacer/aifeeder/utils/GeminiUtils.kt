package im.dacer.aifeeder.utils

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import im.dacer.aifeeder.BuildConfig

suspend fun isFeederLowOnFood(bitmap: Bitmap): Boolean {
    val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
    )

    val inputContent = content {
        image(bitmap)
        text(
            """
            I sent you a top-down photo of a pet feeder. The photo may include:
            - A white feeder body and a food bowl
            - Brown pet food
            - Possibly a pet or other irrelevant items like flooring
            
            You need to determine if the pet needs more food.  
            
            1. Return **true** if (and only if) you can clearly see the bowl and confirm that it is at most 20% full.  
            2. Return **false** if:
               - The bowl is more than 20% full, OR
               - There is a pet actively eating from or otherwise blocking the bowl, OR
               - You cannot clearly identify the bowl or the amount of food (e.g., due to darkness, poor lighting, occlusion), OR
               - The photo is too dark or unclear to see the contents of the bowl.  
            
            Answer **only** with **true** or **false**, no additional explanation.
            """.trimIndent()
        )
    }

    val response = generativeModel.generateContent(inputContent)
    val outputText = response.text?.trim()

    Log.d("PetFeeder", "AI response: $outputText")

    return outputText.equals("true", ignoreCase = true)
}
