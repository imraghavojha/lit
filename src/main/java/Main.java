import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import utils.CommandHandler;

public class Main{
    public static void main(String[] args) throws IOException {
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


            case "branch": 
                if (args.length < 2) {
                    System.err.println("Error: No branch name specified for 'branch' command.");
                    System.out.println("Usage: lit branch <branch_name>");
                    return;
                }
                String branchName = args[1]; // Get the branch name from the arguments
                try {
                    CommandHandler.handleBranch(branchName); // Call the new handleBranch method
                } catch (Exception e) {
                    System.err.println("Error creating branch '" + branchName + "': " + e.getMessage());
                }
                break;

            case "commit":
                if (args.length < 3 || !args[1].equals("-m")) {
                    System.err.println("Error: Use `commit -m \"<your message>\"` to commit changes.");
                } else {
                    // This combines all arguments after "-m" into a single string.
                    String message = Arrays.stream(args, 2, args.length)
                                           .collect(Collectors.joining(" "));
                    CommandHandler.handleCommit(message);
                }
                break;

            case "switch": 
                if (args.length < 2) {
                    System.err.println("Error: No branch or commit specified for 'switch' command.");
                    System.out.println("Usage: lit switch <branch_name_or_commit_sha>");
                    return;
                }
                String targetRef = args[1]; // Get the target reference from the arguments
                try {
                    CommandHandler.handleSwitch(targetRef); // Call the new handleSwitch method
                } catch (Exception e) {
                    System.err.println("Error switching to '" + targetRef + "': " + e.getMessage());
                }
                break;

            case "merge":
            if (args.length == 4 && args[2].equals("-m")) {
                String otherBranchName = args[1]; // Use a different variable name
                String message = args[3];
                CommandHandler.handleMergeCommit(message, otherBranchName);
            } else {
                System.out.println("Usage: lit merge <branch-name> -m \"<merge-message>\"");
            }
            break;
            case "rm":
                if (args.length < 2) {
                    System.err.println("Error: No file specified for 'rm' command.");
                    System.out.println("Usage: lit rm <file>");
                    return;
                }
                String rmFilePath = args[1];
                try {
                    CommandHandler.handleRm(rmFilePath);
                } catch (Exception e) {
                    System.err.println("Error removing file '" + rmFilePath + "': " + e.getMessage());
                }
                break;
                
            // Future commands to be added here
            default:
                System.err.println("Non-existing command!");
                printUsage();
        }

    }

    private static void printUsage(){
        System.out.println("Usage: lit <command> [options]");
        System.out.println();
        System.out.println("The available lit commands are:");
        System.out.println("init        Create an empty Lit repository or initialize one.");
        System.out.println("add         Add file contents to the index.");
        System.out.println("rm          Remove files from the working tree and from the index.");
        System.out.println("branch      Create a new branch.");
        System.out.println("commit      Record changes to the repository."); 
        System.out.println("switch      Switch branches or restore working tree files."); 
    }
}