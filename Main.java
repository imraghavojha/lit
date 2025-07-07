import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import utils.WorkingDirManager;

public class Main {
    public static void main(String[] args) throws IOException {

        // --- TEMPORARY TEST CODE FOR WorkingDirManager ---
        // This block will run when you execute the file directly.
        // Once you're done testing, you can delete this entire block.
        System.out.println("--- Testing WorkingDirManager ---");
        try {
            Path testDir = Path.of("test_workspace");
            System.out.println("\n[STEP 1] Setting up test directory: " + testDir);
            WorkingDirManager.recreateDirectory(testDir);

            System.out.println("\n[STEP 2] Testing writeBlobToWorkingDir...");
            Path nestedFilePath = testDir.resolve("src/com/app/TestFile.java");
            WorkingDirManager.writeBlobToWorkingDir("fakeBlobSha1", nestedFilePath);
            System.out.println("  > Wrote blob to: " + nestedFilePath);
            if (Files.exists(nestedFilePath)) {
                System.out.println("  > VERIFIED: File was created successfully.");
                String content = Files.readString(nestedFilePath);
                System.out.println("  > File content: \"" + content + "\"");
            } else {
                System.err.println("  > FAILED: File was not created.");
            }

            System.out.println("\n[STEP 3] Testing deleteFile...");
            WorkingDirManager.deleteFile(nestedFilePath);
            System.out.println("  > Deleted file: " + nestedFilePath);
            if (!Files.exists(nestedFilePath)) {
                System.out.println("  > VERIFIED: File was deleted successfully.");
            } else {
                System.err.println("  > FAILED: File still exists.");
            }

            System.out.println("\n[STEP 4] Testing recreateDirectory...");
            Path dummyFile = testDir.resolve("src/com/app/dummy.txt");
            Files.writeString(dummyFile, "dummy");
            System.out.println("  > Created a dummy file to be deleted: " + dummyFile);
            WorkingDirManager.recreateDirectory(testDir.resolve("src"));
            System.out.println("  > Recreated directory: " + testDir.resolve("src"));
            if (Files.exists(testDir.resolve("src")) && !Files.exists(dummyFile)) {
                System.out.println("  > VERIFIED: Directory was cleared and recreated.");
            } else {
                System.err.println("  > FAILED: Directory recreation failed.");
            }

            System.out.println("\n[STEP 5] Cleaning up...");
            WorkingDirManager.deleteDirectory(testDir);
            System.out.println("  > Deleted test directory: " + testDir);
            if (!Files.exists(testDir)) {
                System.out.println("  > VERIFIED: Cleanup successful.");
            } else {
                System.err.println("  > FAILED: Cleanup failed.");
            }
        } catch (IOException e) {
            System.err.println("An error occurred during testing:");
            e.printStackTrace();
        }
        System.out.println("\n--- Testing Complete ---");
        // We add a return statement to stop the program after the tests.
        return; 
        // --- END OF TEST CODE ---


        /* // NOTE: The code below will not run because of the 'return' statement above.
        // To use the normal command-line features, comment out or delete the test block.

        if (args.length == 0) {
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
                String filePath = args[1];
                try {
                    CommandHandler.handleAdd(filePath);
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
                String branchName = args[1];
                try {
                    CommandHandler.handleBranch(branchName);
                } catch (Exception e) {
                    System.err.println("Error creating branch '" + branchName + "': " + e.getMessage());
                }
                break;

            case "commit":
                if (args.length < 3 || !args[1].equals("-m")) {
                    System.err.println("Error: Use `commit -m \"<your message>\"` to commit changes.");
                } else {
                    String message = Arrays.stream(args, 2, args.length)
                            .collect(Collectors.joining(" "));
                    CommandHandler.handleCommit(message);
                }
                break;

            default:
                System.err.println("Non-existing command!");
        }
        */
    }

    private static void printUsage() {
        System.out.println("Usage: lit <command> [options]");
        System.out.println();
        System.out.println("The available lit commands are:");
        System.out.println("init        Create an empty Lit repository or initialize one.");
    }
}
