import Messages.FileBlockRequestMessage;

public class DownloadTask extends Thread {
    private final DownloadTasksManager downloadTasksManager;
    private final DealWithClient dealWithClient;
    //private FileSearchResult fileSearchResult;

    public DownloadTask(DownloadTasksManager downloadTasksManager, DealWithClient dealWithClient/*, FileSearchResult fileSearchResult*/) {
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
            //TODO Espera até receber bloco, cadeado ou semaforo ou algo assim
        }
    }
}
