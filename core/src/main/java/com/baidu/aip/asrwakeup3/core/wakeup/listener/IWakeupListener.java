package com.baidu.aip.asrwakeup3.core.wakeup.listener;

import com.baidu.aip.asrwakeup3.core.wakeup.WakeUpResult;

public interface IWakeupListener {


    void onSuccess(String word, WakeUpResult result);

    void onStop();

    void onError(int errorCode, String errorMessge, WakeUpResult result);

    void onASrAudio(byte[] data, int offset, int length);
}
