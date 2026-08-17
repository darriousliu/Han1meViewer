package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.logic.model.Announcement

/** 本平台没有公告来源，首页不显示公告。 */
actual suspend fun fetchPlatformAnnouncements(): List<Announcement> = emptyList()
