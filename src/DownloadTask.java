import Messages.FileBlockRequestMessage;
import Messages.FileSearchResult;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class DownloadTask extends Thread {
    private DownloadTasksManager downloadTasksManager;
    private FileSearchResult fileSearchResult;
    private DealWithClient dealWithClient;
    private CyclicBarrier barrier;

    public DownloadTask(FileSearchResult fileSearchResult, DownloadTasksManager downloadTasksManager, DealWithClient dealWithClient, CyclicBarrier barrier) {
        this.fileSearchResult = fileSearchResult;
        this.downloadTasksManager = downloadTasksManager;
        this.dealWithClient = dealWithClient;
        this.barrier = barrier;
    }

    @Override
    public void run() {
        while(!interrupted()){
            FileBlockRequestMessage request = downloadTasksManager.getBlockRequest();
            if(request == null){
                break;
            }
            dealWithClient.send(request);
        }
        try {
            barrier.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }
}
