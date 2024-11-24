package Messages;

import java.io.Serializable;
import java.net.InetAddress;

public final class FileSearchResult implements Serializable {
    private final WordSearchMessage searchMessage;
    private byte[] hash; //file hash
    private long file_size;
    private final String file_name;
    private InetAddress address; //source address
    private int port; //source port

    public FileSearchResult(WordSearchMessage searchMessage, long file_size, String file_name, int port, byte[] hash, InetAddress address) {
        this.searchMessage = searchMessage;
        this.file_size = file_size;
        this.file_name = file_name;
        this.port = port;
        this.address = address;
        this.hash = hash;
    }

    public WordSearchMessage getSearchMessage() { return searchMessage; }

    public long getFile_size(){ return file_size; }

    public String getFile_name(){ return file_name; }

    public int getPort(){ return port; }

    public InetAddress getAddress(){ return address; }

    public byte[] getHash(){ return hash; }

}
