import Messages.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DealWithClient extends Thread{
    private static final int THREADPOOL_SIMULTANEOUS_THREADS = 5;

    private final InetAddress inetAddress; //Client's address
    private final int port; //Client's port
    private final Socket socket;
    private final Node node; //Parent node
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private ExecutorService pool;

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
    public synchronized void send(Serializable message) {
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
    public FileBlockAnswerMessage createFileBlockAnswer(FileBlockRequestMessage request) {
        File file = node.getFile(request.getHash());
        try {
            byte[] f = Files.readAllBytes(file.toPath());
            int offset = (int)request.getBlockOffset();
            byte[] blockData = new byte[(int)request.getBlockLength()];
            for(int i = 0; i <blockData.length; i++){
                blockData[i] = f[offset+i];
            }
            return new FileBlockAnswerMessage(blockData, request.getHash(), request.getBlockOffset(), node.getAddress() , node.getPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    ////

    //// Loop that Receives Message/Request from Client Node and processes its request
    /// each request is executed in its own thread with is submitted to the ThreadPool
    private void serve(){
        pool = Executors.newFixedThreadPool(THREADPOOL_SIMULTANEOUS_THREADS);
        while(!interrupted()){
            try {
                Object obj = in.readObject();
                if(obj instanceof WordSearchMessage search){  //Responder aos pedidos de Procura
                    Thread t = new Thread(() -> {
                        List<FileSearchResult> searchResultList = search(search);
                        send(new FileSearchResultList(searchResultList));
                    });
                    pool.submit(t);
                }
                if(obj instanceof FileBlockRequestMessage request){  //Responder aos pedidos de Download
                    Thread t = new Thread(() -> {
                        FileBlockAnswerMessage answer = createFileBlockAnswer(request);
                        send(answer);
                    });
                    pool.submit(t);
                }
                if(obj instanceof FileSearchResultList resultList){ //Receber Resultados de Procura
                    Thread t = new Thread(() -> {
                        List<FileSearchResult> list = resultList.getFileSearchResultList();
                        node.updateSearchList(list);
                    });
                    pool.submit(t);
                }
                if(obj instanceof FileBlockAnswerMessage answer){ //Receber Downloads
                    Thread t = new Thread(() -> {
                        System.out.println("Block received from: " +answer.getAddress() + ":" + answer.getPort());
                        DownloadTasksManager dtm = node.getTaskManager(answer.getHash());
                        dtm.putBlockAnswer(answer);
                    });
                    pool.submit(t);
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
