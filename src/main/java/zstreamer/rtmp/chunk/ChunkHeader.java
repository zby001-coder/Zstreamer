package zstreamer.rtmp.chunk;

/**
 * @author 张贝易
 * 分片头信息，即basicHead
 */
public class ChunkHeader {
    private int chunkStreamId;
    private int type;

    public ChunkHeader(int chunkStreamId, int type) {
        this.chunkStreamId = chunkStreamId;
        this.type = type;
    }

    public int getChunkStreamId() {
        return chunkStreamId;
    }

    public void setChunkStreamId(int chunkStreamId) {
        this.chunkStreamId = chunkStreamId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
