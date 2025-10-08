package com.misaka.hoshinoschedule

import android.app.Application

class KiraraApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
