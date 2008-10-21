package grouphug;

class KarmaItem {

    private int ID;
    private String name;
    private int karma;

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public int getKarma() {
        return karma;
    }

    public KarmaItem(int ID, String name, int karma) {
        this.ID = ID;
        this.name = name;
        this.karma = karma;
    }

    public String toString() {
        if(karma == 0)
            return "neutral";
        else
            return ""+karma;
    }
}
