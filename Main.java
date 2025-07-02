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
            case "test-blob":
                // Test saving a blob for testfile.txt (echo "Hello, Lit!" > testfile.txt)
                BlobObject blob = new BlobObject("testfile.txt");
                blob.save();
                break;
            
            case "test-tree":
                // Test saving a tree for the current directory (excluding .lit)
                try {
                    TreeObject tree = TreeBuilder.buildTree(Paths.get("."));
                    tree.save();
                } catch (IOException e) {
                    System.err.println("Failed to build or save tree: " + e.getMessage());
                }
                break;
            case "test-commit":
                // Replace this with a real tree SHA-1 from a previous test-tree run
                String treeSha1 = "55a1be6c4200992778133fe430e96b7eb858cdd7"; // <-- put your actual tree SHA-1 here
                String parentSha1 = null; // No parent for initial commit
                String authorName = "Test User";
                String authorEmail = "test@example.com";
                String commitMessage = "Initial commit for testing";

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