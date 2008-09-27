package grouphug;

class KarmaItem {

    private int ID;
    private String field;
    private int karma;

    public int getID() {
        return ID;
    }

    public String getField() {
        return field;
    }

    public int getKarma() {
        return karma;
    }

    public KarmaItem(int ID, String field, int karma) {
        this.ID = ID;
        this.field = field;
        this.karma = karma;
    }

    public String toString() {
        if(karma == 0)
            return "neutral";
        else
            return ""+karma;
    }
}
