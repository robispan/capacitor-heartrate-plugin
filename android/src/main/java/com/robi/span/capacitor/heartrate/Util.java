package com.robi.span.capacitor.heartrate;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class Util {
    public static void setTimeout(Runnable runnable, int delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            } catch (Exception e) {
                System.err.println(e);
            }
        }).start();
    }

    public static void setInterval(Runnable runnable, int interval) {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        }, 0, interval);
    }

    public static byte[] toByteArray(ByteBuffer $this$toByteArray) {
        $this$toByteArray.rewind();
        byte[] data = new byte[$this$toByteArray.remaining()];
        $this$toByteArray.get(data);
        return data;
    }

}

