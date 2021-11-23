package dev.koding.launcher.data.minecraft.assets

import dev.koding.launcher.data.minecraft.manifest.Asset

fun Map.Entry<String, AssetIndex.Object>.toAsset() = "${value.hash.substring(0..1)}/${value.hash}"
    .let {
        Asset(
            "https://resources.download.minecraft.net/$it",
            value.size,
            id = key,
            sha1 = value.hash,
            path = it
        )
    }