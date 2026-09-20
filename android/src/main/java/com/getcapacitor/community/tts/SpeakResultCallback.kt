package com.getcapacitor.community.tts

public interface SpeakResultCallback {
    public fun onDone()

    public fun onError()

    public fun onRangeStart(start: Int, end: Int)
}
