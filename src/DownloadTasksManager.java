import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Messages.FileSearchResult;

import java.io.IOException;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTasksManager extends Thread {
    private static final int BLOCK_SIZE = 10240; //Block size in bytes
    private final Node node;
    private final String fileName;
    private final byte[] file;
    private final byte[] hash;
    private final List<FileSearchResult> resultList;
    private final List<FileBlockRequestMessage> blockRequests;
    private final List<FileBlockAnswerMessage> blockAnswers;
    private final long startTime;
    //private List<DownloadTask> tasks;
    private CountdownLatch countdownLatch; //Used to wait until all Blocks are received

    public DownloadTasksManager(List<FileSearchResult> resultList, Node node) {
        this.node = node;
        this.hash = resultList.getFirst().getHash();
        this.resultList = resultList;
        FileSearchResult result = resultList.getFirst();
        this.file = new byte[(int)result.getFile_size()];
        fileName = result.getFile_name();
        blockRequests = generateFileBlockRequestMessages(result);
        blockAnswers = new ArrayList<>();
        startTime = System.currentTimeMillis();
        //tasks = new ArrayList<>();
    }

    //// BlockRequest Getter used by DownloadTask Thread
    private final Lock requestsLock = new ReentrantLock();
    public FileBlockRequestMessage getBlockRequest() {
        requestsLock.lock();
        if (blockRequests.isEmpty()) {
            return null;
        }
        FileBlockRequestMessage request = blockRequests.removeFirst();
        requestsLock.unlock();
        return request;
    }

    //// Used by DealWithClient when it receives a download answer
    private final Lock answerLock = new ReentrantLock();
    public void putBlockAnswer(FileBlockAnswerMessage blockAnswer) {
        answerLock.lock();
        blockAnswers.add(blockAnswer);
        countdownLatch.countDown();
        answerLock.unlock();
    }

     public byte[] getHash(){
        return hash;
     }

    //// Generates all blockRequest from info provided by FileSearchResult
    private List<FileBlockRequestMessage> generateFileBlockRequestMessages(FileSearchResult request) {
        //f.length -> file size in bytes
        int nBlocks;
        int fileSize = (int)request.getFile_size();
        if(fileSize%BLOCK_SIZE != 0){
            nBlocks = (fileSize/BLOCK_SIZE)+1;
        }else{
            nBlocks = (fileSize/BLOCK_SIZE);
        }
        List<FileBlockRequestMessage> blockList = new ArrayList<>();
        for(int i = 0, offset = 0; i < nBlocks; i++){
            if(i==nBlocks-1){
                blockList.add(new FileBlockRequestMessage(request.getHash(), offset, fileSize-offset));
                break;
            }
            blockList.add(new FileBlockRequestMessage(request.getHash(), offset, BLOCK_SIZE));
            offset += BLOCK_SIZE;
        }
        return blockList;
    }

    /// Finish Download and invokes methods to writeFile to disk and update File list
    /// Also calls GUI to display download result
    private void finishDownload() throws NoSuchAlgorithmException, IOException {
        List<SupplierResult> list = joinFileBlocks();
        byte[] newHash = MessageDigest.getInstance("SHA-256").digest(file);
        if(Arrays.equals(hash, newHash)){
            node.writeFile(file, fileName);
            node.updateFileList();
            int elapsedTime = (int) (System.currentTimeMillis() - startTime)/1000;
            System.out.println("Download finished: " + fileName + " Elapsed time: " + elapsedTime + "s");
            GUI.getInstance().showMessage("Descarga Completa: " + fileName + "\n" +
                    SupplierResult.listToString(list) + "Tempo Decorrido:" + elapsedTime + "s");
        }else{
            System.out.println("Download failed: Hash doesn't match. File:" + fileName);
            GUI.getInstance().showMessage("Download failed: Hash doesn't match. \n File:" + fileName);
        }
    }


    //// Joins all data arrays from FileBlockAnswerMessage to the byte[] file
    private List<SupplierResult> joinFileBlocks(){
        List<SupplierResult> list = new ArrayList<>();
        for(FileBlockAnswerMessage blockAnswer : blockAnswers) {
            byte[] data = blockAnswer.getData();
            int offset = (int) blockAnswer.getOffset();
            for (int i = 0; i < data.length; i++) {
                file[offset + i] = data[i];
            }
           SupplierResult.addSupplierResult(list, blockAnswer.getAddress(), blockAnswer.getPort());
        }
        return list;
    }

    //// SupplierResult class to assist in download result display
    //// and its list auxiliary methods
    private static class SupplierResult{
        private final InetAddress address;
        private final int port;
        private int nBlocks;

        public SupplierResult(InetAddress address, int port) {
            this.address = address;
            this.port = port;
            nBlocks = 1;
        }
        public void incrementBlock(){
            nBlocks++;
        }
        @Override
        public String toString() {
            return "Fornecedor [endereco=" + address + ", porto=" + port + "]:" + nBlocks;
        }

        //// Used to increment or add SupplierResult to List
        public static void addSupplierResult(List<SupplierResult> list, InetAddress address, int port) {
            boolean added = false;
            for (SupplierResult temp : list) {
                if (temp.address.equals(address) && temp.port == port) {
                    temp.incrementBlock();
                    added = true;
                    break;
                }
            }
            if (!added) {
                list.add(new SupplierResult(address, port));
            }
        }

        //// Used to create a String with all SupplierResults to send to GUI
        public static String listToString(List<SupplierResult> list) {
            String s = "";
            for (SupplierResult temp : list) {
                s += temp.toString()+"\n";
            }
            return s;
        }
    }


    @Override
    public void run() {
        System.out.println("Download started: " + fileName + " Expected blocks: " + blockRequests.size());
        countdownLatch = new CountdownLatch(blockRequests.size());
        for(FileSearchResult result : resultList){
            DownloadTask task = new DownloadTask(/*result,*/this, node.getDealWithClient(result.getAddress(), result.getPort()));
            //tasks.add(task);
            task.start();
        }
        try {
            countdownLatch.await();
            finishDownload();
            node.removeDownloadTaskManager(this);
        } catch (InterruptedException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }


}
