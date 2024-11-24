package Messages;

import java.io.Serializable;
import java.util.List;

public final class FileSearchResultList implements Serializable {
    //// Wrapper class created to enable to output via ObjectStream a list of FileSearchResult objects
    private final List<FileSearchResult> fileSearchResultList;
    public FileSearchResultList(List<FileSearchResult> fileSearchResultList) {
        this.fileSearchResultList = fileSearchResultList;
    }
    public List<FileSearchResult> getFileSearchResultList() {
        return fileSearchResultList;
    }
}
