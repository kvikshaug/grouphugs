package no.kvikshaug.gh.exceptions;

public class PreferenceNotSetException extends Exception {
    public PreferenceNotSetException() {
    }

    public PreferenceNotSetException(String s) {
        super(s);
    }

    public PreferenceNotSetException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public PreferenceNotSetException(Throwable throwable) {
        super(throwable);
    }
}
