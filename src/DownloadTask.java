import Messages.FileSearchResult;

public class DownloadTask extends Thread {
    private DownloadTasksManager downloadTasksManager;
    private FileSearchResult fileSearchResult;
    private DealWithClient dealWithClient;

    public DownloadTask(FileSearchResult fileSearchResult, DownloadTasksManager downloadTasksManager) {
        this.fileSearchResult = fileSearchResult;
        this.downloadTasksManager = downloadTasksManager;
    }

    @Override
    public void run() {

    }
}
