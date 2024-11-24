import Messages.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DealWithClient extends Thread{
    private final InetAddress inetAddress; //Client's address
    private final int port; //Client's port
    private Socket socket;
    private Node node; //Parent node
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public DealWithClient(InetAddress inetAddress, int port, Node node, Socket socket) {
        this.inetAddress = inetAddress;
        this.port = port;
        this.socket = socket;
        this.node = node;
    }

    @Override
    public String toString(){
        return "NodeAddress[address=" + socket.getInetAddress().getHostAddress() +
        ", port=" + port + "]";
    }

    //// Getters
    public InetAddress getInetAddress() {
        return inetAddress;
    }
    public int getPort() {
        return port;
    }
    ////

    //// Creates Output and Input
    private void doConnections() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    ////

    //// Sends any message/request to client Node
    public void send(Serializable message) {  //TODO Mostrar à Marta
        try {
            out.writeObject(message);
        } catch (IOException e) {
            //e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    ////

    //// Searches within the Node's file if exists any with the file_name containing the keyword
    /// not case-sensitive, compares file_name and keyword converted into lower_case
    private List<FileSearchResult> search(WordSearchMessage search){
        List<FileSearchResult> results = new ArrayList<>();
        Map<File, byte[]> files = node.getFiles();
        String keyword = search.getKeyword().toLowerCase();
        for(Map.Entry<File, byte[]> entry: files.entrySet()){
            File f = entry.getKey();
            byte[] hash = entry.getValue();
            if(f.getName().toLowerCase().contains(keyword)){
                results.add(new FileSearchResult(search, f.length(), f.getName(), node.getPort(), hash, node.getAddress()));
            }
        }
        return results;
    }
    ////

    //// Creates a FileBlockAnswerMessage with the requested block data by the FileBlockRequestMessage
    public FileBlockAnswerMessage createFileBlockAnswer(FileBlockRequestMessage request) throws IOException {
        File file = node.getFile(request.getHash());
        byte[] f = Files.readAllBytes(file.toPath());
        int offset = (int)request.getBlockOffset();
        byte[] blockData = new byte[request.getBlockLength()];
        for(int i = offset; i <blockData.length; i++){
            blockData[i] = f[offset+i];
        }
        return new FileBlockAnswerMessage(blockData,node.getSocket().getInetAddress(), node.getPort());
    }
    ////

    //// Loop that Receives Message/Request from Client Node and processes its request
    private void serve(){
        while(!interrupted()){
            try {
                Object obj = in.readObject();
                if(obj instanceof WordSearchMessage){  //Responder aos pedidos de Procura
                    List<FileSearchResult> searchResultList = search((WordSearchMessage) obj);
                    out.writeObject(new FileSearchResultList(searchResultList));
                }
                if(obj instanceof FileBlockRequestMessage){  //Responder aos pedidos de Download
                    FileBlockAnswerMessage answer = createFileBlockAnswer((FileBlockRequestMessage) obj);
                    out.writeObject(answer);
                }
                if(obj instanceof FileSearchResultList){ //Receber Resultados de Procura
                    List<FileSearchResult> list = ((FileSearchResultList)obj).getFileSearchResultList();
                    node.updateSearchList(list);
                }
            } catch (IOException e) {
                break;
            } catch (ClassNotFoundException e) {
                //e.printStackTrace();
            }
        }
    }
    ////

    @Override
    public void run(){
        try {//Conexao aceite
            System.out.println("Added new node: " + this);
            doConnections();
            serve();
        } finally {//a fechar
            try {
                socket.close();
                node.removeDealWithClient(this);
                System.out.println("Removed node: " + this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
