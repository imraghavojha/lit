import java.io.IOException;
import java.nio.file.Paths;

import objects.BlobObject;
import objects.TreeObject;
import utils.CommandHandler;
import utils.TreeBuilder;

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
        
            case "add": 
                if (args.length < 2) {
                    System.err.println("Error: No file specified for 'add' command.");
                    System.out.println("Usage: lit add <file>");
                    return;
                }
                String filePath = args[1]; // Get the file path from the arguments
                try {
                    CommandHandler.handleAdd(filePath); // Call the new handleAdd method
                } catch (Exception e) {
                    System.err.println("Error staging file '" + filePath + "': " + e.getMessage());
                }
                break;

                objects.CommitObject commit = new objects.CommitObject(
                    treeSha1, parentSha1, authorName, authorEmail, commitMessage
                );
                commit.save();
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