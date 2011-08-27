package no.kvikshaug.gh.github;

import com.sun.net.httpserver.BasicAuthenticator;
import no.kvikshaug.gh.Config;
import no.kvikshaug.gh.exceptions.PreferenceNotSetException;

public class GithubBasicAuthenticator extends BasicAuthenticator{
    private String validUsername;
    private String validPassword;
    public GithubBasicAuthenticator(String s, String validUsername, String validPassword) {
        super(s);
        this.validUsername = validUsername;
        this.validPassword = validPassword;
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        return username.matches(validUsername) && password.matches(validPassword);
    }
}
