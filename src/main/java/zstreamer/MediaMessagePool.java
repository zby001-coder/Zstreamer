package zstreamer;

import zstreamer.httpflv.Audience;
import zstreamer.rtmp.Streamer;
import zstreamer.rtmp.message.messageType.media.DataMessage;
import zstreamer.rtmp.message.messageType.media.MediaMessage;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 张贝易
 * 保存各个主播的媒体流
 */
public class MediaMessagePool {
    /**
     * 这里使用ConcurrentHashMap提升并发效率
     * key是房间id，value是它的流
     */
    private static final ConcurrentHashMap<String, MediaStream> POOL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Streamer> STREAMER = new ConcurrentHashMap<>();

    /**
     * 由于只有一个主播可以推流，所以这里不用加锁
     *
     * @param roomName 主播的房间号(串流id)
     * @param message  媒体信息
     */
    public static void pushMediaMessage(String roomName, MediaMessage message, Streamer streamer) {
        POOL.get(roomName).pushMessage(message,
                streamer.getMetaData(),
                streamer.getAac(),
                streamer.getAvc(),
                streamer.getSei());
    }

    public static Streamer getStreamer(String roomName){
        return STREAMER.get(roomName);
    }

    public static boolean registerAudience(String roomName, Audience audience) {
        Streamer streamer = STREAMER.get(roomName);
        if (streamer != null) {
            return streamer.registerAudience(audience);
        }
        return false;
    }

    public static void unRegisterAudience(String roomName, Audience audience) {
        Streamer streamer = STREAMER.get(roomName);
        if (streamer != null) {
            streamer.unregisterAudience(audience);
        }
    }

    public static void createRoom(String roomName, Streamer streamer) {
        POOL.put(roomName, new MediaStream());
        STREAMER.put(roomName, streamer);
    }

    public static void closeRoom(String roomName) throws IOException {
        POOL.remove(roomName);
        Streamer streamer = STREAMER.get(roomName);
        if (streamer != null) {
            streamer.doCloseRoom();
        }
        STREAMER.remove(roomName);
    }

    public static boolean hasRoom(String roomName) {
        return POOL.containsKey(roomName);
    }

    public static Node pullMediaMessage(String roomName, int timeStamp) throws Exception {
        if (hasRoom(roomName)) {
            MediaStream mediaStream = POOL.get(roomName);
            //这里用双重判定也是为了处理同名streamer互相顶掉的问题
            if (mediaStream != null) {
                return mediaStream.pullMessage(timeStamp);
            }
        }
        throw new Exception("Room Doesnt Exist!");
    }

    public static Node pullTailMessage(String roomName) throws Exception {
        if (hasRoom(roomName)) {
            MediaStream mediaStream = POOL.get(roomName);
            //这里用双重判定也是为了处理同名streamer互相顶掉的问题
            if (mediaStream != null) {
                return mediaStream.getTail();
            }
        }
        throw new Exception("Room Doesnt Exist!");
    }

    /**
     * @author 张贝易
     * 媒体流，使用链表保存媒体流信息
     * 设置链表为固定大小，限制内存占用
     * 离开窗口的节点并不会释放next，这让已经拉到流的观众可以一直向下拉流
     * 当某个节点离开窗口，而且它和它之前的节点没有观众持有，那就会被回收
     * 这样的模型是为了不用加锁，而且不会内存泄露
     * 如果需要流量控制，可以在puller线程中记录一轮pull中最早的node，在streamer中记录最晚的node，当两个node差距过大时暂停推流
     */
    private static class MediaStream {
        /**
         * head作为哑节点
         */
        private Node head = new Node(null, null, null, null);
        private Node tail = head;
        private static final int WINDOW_SIZE = 30;
        private int presentSize = 0;

        /**
         * 由于只有一个主播推流，所以不用加锁
         *
         * @param message 要推的媒体信息
         */
        public void pushMessage(MediaMessage message, DataMessage metaData, MediaMessage aac, MediaMessage avc, MediaMessage sei) {
            tail.next = new Node(message, metaData, aac, avc, sei);
            tail = tail.next;
            if (presentSize == WINDOW_SIZE) {
                head = head.next;
                presentSize--;
            }
            presentSize++;
        }

        /**
         * 将当前窗口内最接近timeStamp的媒体节点返回
         * 由于观众拉流不需要精准的窗口，所以可以不用加锁，即在一个观众拉流的过程中，窗口是可以变化的
         *
         * @param time 期望的时间戳
         * @return 拉到的媒体节点
         */
        public Node pullMessage(int time) {
            if (presentSize == 0) {
                return head;
            }
            //获取当前窗口内部里大于等于time的第一个节点，如果没有就返回tail
            Node now = head;
            Node end = tail;
            while (now != end) {
                now = now.next;
                if (now.message.getTimeStamp() >= time) {
                    break;
                }
            }
            return now;
        }

        public Node getTail() {
            return tail;
        }
    }

    /**
     * @author 张贝易
     * 媒体节点，内部保存着媒体信息
     * 整个媒体流使用链表，每个节点保存后一个节点
     * 一旦拉到了某个节点，就可以一直往后拉
     */
    public static class Node {
        private Node next;
        /**
         * 当前tag的信息
         */
        private final MediaMessage message;
        /**
         * 整个流的元信息
         */
        private final DataMessage metaData;

        /**
         * 音频编解码元信息
         */
        private final MediaMessage aacSequenceHeader;
        /**
         * 视频编解码元信息
         */
        private final MediaMessage avcSequenceHeader;

        /**
         * 视频编码辅助信息，它不是必须的
         */
        private final MediaMessage sei;

        public Node(DataMessage dataMessage, MediaMessage aac, MediaMessage avc, MediaMessage sei) {
            this.message = null;
            this.metaData = dataMessage;
            this.aacSequenceHeader = aac;
            this.avcSequenceHeader = avc;
            this.sei = sei;
        }

        public Node(MediaMessage message, DataMessage dataMessage, MediaMessage aac, MediaMessage avc, MediaMessage sei) {
            this.metaData = dataMessage;
            this.message = message;
            this.aacSequenceHeader = aac;
            this.avcSequenceHeader = avc;
            this.sei = sei;
        }

        public Node(Node next, MediaMessage message, DataMessage dataMessage, MediaMessage aac, MediaMessage avc, MediaMessage sei) {
            this.metaData = dataMessage;
            this.next = next;
            this.message = message;
            this.aacSequenceHeader = aac;
            this.avcSequenceHeader = avc;
            this.sei = sei;
        }

        public boolean hasNext() {
            return next != null;
        }

        public Node getNext() {
            return next;
        }

        public MediaMessage getMessage() {
            return message;
        }

        public DataMessage getMetaData() {
            return metaData;
        }

        public MediaMessage getAacSequenceHeader() {
            return aacSequenceHeader;
        }

        public MediaMessage getAvcSequenceHeader() {
            return avcSequenceHeader;
        }

        public MediaMessage getSei() {
            return sei;
        }
    }
}
