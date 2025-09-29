package com.misaka.kiraraschedule

import android.app.Application

class KiraraApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
