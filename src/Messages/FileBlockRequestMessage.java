package Messages;

import java.io.Serializable;

public final class FileBlockRequestMessage implements Serializable {
    private final byte[] hash;  //file hash
    private final long blockOffset; // desired block offset
    private final int blockLength; // desired block length

    public FileBlockRequestMessage(byte[] hash, long blockOffset, int blockLength) {
        this.blockOffset = blockOffset;
        this.blockLength = blockLength;
        this.hash = hash;
    }

    public long getBlockOffset(){
        return blockOffset;
    }

    public int getBlockLength(){
        return blockLength;
    }

    public byte[] getHash() {
        return hash;
    }
}
