package Messages;

import java.io.Serializable;

public final class WordSearchMessage implements Serializable {
    private final String keyword;

    public WordSearchMessage(String search) {
        this.keyword = search;
    }

    public String getKeyword() {
        return keyword;
    }
}
