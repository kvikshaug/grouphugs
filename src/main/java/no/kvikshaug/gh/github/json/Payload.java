package no.kvikshaug.gh.github.json;
import java.util.List;

public class Payload {
    private String after;
    private String before;
    private String base;
    private List<Commit> commits;
    private String compare;
    private boolean created;
    private boolean deleted;
    private boolean forced;
    private User pusher;
    private String ref;
    private Repository repository;

    public Commit getHead() {
        for (Commit commit : commits) {
            if (commit.getId().equals(after)) {
                return commit;
            }
        }
        return null;
    }

    public String getAffectedRefName() {
        return ref.substring(ref.lastIndexOf('/') + 1);
    }

    public String prefix() {
        return repository.getRepoName() + ' ' + getAffectedRefName();
    }

    public Repository getRepository() {
        return repository;
    }

    public String getRef() {
        return ref;
    }

    public User getPusher() {
        return pusher;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isCreated() {
        return created;
    }

    public String getCompare() {
        return compare;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public String getBase() {
        return base;
    }

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }
}
