import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Messages.FileSearchResult;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTasksManager extends Thread {
    //TODO Equivalente ao DealWithClient
    public static final int BLOCK_SIZE = 10240; //Block size in bytes
    public static final int THREADPOOL_SIMULTANEOUS_THREADS = 5;
    private Node node;
    private final String fileName;
    private byte[] file;
    private final byte[] hash;
    private final List<FileSearchResult> resultList;
    private List<FileBlockRequestMessage> blockRequests;
    private List<FileBlockAnswerMessage> blockAnswers;
    private final long startTime;
    private List<DownloadTask> tasks;
    private Lock lock = new ReentrantLock();
    private CountdownLatch countdownLatch;
    //TODO arranjar condiçoes para a Lock

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
        tasks = new ArrayList<>();
    }


    public FileBlockRequestMessage getBlockRequest() {
        if (blockRequests.isEmpty()) {
            return null;
        }
        lock.lock();
        FileBlockRequestMessage request = blockRequests.removeFirst();
        lock.unlock();
        return request;
    }

    public synchronized void putBlockAnswer(FileBlockAnswerMessage blockAnswer) { //TODO ver syncronized
        blockAnswers.add(blockAnswer);
        countdownLatch.countDown();
    }

     public byte[] getHash(){
        return hash;
     }

    //// Generates all blockRequest from info provided by FileSearchResult
    public List<FileBlockRequestMessage> generateFileBlockRequestMessages(FileSearchResult request) {
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
    ////

    private void finishDownload() throws NoSuchAlgorithmException, IOException {
        joinFileBlocks();
        byte[] newHash = MessageDigest.getInstance("SHA-256").digest(file);
        if(Arrays.equals(hash, newHash)){
            node.writeFile(file, fileName);
            node.updateFileList();
            System.out.println("Download finished: " + fileName);
        }else{
            //TODO Download Failed
            System.out.println("Download failed: Hash doesn't match: " + fileName + "\n" + Arrays.toString(hash)
            + "\n" + Arrays.toString(newHash));
        }
    }

    private void joinFileBlocks(){
        for(FileBlockAnswerMessage blockAnswer : blockAnswers) {
            byte[] data = blockAnswer.getData();
            int offset = (int)blockAnswer.getOffset();
            for(int i= 0; i<data.length; i++){
                file[offset+i] = data[i];
            }
        }
    }

    @Override
    public void run() {
        System.out.println("Download started: " + fileName + " Expected blocks: " + blockRequests.size());
        countdownLatch = new CountdownLatch(blockRequests.size());
        for(FileSearchResult result : resultList){
            DownloadTask task = new DownloadTask(/*result,*/this, node.getDealWithClient(result.getAddress(), result.getPort()));
            tasks.add(task);
            task.start();
        }
        try {
            countdownLatch.await();
            finishDownload();
        } catch (InterruptedException | NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }


}
