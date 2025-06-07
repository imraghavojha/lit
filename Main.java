import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import objects.BlobObject;
import objects.TreeObject;
import utils.TreeBuilder;

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
