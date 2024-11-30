import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Messages.FileSearchResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadTasksManager extends Thread {
    //TODO Equivalente ao DealWithClient
    public static final int BLOCK_SIZE = 10240; //Block size in bytes
    public static final int THREADPOOL_SIMULTANEOUS_THREADS = 5;
    private Node node;
    private byte[] file;
    private byte[] hash;
    private final List<FileSearchResult> resultList;
    private List<FileBlockRequestMessage> blockRequests;
    private List<FileBlockAnswerMessage> blockAnswers;
    private final long startTime;
    private List<DownloadTask> tasks;
    private Lock lock = new ReentrantLock();
    //TODO arranjar condiçoes para a Lock

    public DownloadTasksManager(List<FileSearchResult> resultList, Node node) {
        this.node = node;
        this.hash = resultList.getFirst().getHash();
        this.resultList = resultList;
        blockRequests = generateFileBlockRequestMessages(resultList.getFirst());
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
    }

     public byte[] getHash(){
        return hash;
     }

    //// Generates all blockRequest from info provided by FileSearchResult
    public List<FileBlockRequestMessage> generateFileBlockRequestMessages(FileSearchResult request) {
        //f.length -> file size in bytes
        int nBlocks;
        long fileSize = request.getFile_size();
        if(fileSize%BLOCK_SIZE != 0){
            nBlocks = (int)(fileSize/BLOCK_SIZE)+1;
        }else{
            nBlocks = (int)(fileSize/BLOCK_SIZE);
        }
        List<FileBlockRequestMessage> blockList = new ArrayList<>();
        for(int i = 0, offset = 0; i < nBlocks; i++){
            if(i==nBlocks-1){
                blockList.add(new FileBlockRequestMessage(request.getHash(), offset, (int)(fileSize-offset)));
                break;
            }
            blockList.add(new FileBlockRequestMessage(request.getHash(), offset, BLOCK_SIZE));
            offset += BLOCK_SIZE;
        }
        return blockList;
    }
    ////

    public void finishDownload() {
        System.out.println("Download finished");
        System.out.println(blockRequests.size());
        System.out.println(blockAnswers.size());
        System.out.println(Arrays.toString(blockAnswers.getFirst().getHash()));
    }

    @Override
    public void run() {
        CyclicBarrier barrier = new CyclicBarrier(resultList.size(), new Runnable() {
            @Override
            public void run() {
                finishDownload();
            }
        });
        for(FileSearchResult result : resultList){
            DownloadTask task = new DownloadTask(result,this, node.getDealWithClient(result.getAddress(), result.getPort()), barrier);
            tasks.add(task);
            task.start();
        }
        //TODO Barreira que espera que as threads acabem e junto o ficheiro/grava
    }


}
