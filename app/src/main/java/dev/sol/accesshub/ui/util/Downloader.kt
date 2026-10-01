package dev.sol.accesshub.ui.util

import dev.sol.accesshub.accessHubApp
import okhttp3.Request

fun checkNewVersion(): LatestVersionInfo {
    if (!isNetworkAvailable(accessHubApp)) return LatestVersionInfo()
    val url = "https://api.github.com/repos/Shimmerfly/AccessHub/releases/latest"
    // default null value if failed
    val defaultValue = LatestVersionInfo()
    runCatching {
        accessHubApp.okhttpClient.newCall(Request.Builder().url(url).build()).execute()
            .use { response ->
                if (!response.isSuccessful) {
                    return defaultValue
                }
                val body = response.body.string()
                val json = org.json.JSONObject(body)
                val changelog = json.optString("body")

                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (!name.endsWith(".apk")) {
                        continue
                    }

                    val regex = Regex("v(.+?)_(\\d+)-")
                    val matchResult = regex.find(name) ?: continue
                    matchResult.groupValues[1]
                    val versionCode = matchResult.groupValues[2].toInt()
                    val downloadUrl = asset.getString("browser_download_url")

                    return LatestVersionInfo(
                        versionCode,
                        downloadUrl,
                        changelog
                    )
                }

            }
    }
    return defaultValue
}
