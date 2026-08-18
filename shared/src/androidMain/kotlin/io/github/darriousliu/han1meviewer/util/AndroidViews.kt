package io.github.darriousliu.han1meviewer.util

import android.view.View
import android.view.ViewGroup

fun View.removeItself() {
    (parent as? ViewGroup)?.removeView(this)
}
