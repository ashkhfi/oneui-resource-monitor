package com.oneui.monitor

import android.graphics.drawable.Drawable

data class ProcessModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val lastTimeUsed: Long,
    val formattedStatus: String,
    val isSystemApp: Boolean
)
