package grouphug;

/**
 * Created by IntelliJ IDEA.
 * User: Alex
 * Date: Jun 17, 2008
 * Time: 12:08:52 AM
 * To change this template use File | Settings | File Templates.
 */
public interface GrouphugModule {
    // TODO this should be implemented when i get around to not letting the module classes be static :p
    public abstract void trigger(Grouphug bot, String sender, String message);
}
