package no.kvikshaug.gh.exceptions;

public class GithubHookDisabledException extends Exception {
    public GithubHookDisabledException() {
    }

    public GithubHookDisabledException(String s) {
        super(s);
    }

    public GithubHookDisabledException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public GithubHookDisabledException(Throwable throwable) {
        super(throwable);
    }
}
