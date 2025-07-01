import objects.BlobObject;
import utils.CommandHandler;

public class Main{
    public static void main(String[] args) {
        if (args.length == 0){
            printUsage();
            return;
        }

        String command = args[0];

        switch (command) {
            case "init":
                CommandHandler.handleInit();
                break;
             case "test-blob":
                // Test saving a blob for testfile.txt (echo "Hello, Lit!" > testfile.txt)
                BlobObject blob = new BlobObject("testfile.txt");
                blob.save();
                break;

        // Future commands to be added here
            default:
                System.err.println("Non-existing command!");
        }

    }

    private static void printUsage(){
        System.out.println("Usage: lit <command> [options]");
        System.out.println();
        System.out.println("The available lit commands are:");
        System.out.println("init        Create an empty Lit repository or initialize one.");
    }
}