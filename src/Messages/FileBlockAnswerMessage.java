package Messages;

import java.io.Serializable;
import java.net.InetAddress;

public final class FileBlockAnswerMessage implements Serializable {
    private final byte[] data; //file block data
    private final InetAddress address; //source address
    private final int port; //source port

    public FileBlockAnswerMessage(byte[] data , InetAddress address, int port) {
        this.data = data;
        this.address = address;
        this.port = port;
    }
    public byte[] getData() {
        return data;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
