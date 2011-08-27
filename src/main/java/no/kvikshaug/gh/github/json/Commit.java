package no.kvikshaug.gh.github.json;

import org.joda.time.DateTime;
import java.util.List;


public class Commit {
    private List<String> added;
    private User author;
    private boolean distinct;
    private String id;
    private String message;
    private List<String> modified;
    private List<String> removed;
    private DateTime timestamp;
    private String url;


    public String getShortMessage() {
        if (message.indexOf('\n') != -1) {
            return message.substring(0, message.indexOf('\n'));
        } else {
            return message;
        }
    }

    public List<String> getAdded() {
        return added;
    }

    public User getAuthor() {
        return author;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getModified() {
        return modified;
    }

    public List<String> getRemoved() {
        return removed;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getUrl() {
        return url;
    }
}
