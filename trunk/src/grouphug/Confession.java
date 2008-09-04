package grouphug;

/**
 * This object is a confession from grouphug.us
 * Contains a String of the confession, and an int of the no. of hugs
 */
public class Confession {

    private String confession;
    private int hugs;

    public String getConfession() {
        return confession;
    }

    public int getHugs() {
        return hugs;
    }

    public Confession(String confession, int hugs) {
        this.confession = confession;
        this.hugs = hugs;
    }

    public String toString() {
        if(hugs == -1)
            return confession;
            // TODO: test this: need to cut last char?
            //return confession.substring(0, confession.length()-1); // substring because last char is \n
        else
            return confession+hugs+" klemz";
    }
}
