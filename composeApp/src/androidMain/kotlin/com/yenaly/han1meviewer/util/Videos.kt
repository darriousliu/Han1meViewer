package com.yenaly.han1meviewer.util

import androidx.fragment.app.Fragment
import com.yenaly.han1meviewer.ui.activity.AndroidMainActivity

fun Fragment.openVideo(code: String) {
    (activity as? AndroidMainActivity)?.showVideoDetailFragment(code)
}
