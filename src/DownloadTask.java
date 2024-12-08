import Messages.FileBlockRequestMessage;

public class DownloadTask extends Thread {
    private final DownloadTasksManager downloadTasksManager;
    private final DealWithClient dealWithClient;

    public DownloadTask(DownloadTasksManager downloadTasksManager, DealWithClient dealWithClient) {
        this.downloadTasksManager = downloadTasksManager;
        this.dealWithClient = dealWithClient;
    }

    @Override
    public void run() {
        while(!interrupted()){
            FileBlockRequestMessage request = downloadTasksManager.getBlockRequest();
            if(request == null){
                break;
            }
            dealWithClient.sendBlockRequest(request, downloadTasksManager);
        }
    }
}
