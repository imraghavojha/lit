package objects;

public class TreeEntry {

    private String mode;
    private String type;
    private String objectSha1Id;
    private String name;

    public TreeEntry(String mode, String type, String objectSha1Id, String name){

        this.mode = mode;
        this.type = type;
        this.objectSha1Id = objectSha1Id;
        this.name = name;

    }

    public String getMode(){
        return mode;
    }
    public String getType(){
        return type;
    }
    public String getName(){
        return name;
    }
    public String getObjectSha1Id(){
        return objectSha1Id;
    }


} 


