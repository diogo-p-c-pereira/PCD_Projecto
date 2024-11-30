import Messages.FileBlockRequestMessage;

public class DownloadTask extends Thread {
    private DownloadTasksManager downloadTasksManager;
    //private FileSearchResult fileSearchResult;
    private DealWithClient dealWithClient;

    public DownloadTask(/*FileSearchResult fileSearchResult,*/ DownloadTasksManager downloadTasksManager, DealWithClient dealWithClient) {
        //this.fileSearchResult = fileSearchResult;
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
            dealWithClient.send(request);
        }
    }
}
