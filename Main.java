import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import objects.BlobObject;
import objects.TreeObject;
import utils.TreeBuilder;
import objects.CommitObject;

public class Main{
    public static void main(String[] args) {

        // Blob testing
        BlobObject blob1 = new BlobObject("sample/sampledir1/blobSample1.txt");

        String sample_sha1 = blob1.getSha1();

        System.out.println("---Blob SHA-1---");
        System.out.println(sample_sha1);
        System.out.println();

        // Tree testing
        Path rootDirectoryPath = Paths.get("sample"); // Changed to 'lit/sample' for testing the sample directory

        if (!Files.isDirectory(rootDirectoryPath)) {
            System.err.println("Error: Root directory for tree building not found at " + rootDirectoryPath.toAbsolutePath());
            System.err.println("Please ensure the directory exists and is accessible.");
            return; // Exit if the root directory isn't found
        }


        try {
            System.out.println("Building tree for directory: " + rootDirectoryPath.toAbsolutePath());
            TreeObject rootTree = TreeBuilder.buildTree(rootDirectoryPath);

            System.out.println("\nRoot Tree Object SHA-1: " + rootTree.getSha1Id());

            System.out.println("\n--- Root Tree Entries (Sorted) ---");
            for (objects.TreeEntry entry : rootTree.getEntries()) {
                System.out.println("  Mode: " + entry.getMode() +
                                   ", Type: " + entry.getType() +
                                   ", SHA-1: " + entry.getObjectSha1Id() +
                                   ", Name: " + entry.getName());
            }

            System.out.println("\n----------------------------------------\n");

            // --- Commit Testing ---
            System.out.println("--- Creating First Commit ---");
            
            // 1. Get the root tree SHA-1 from the previous step
            String rootTreeSha1 = rootTree.getSha1Id();
            
            // 2. For the first commit, there is no parent
            String parentSha1 = null; 
            
            // 3. Define author details and commit message
            String authorName = "Raghav Ojha";
            String authorEmail = "imraghavojha@gmail.com";
            String commitMessage = "Initial commit: Add sample files";
            
            // 4. Create the commit object
            CommitObject initialCommit = new CommitObject(rootTreeSha1, parentSha1, authorName, authorEmail, commitMessage);
            
            System.out.println("New Commit Object Created!");
            System.out.println("Commit SHA-1: " + initialCommit.getSha1());
            System.out.println("Tree SHA-1:   " + initialCommit.getTreeSha1());
            System.out.println("Parent SHA-1: " + initialCommit.getParentSha1());
            System.out.println("Message:      " + initialCommit.getCommitMessage());


        } catch (IOException e) {
            System.err.println("An I/O error occurred during tree building: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.err.println("An unexpected runtime error occurred: " + e.getMessage());
            e.printStackTrace();
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
        }
    }
}
