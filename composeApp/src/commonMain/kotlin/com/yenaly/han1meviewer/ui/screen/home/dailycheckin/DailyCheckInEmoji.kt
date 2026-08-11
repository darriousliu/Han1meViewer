package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import com.yenaly.han1meviewer.logic.entity.CheckInType

/**
 * 将打卡类型转换为对应 emoji。
 *
 * @param type [CheckInType.storeName] 值
 * @return 对应的 emoji 字符
 */
fun typeEmoji(type: String): String = when (type) {
    CheckInType.MASTURBATION.storeName -> "\uD83E\uDD1C"
    CheckInType.WET_DREAM.storeName -> "\uD83D\uDCA4"
    CheckInType.SEX.storeName -> "\uD83D\uDC91"
    CheckInType.ORAL.storeName -> "\uD83D\uDC45"
    CheckInType.OTHER.storeName -> "\u2753"
    else -> "\uD83D\uDCCA"
}
