package com.yenaly.han1meviewer.ui.navigation.main

import com.yenaly.han1meviewer.datetime.shiftYearMonthCode

internal fun shiftMonthCodeForPreview(code: String, delta: Int): String {
    return shiftYearMonthCode(code, delta)
}
