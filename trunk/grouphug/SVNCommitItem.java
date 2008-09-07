package grouphug;

import java.util.ArrayList;

public class SVNCommitItem {

    private int revision;
    private String user;
    private ArrayList<SVNCommitItemFile> files;
    private String comment;

    public SVNCommitItem(int revision, String user, ArrayList<SVNCommitItemFile> files, String comment) {
        this.revision = revision;
        this.user = user;
        this.files = files;
        this.comment = comment;
    }

    public String toString() {
        String returnStr = "gh svn @ r"+revision+" : "+user;
        for(SVNCommitItemFile file : files) {
            returnStr += "\n"+file;
        }
        returnStr += "\n"+comment;
        return returnStr;
    }
}
