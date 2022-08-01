package zstreamer;

import zstreamer.httpflv.Audience;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

/**
 * @author 张贝易
 * 拉流线程池，当前只做单线程
 */
public class PullerPool {
    private static final ConcurrentLinkedQueue<Audience> AUDIENCES = new ConcurrentLinkedQueue<>();
    private static final Puller PULLER;

    static {
        PULLER = new Puller();
        PULLER.start();
    }

    public static void addAudience(Audience audience) {
        AUDIENCES.add(audience);
    }

    public static void removeAudience(Audience audience) {
        AUDIENCES.remove(audience);
    }

    public static void shutdownPuller() {
        PULLER.interrupt();
    }

    public static void wakeUpPuller() {
        LockSupport.unpark(PULLER);
    }

    /**
     * @author 张贝易
     * 拉流线程，它通过遍历所有观众的方式拉流
     */
    private static class Puller extends Thread {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                int cnt = 0;
                for (Audience audience : AUDIENCES) {
                    cnt += audience.pollMessage();
                    //如果该观众已经从直播间去除了，就从链表中删除
                    if (audience.closed()) {
                        AUDIENCES.remove(audience);
                    }
                }
                //如果当前没有推成功一个流，就阻塞住
                if (cnt == 0) {
                    LockSupport.park();
                }
            }
        }
    }
}
