package com.evenfall.app.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import com.evenfall.app.MainActivity
import com.evenfall.app.R
import com.evenfall.app.data.db.entity.BookEntity

object EvenfallShortcutManager {

    const val ACTION_CONTINUE_BOOK = "com.evenfall.app.action.CONTINUE_BOOK"
    const val ACTION_OPEN_LIBRARY = "com.evenfall.app.action.OPEN_LIBRARY"
    const val EXTRA_BOOK_ID = "com.evenfall.app.extra.BOOK_ID"

    const val ID_CONTINUE_BOOK = "shortcut_continue_book"
    const val ID_LIBRARY = "shortcut_library"

    fun updateShortcuts(context: Context, book: BookEntity?) {
        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            val maxShortcuts = shortcutManager.maxShortcutCountPerActivity
            val shortcuts = mutableListOf<ShortcutInfo>()

            if (book != null) {
                val title = book.displayTitle
                val shortTitle = if (title.length > 18) title.take(15) + "…" else title
                val continueShortcut = ShortcutInfo.Builder(context, ID_CONTINUE_BOOK)
                    .setShortLabel(context.getString(R.string.shortcut_continue, shortTitle))
                    .setLongLabel(context.getString(R.string.shortcut_continue, title))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_play))
                    .setIntent(
                        Intent(context, MainActivity::class.java).apply {
                            action = ACTION_CONTINUE_BOOK
                            putExtra(EXTRA_BOOK_ID, book.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    )
                    .setRank(0)
                    .build()
                shortcuts.add(continueShortcut)
            }

            val libraryShortcut = ShortcutInfo.Builder(context, ID_LIBRARY)
                .setShortLabel(context.getString(R.string.shortcut_library))
                .setLongLabel(context.getString(R.string.shortcut_library))
                .setIcon(Icon.createWithResource(context, R.drawable.ic_shortcut_library))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = ACTION_OPEN_LIBRARY
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                .setRank(1)
                .build()
            shortcuts.add(libraryShortcut)

            shortcutManager.dynamicShortcuts = shortcuts.take(maxShortcuts)
        } catch (_: Exception) {
            // Ignore failures if shortcuts not supported on custom launchers
        }
    }
}
