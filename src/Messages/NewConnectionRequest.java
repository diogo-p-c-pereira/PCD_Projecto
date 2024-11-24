package Messages;

import java.io.Serializable;
import java.net.InetAddress;

public final class NewConnectionRequest implements Serializable {
    private final InetAddress address; //source address
    private final int port; //source port

    public NewConnectionRequest(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }
    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
