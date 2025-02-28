package Messages;

import java.io.Serializable;
import java.net.InetAddress;

public final class FileBlockAnswerMessage implements Serializable {
    private final byte[] data; //file block data
    private final byte[] hash;
    private final long offset;
    private final InetAddress address; //source address
    private final int port; //source port

    public FileBlockAnswerMessage(byte[] data, byte[] hash, long offset, InetAddress address, int port) {
        this.data = data;
        this.address = address;
        this.offset = offset;
        this.port = port;
        this.hash = hash;
    }

    public byte[] getData() {
        return data;
    }

    public long getOffset() {
        return offset;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public byte[] getHash() {
        return hash;
    }
}
