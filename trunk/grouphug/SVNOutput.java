package grouphug;

/**
 * SVNOutput: A module that parses and outputs information about an SVN commit.
 */
// TODO: maybe this shouldn't implement GHModule, since it's not triggered by trigger() ?
public class SVNOutput implements GrouphugModule {

    public SVNOutput() {

        
    }

    public void trigger(Grouphug bot, String channel, String sender, String login, String hostname, String message) {
        // do nothing
    }
}
