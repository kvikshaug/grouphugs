package grouphug;

public class SlangItem {

    private int number;
    private String word;
    private String definition;
    private String example;

    public int getNumber() {
        return number;
    }

    public String getWord() {
        return word;
    }

    public String getDefinition() {
        return definition;
    }

    public String getExample() {
        return example;
    }

    public SlangItem(int number, String word, String definition, String example) {
        this.number = number;
        this.word = word;
        this.definition = definition;
        this.example = example;
    }
}
