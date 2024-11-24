import Messages.FileSearchResult;
import Messages.NewConnectionRequest;
import Messages.WordSearchMessage;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Node {
    private final String path;  //folder path
    private Map<File, byte[]> files; //TODO Trocar a key e o value, faz mais sentido a key ser a hash
    private ServerSocket serverSocket;
    private List<DealWithClient> dealWithClients;
    private DefaultListModel<FileSearch> currentSearch;

    public Node(int port, String path) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.path = path;
            this.files = new HashMap<>();
            currentSearch = new DefaultListModel<>();
            dealWithClients = new ArrayList<>();
            File dir = new File(path);
            if (!dir.exists()) dir.mkdirs();
            updateFileList();
            ConnectionThread connectionThread = new ConnectionThread(this);
            connectionThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return "Port NodeAddress [address=" + InetAddress.getLocalHost().getHostAddress()+", port=" + serverSocket.getLocalPort()+"]";
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    //// Getters
    public DefaultListModel<FileSearch> getCurrentSearchResults() {
        return currentSearch;
    }
    public int getPort(){
        return serverSocket.getLocalPort();
    }
    public InetAddress getAddress() {
        return serverSocket.getInetAddress();
    }
    public ServerSocket getSocket(){
        return serverSocket;
    }
    public Map<File, byte[]> getFiles() {
        return files;
    }
    ////

    //// Updates File Hashmap with the files on the folder
    public void updateFileList(){
        File[] fs = new File(path).listFiles(/*f -> f.getName().endsWith(".mp3")*/ _ -> true); //TODO Alterar, só para testes
        if (fs != null) {
            for(File f : fs){
                files.put(f, generateFileHash(f));
            }
        }
    }
    ////

    //// Generates Hash from the File data
    public static byte[] generateFileHash(File f) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    ////

    //// Obtains a file via its unique hash
    public File getFile(byte[] hash) {
        for(Map.Entry<File, byte[]> entry : files.entrySet()){
            File f = entry.getKey();
            byte[] entryHash = entry.getValue();
            if(Arrays.equals(entryHash, hash)){
                return f;
            }
        }
        return null; //TODO Mandar erro qq dps
    }

    //// Download ////
    public void startDownload(List<FileSearchResult> results){
        DownloadTasksManager downloadTasksManager = new DownloadTasksManager(results, this);
        downloadTasksManager.start();
        try {
            downloadTasksManager.join();
            updateFileList();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
    //////////

    //// Search ////
    /// Sets up a clean currentSearch DefaultListModel
    /// and sends search request to all connected nodes via each DealWithClient
    public void search(String s){
        //TODO Não deixar fazer nova procura sem receber as respostas todas da anterior, usar CountdownLatch?
        Thread t = new Thread(() -> {
            WordSearchMessage message = new WordSearchMessage(s);
            currentSearch.clear();
            for(DealWithClient dealWithClient : dealWithClients){
                dealWithClient.send(message);
            }
        });
        t.start();
    }

    //// Lock to ensure DefaultListModel coherence
    private Lock updateSearchLock = new ReentrantLock();

    //// Invoked when DealWithClient receives search result
    /// updates DefaultListModel with the results
    public void updateSearchList(List<FileSearchResult> list) { //TODO Perguntar pq demora, ou trocar por outra coisa
        for(FileSearchResult r: list){
            updateSearchLock.lock();
            boolean found = false;
            Object[] arr = currentSearch.toArray();
            for(Object s: arr){
                FileSearch search = (FileSearch) s;
                if(Arrays.equals(search.getHash(), r.getHash())){
                    int i = currentSearch.indexOf(search);
                    currentSearch.get(i).add(r);
                    found = true;
                    break;
                }
            }
            if(!found){
                FileSearch se = new FileSearch(r);
                currentSearch.addElement(se);
            }
            updateSearchLock.unlock();
        }
    }
    ///////////////


    private void addDealWithClient(DealWithClient dealWithClient){
        dealWithClients.add(dealWithClient);
    }

    public void removeDealWithClient(DealWithClient dealWithClient){
        dealWithClients.remove(dealWithClient);
    }

    //// Thread responsible for receiving connection Requests
    private class ConnectionThread extends Thread {
        Node node;

        public ConnectionThread(Node node) {
            this.node = node;
        }

        @Override
        public void run() {
            System.out.println("Awaiting connection...");
            while(!interrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    NewConnectionRequest request = (NewConnectionRequest) in.readObject();
                    DealWithClient thread = new DealWithClient(request.getAddress(), request.getPort(), node, socket);
                    node.addDealWithClient(thread);
                    thread.start();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    ////

    //// Establishes new connection to another Node, return true if connection sussecful, false otherwise
    public boolean newConnection(String ip, int destinationPort){
        NewConnectionRequest request;
        try {
            InetAddress address = InetAddress.getByName(ip);
            //Verifies if it isn't connecting with himself
            if((address.isLoopbackAddress() || ip.equals(InetAddress.getLocalHost().getHostAddress()) )
                    && destinationPort == serverSocket.getLocalPort()){
                throw new ConnectException();
            }
            //Verifies if it is already connected to that Address and Port
            for(DealWithClient d : dealWithClients){
                if(d.getInetAddress() == address && d.getPort() == destinationPort){
                    throw new ConnectException();
                }
            }
            Socket socket = new Socket(address, destinationPort);
            //// Send NewConnectionRequest to other Node
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            request = new NewConnectionRequest(InetAddress.getByName(null), serverSocket.getLocalPort());
            out.writeObject(request);
            //// Creates new DealWithClient
            DealWithClient thread = new DealWithClient(address, destinationPort, this, socket);
            thread.start();
            addDealWithClient(thread);

        } catch (ConnectException e) {
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
