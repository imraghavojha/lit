package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import objects.BlobObject;
import objects.CommitObject;
import objects.IndexEntry;
import objects.TreeObject;

public class CommandHandler {

    public static void handleInit(){
            Path currentDirectory = Paths.get("");
            Path litPath = currentDirectory.resolve(".lit");
            Path objectsPath = litPath.resolve("objects");
            Path refPath = litPath.resolve("refs/heads");
            Path HEADPath = litPath.resolve("HEAD");
            Path mainPath = refPath.resolve("main");

            if(Files.exists(litPath)){
                System.err.println("The repo has already been initialized!");
                return;
            }
            
            try {
                // creating .lit directory and subdirectories
                Files.createDirectory(litPath);
                
                Files.createDirectory(objectsPath);

                Files.createDirectories(refPath);

                Files.createFile(HEADPath);
                Files.write(HEADPath, "ref: refs/heads/main".getBytes());

                Files.createFile(mainPath);

            } catch (IOException e) {
                System.err.println("Error: Failed to initialize repository.");
                System.err.println("Reason: " + e.getMessage());
                // cleanup logic to be added later in case any one of the creation is success
               
            }
    }

    public static void handleAdd(String filePathString) throws IOException {
        Path litPath = Paths.get("").toAbsolutePath().resolve(".lit");
        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            System.err.println("Error: Not a Lit repository (or any of the parent directories): .lit");
            return;
        }

        Path absoluteFilePath = Paths.get(filePathString).toAbsolutePath();
        Path currentDirectory = Paths.get("").toAbsolutePath();

        // Ensure the file is within the repository's working directory
        if (!absoluteFilePath.startsWith(currentDirectory)) {
             System.err.println("Error: File '" + filePathString + "' is outside the current working directory.");
             return;
        }
        
        if (!Files.exists(absoluteFilePath) || !Files.isRegularFile(absoluteFilePath)) {
            System.err.println("Error: pathspec '" + filePathString + "' did not match any files.");
            return;
        }
        
        // Create BlobObject and get its SHA-1
        BlobObject blob = new BlobObject(filePathString);
        String blobSha1 = blob.getSha1();
        
        if (blobSha1 == null) {
            System.err.println("Error: Could not generate SHA-1 for file: " + filePathString);
            return;
        }

        blob.save();
        // Determine the file mode (permissions)
        // Currently only works for 100644 (regular file)
        String fileMode = "100644";

        // Get the file path relative to the repository root for the IndexEntry
        // This is important because Git stores paths relative to the .git (or .lit) directory.
        Path relativeFilePath = currentDirectory.relativize(absoluteFilePath);
        
        String gitStylePath = relativeFilePath.toString().replace("\\", "/");
        IndexEntry newEntry = new IndexEntry(fileMode, blobSha1, gitStylePath);

        // Use IndexManager to add the entry and save the index
        IndexManager indexManager = new IndexManager();     // reads the existing index
        indexManager.addEntry(newEntry);                    // add or update the new entry
        indexManager.writeIndex();                          // write the updated index to disk

        System.out.println("File '" + filePathString + "' staged successfully with SHA-1: " + blobSha1);
    }

    public static void handleBranch(String branchName) throws IOException, IllegalArgumentException {
        Path litPath = Paths.get("").toAbsolutePath().resolve(".lit");
        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            System.err.println("Error: Not a Lit repository (or any of the parent directories): .lit");
            return; // No Git repository found
        }

        ReferenceManager refManager = new ReferenceManager();
        refManager.createBranch(branchName); // Call the createBranch method in ReferenceManager
    }

    public static void handleCommit(String message) throws IOException {
        Path indexPath = Paths.get(".lit/index");

        IndexManager indexManager = new IndexManager();
        List<IndexEntry> indexEntries = indexManager.getIndexEntries();

        if (indexEntries.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean");
            return;
        }

        TreeBuilder treeBuilder = new TreeBuilder();
        TreeObject rootTree = treeBuilder.buildTreeFromIndex(indexEntries);
        
        rootTree.save();
        String treeSha = rootTree.getSha1Id();

        // instead of manually reading files, we use the manager.
        ReferenceManager refManager = new ReferenceManager();
        String parentCommitSha = refManager.getHeadCommit();

        String author = "User Name <user@example.com>";
        String committer = author;
        CommitObject newCommit = new CommitObject(treeSha, parentCommitSha, author, committer, message);

        newCommit.save();
        String newCommitSha = newCommit.getSha1();

        // instead of manually writing to the branch file, we use the manager.
        refManager.updateHead(newCommitSha);
        
        if(Files.exists(indexPath)) {
            Files.delete(indexPath);
        }

        System.out.println("Commit " + newCommitSha + " created.");
    }
}