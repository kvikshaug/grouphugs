package grouphug;

class SVNCommitItemFile {

    private char modification;
    private String filename;

    public SVNCommitItemFile(char modification, String filename) {
        this.modification = modification;
        this.filename = filename;
    }

    public String toString() {
        return " - "+modification+" "+filename;
    }
}
