package com.arthur.arcade.data

import android.graphics.drawable.Drawable

data class GameProfile(
	var doNotDisturbOn: Boolean? = null,
	var blockInternet: Boolean = false,
)

data class GameApp(
	var name: String,
	val packageName: String,
	val icon: Drawable,
	var profile: GameProfile
)