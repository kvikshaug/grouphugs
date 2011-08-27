package no.kvikshaug.gh.github.json;

import org.joda.time.DateTime;

public class Repository {
    private DateTime created_at;
    private String description;
    private boolean fork;
    private int forks;
    private boolean has_downloads;
    private boolean has_issues;
    private boolean has_wiki;
    private String homepage;
    private String language;
    private String name;
    private int open_issues;
    private User owner;
    private int watchers;
    private boolean _private;
    private DateTime pushed_at;
    private int size;
    private String url;

    public String getRepoName() {
        return owner.getName() + '/' + name;
    }
}
