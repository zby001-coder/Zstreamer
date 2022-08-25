package zstreamer.http.service.httpflv.flv;

/**
 * @author 张贝易
 * FLV文件的头部，在尾部包含prevLen信息
 */
public class FlvHeader {
    /**
     * Flv头必须以FLV开始
     * version：版本号，都是1
     * audioFlag：是否有音频
     * videoFlag：是否有视频
     * DATA_OFFSET：头的长度，都是9
     */
    private static final String START_WITH = "FLV";
    private static final byte VERSION = 1;
    private static final int DATA_OFFSET = 9;
    private static final byte AUDIO_MOVE = 2;
    private static final byte VIDEO_MOVE = 0;
    private static final byte AUDIO_MASK = 0X4;
    private static final byte VIDEO_MASK = 0X1;
    private final byte audioFlag;
    private final byte videoFlag;

    public byte[] generateHeader() {
        //不仅为header留空间，还为prevLen留空间，可以减少一次复制
        byte[] bytes = new byte[9 + 4];
        int idx = 0;
        for (; idx < 3; idx++) {
            bytes[idx] = (byte) START_WITH.charAt(idx);
        }
        bytes[idx] = VERSION;
        idx++;
        //这个byte决定是有音频/视频/音视频
        bytes[idx] = (byte) (((audioFlag << AUDIO_MOVE) & AUDIO_MASK) | ((videoFlag << VIDEO_MOVE) & VIDEO_MASK));
        bytes[8] = DATA_OFFSET;
        return bytes;
    }

    public FlvHeader(byte audioFlag, byte videoFlag) {
        this.audioFlag = audioFlag;
        this.videoFlag = videoFlag;
    }
}
