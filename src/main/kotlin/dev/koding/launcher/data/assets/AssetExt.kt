package dev.koding.launcher.data.assets

import dev.koding.launcher.data.manifest.Asset

fun Map.Entry<String, AssetObject>.toAsset() = "${value.hash.substring(0..1)}/${value.hash}"
    .let {
        Asset(
            "https://resources.download.minecraft.net/$it",
            value.size,
            id = key,
            sha1 = value.hash,
            path = it
        )
    }