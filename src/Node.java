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
    private final Map<File, byte[]> files;
    private final ServerSocket serverSocket;
    private final List<DealWithClient> dealWithClients;
    private final DefaultListModel<FileSearch> currentSearch;
    private final List<DownloadTasksManager> tasksManagers;

    public Node(int port, String path) {
        try {
            this.serverSocket = new ServerSocket(port);
            this.path = path;
            this.files = new HashMap<>();
            currentSearch = new DefaultListModel<>();
            dealWithClients = new ArrayList<>();
            tasksManagers = new ArrayList<>();
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
        try {
            return InetAddress.getByName(null);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<File, byte[]> getFiles() {
        synchronized (files) {
            return files;
        }
    }
    ////

    //// Updates File Hashmap with the files on the folder
    public void updateFileList(){
        synchronized (files) {
            File[] fs = new File(path).listFiles(/*f -> f.getName().endsWith(".mp3")*/ _ -> true); //TODO Alterar, só para testes
            if (fs != null) {
                for(File f : fs){
                    files.put(f, generateFileHash(f));
                }
            }
        }
    }


    //// Generates Hash from the File data
    public static byte[] generateFileHash(File f) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    //// Obtains a file via its unique hash
    public File getFile(byte[] hash) {
        synchronized (files) {
            for(Map.Entry<File, byte[]> entry : files.entrySet()){
                File f = entry.getKey();
                byte[] entryHash = entry.getValue();
                if(Arrays.equals(entryHash, hash)){
                    return f;
                }
            }
            return null;
        }
    }

    //// Writes file to disk
    public synchronized void writeFile(byte[] file, String filename) throws IOException {
        Files.write(new File(path + File.separator + filename).toPath() ,file);
    }

    //// Download ////
    public void startDownload(List<FileSearch> results){
        for(FileSearch result : results){
            DownloadTasksManager downloadTasksManager = new DownloadTasksManager(result.getList(), this);
            synchronized (tasksManagers) {
                tasksManagers.add(downloadTasksManager);
            }
            downloadTasksManager.start();
        }
    }

    /// DownloadTaskManager getter, used by DealWithClient to know to which DTM to send received block
    public DownloadTasksManager getTaskManager(byte[] hash){
        synchronized (tasksManagers) {
            for(DownloadTasksManager downloadTasksManager : tasksManagers){
                if(Arrays.equals(downloadTasksManager.getHash(), hash)){
                    return downloadTasksManager;
                }
            }
            throw new RuntimeException("No DownloadTasksManager found for hash: " + Arrays.toString(hash));
        }
    }

    /// Removes DownloadTaskManager, used when download finishes
    public void removeDownloadTaskManager(DownloadTasksManager dtm){
        synchronized (tasksManagers) {
            tasksManagers.remove(dtm);
        }
    }

    /// DealWithClient getter, used to be give argument to DownloadTask
    public DealWithClient getDealWithClient(InetAddress address, int port) {
        synchronized (dealWithClients) {
            for(DealWithClient dealWithClient : dealWithClients){
                if(dealWithClient.getInetAddress().equals(address) && dealWithClient.getPort() == port){
                    return dealWithClient;
                }
            }
            throw new IllegalArgumentException("No deal with client found");
        }
    }

    /// Adds new DealWithClient, used when connection is created
    private void addDealWithClient(DealWithClient dealWithClient){
        synchronized (dealWithClients) {
            dealWithClients.add(dealWithClient);
        }
    }

    /// Removes DealWithClient, used when connection is severed
    public void removeDealWithClient(DealWithClient dealWithClient){
        synchronized (dealWithClients) {
            dealWithClients.remove(dealWithClient);
        }
    }

    //// Search ////
    /// Sets up a clean currentSearch DefaultListModel
    /// and sends search request to all connected nodes via each DealWithClient
    public void search(String s){
        //TODO Não deixar fazer nova procura sem receber as respostas todas da anterior, usar CountdownLatch?, para não usar syncronized
        Thread t = new Thread(() -> {
            WordSearchMessage message = new WordSearchMessage(s);
            currentSearch.clear();
            synchronized (dealWithClients) {
                for(DealWithClient dealWithClient : dealWithClients){
                    dealWithClient.send(message);
                }
            }
        });
        t.start();
    }

    //// Lock to ensure DefaultListModel coherence
    private final Lock updateSearchLock = new ReentrantLock();

    //// Invoked when DealWithClient receives search result
    /// updates DefaultListModel with the results
    public void updateSearchList(List<FileSearchResult> list) {
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
    ////////////

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
            synchronized (dealWithClients) {
                for(DealWithClient d : dealWithClients){
                    if(d.getInetAddress().equals(address) && d.getPort() == destinationPort){
                        throw new ConnectException();
                    }
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
