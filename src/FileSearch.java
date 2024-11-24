import Messages.FileSearchResult;

import java.util.ArrayList;
import java.util.List;

public class FileSearch {
    //TODO Name pending (find a more suitable name for this)
    //// Wrapper class used for collecting all SearchResults for the same file of different nodes
    /// its main purpose it's to be loaded to the DefaultListModel to be used on the GUI JList
    /// to be displayed and selected from the Graphic Interface
    /// its toString also enables the GUI to display the number of nodes a file is available
    private final String fileName;
    private final byte[] hash;
    private List<FileSearchResult> list;

    public FileSearch(FileSearchResult fileSearchResult) {
        this.list = new ArrayList<>();
        list.add(fileSearchResult);
        hash = fileSearchResult.getHash();
        fileName = fileSearchResult.getFile_name();
    }

    public byte[] getHash() {
        return hash;
    }

    public List<FileSearchResult> getList() {
        return list;
    }

    public void add(FileSearchResult fsr) {
        list.add(fsr);
    }

    @Override
    public String toString() {
        return fileName + "<" + list.size()+">";
    }

}
