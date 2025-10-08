package com.misaka.hoshinoschedule.ui

import android.content.Context
import android.content.Intent
import com.misaka.hoshinoschedule.MainActivity

object MainActivityLauncher {
    fun launchIntent(context: Context): Intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
