package offsetclient.config

import com.google.gson.*
import offsetclient.OffsetClient.Companion.configData
import offsetclient.features.NoBlockAnimation
import java.io.File
import java.nio.charset.StandardCharsets
object ConfigManager {

    private val categories = mapOf<String, JsonElement>(
        Pair("Block Animation Blacklist", JsonArray()),
        Pair("Hidden Mod IDs", JsonArray()),
        Pair("Item Macros", JsonArray())
    )

    fun loadConfig(config: File) {
        try {
            if (!config.exists()) config.createNewFile()
            val gson = Gson().fromJson(config.readText(), JsonElement::class.java)?.asJsonObject ?: JsonObject()
            configData.clear()
            gson.entrySet().forEach { configData[it.key] = it.value }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun writeConfig(config: File) {
        try {
            config.bufferedWriter(StandardCharsets.UTF_8).run {
                write(GsonBuilder().setPrettyPrinting().create().toJson(configData))
                close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseData() {
        for ((dataCategory, type) in categories) {
            if (!configData.contains(dataCategory)) {
                configData[dataCategory] = type
            }
        }

        try {
            NoBlockAnimation.blacklist.clear()
            configData["Block Animation Blacklist"]?.asJsonArray?.forEach {
                NoBlockAnimation.blacklist.add(it.asString)
            }
        } catch (e: Exception) {
            println("Error Reading Config")
            e.printStackTrace()
        }
    }
}
