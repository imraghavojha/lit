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

            // This is the new case for the commit command.
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