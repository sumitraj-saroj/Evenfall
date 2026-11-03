package com.evenfall.app.playback

import com.evenfall.app.R

enum class HeadsetAction(val labelResId: Int) {
    NEXT_CHAPTER(R.string.action_next_chapter),
    PREVIOUS_CHAPTER(R.string.action_prev_chapter),
    SKIP_FORWARD(R.string.action_skip_forward),
    SKIP_BACK(R.string.action_skip_back),
    SAVE_BOOKMARK(R.string.action_save_bookmark),
    TOGGLE_SLEEP_TIMER(R.string.action_toggle_sleep),
    CHANGE_SPEED(R.string.action_change_speed),
    NONE(R.string.action_none)
}

enum class HeadsetGesture {
    SINGLE_PRESS,
    DOUBLE_PRESS,
    TRIPLE_PRESS,
    LONG_PRESS
}
