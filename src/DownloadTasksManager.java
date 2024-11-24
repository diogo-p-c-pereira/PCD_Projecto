import Messages.FileBlockAnswerMessage;
import Messages.FileBlockRequestMessage;
import Messages.FileSearchResult;
import java.util.ArrayList;
import java.util.List;

public class DownloadTasksManager extends Thread {
    //TODO Equivalente ao DealWithClient
    public static final int BLOCK_SIZE = 10240;//Block size in bytes
    public static final int THREADPOOL_SIMULTANEOUS_THREADS = 5;
    private Node node;
    private byte[] file;
    private final List<FileSearchResult> resultList;
    private List<FileBlockRequestMessage> blockRequests;
    private List<FileBlockAnswerMessage> blockAnswers;
    private final long startTime;

    private List<DownloadTask> tasks;
    //TODO metodos sincronos

    public DownloadTasksManager(List<FileSearchResult> resultList, Node node) {
        this.node = node;
        this.resultList = resultList;
        blockRequests = generateFileBlockRequestMessages(resultList.getFirst());
        startTime = System.currentTimeMillis();
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

    @Override
    public void run() {
        for(FileSearchResult result : resultList){
            DownloadTask task = new DownloadTask(result,this);
            tasks.add(task);
            task.start();
        }
        //TODO Barreira que espera que as threads acabem e junto o ficheiro/grava
    }


}
