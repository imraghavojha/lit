package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import objects.BlobObject;
import objects.CommitObject;
import objects.IndexEntry;
import objects.TreeEntry;
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

    public static void handleRm(String filePathString) throws IOException {
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

        // Get the file path relative to the repository root
        Path relativeFilePath = currentDirectory.relativize(absoluteFilePath);
        String gitStylePath = relativeFilePath.toString().replace("\\", "/");

        // Load the index
        IndexManager indexManager = new IndexManager();
        
        // Check if file is in the index
        boolean fileInIndex = indexManager.getIndexEntries().stream()
                .anyMatch(entry -> entry.getFilePath().equals(gitStylePath));
        
        if (!fileInIndex) {
            System.err.println("Error: pathspec '" + filePathString + "' did not match any files in index.");
            return;
        }

        // marks deleted in index with mode "0" and sha1 "0"
        IndexEntry deletionEntry = new IndexEntry("0", "0", gitStylePath);
        indexManager.addEntry(deletionEntry);
        indexManager.writeIndex();

        // Delete the file from working directory if it exists
        if (Files.exists(absoluteFilePath)) {
            Files.delete(absoluteFilePath);
        }

        System.out.println("rm '" + filePathString + "'");
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

        ReferenceManager refManager = new ReferenceManager();
        String parentCommitSha = refManager.getHeadCommit();

        String authorName = "User Name";
        String authorEmail = "user@example.com";
        CommitObject newCommit = new CommitObject(treeSha, parentCommitSha, authorName, authorEmail, message);

        newCommit.save();
        String newCommitSha = newCommit.getSha1();

        refManager.updateHead(newCommitSha);
        
        // Clean up deletion markers from index after successful commit
        List<IndexEntry> remainingEntries = indexEntries.stream()
                .filter(entry -> !entry.isDeleted())
                .collect(java.util.stream.Collectors.toList());
        
        if (remainingEntries.isEmpty()) {
            if(Files.exists(indexPath)) {
                Files.delete(indexPath);
            }
        } else {
            indexManager.getIndexEntries().clear();
            remainingEntries.forEach(indexManager::addEntry);
            indexManager.writeIndex();
        }

        System.out.println("Commit " + newCommitSha + " created.");
    }

    public static void handleSwitch(String targetRef) throws IOException, IllegalArgumentException {
        Path litPath = Paths.get("").toAbsolutePath().resolve(".lit");
        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            throw new IOException("Error: Not a Lit repository (or any of the parent directories): .lit");
        }
        CheckoutManager.checkout(targetRef);
    }

    public static void handleMergeCommit(String message, String otherBranchName) throws IOException {
        ReferenceManager refManager = new ReferenceManager();
        String currentBranchSha = refManager.getHeadCommit();
        String otherBranchSha = refManager.getBranchCommit(otherBranchName);

        if (otherBranchSha == null) {
            System.err.println("Error: Branch '" + otherBranchName + "' not found or has no commits.");
            return;
        }

        List<String> parents = List.of(currentBranchSha, otherBranchSha);
        CommitObject currentCommit = ObjectLoader.loadCommit(currentBranchSha);
        String treeSha = currentCommit.getTreeSha1();

        String authorName = "User Name";
        String authorEmail = "user@example.com";
        CommitObject mergeCommit = new CommitObject(treeSha, parents, authorName, authorEmail, message);
        
        mergeCommit.save();
        refManager.updateHead(mergeCommit.getSha1());

        System.out.println("Merged " + otherBranchName + " into current branch. New merge commit: " + mergeCommit.getSha1());
    }

    public static void handleStatus() throws IOException {
        Path litPath = Paths.get("").toAbsolutePath().resolve(".lit");
        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            System.err.println("fatal: not a lit repository (or any of the parent directories)");
            return;
        }

        ReferenceManager refManager = new ReferenceManager();
        IndexManager indexManager = new IndexManager();

        String headCommitSha = refManager.getHeadCommit();
        Map<String, String> headTreeEntries = null;

        if (headCommitSha != null) {
            CommitObject headCommit = ObjectLoader.loadCommit(headCommitSha);
            if (headCommit != null) {
                TreeObject headTree = ObjectLoader.loadTree(headCommit.getTreeSha1());
                headTreeEntries = headTree.getEntries().stream()
                    .collect(Collectors.toMap(
                        TreeEntry::getName,
                        TreeEntry::getObjectSha1Id
                    ));
            }
        }

        // map of current index entries for easy lookup
        Map<String, IndexEntry> indexMap = indexManager.getIndexEntries().stream()
            .collect(Collectors.toMap(
                IndexEntry::getFilePath, 
                entry -> entry
            ));

        // all the files in the working directory
        Path currentDirectory = Paths.get("").toAbsolutePath();
        Set<String> workingDirFiles = listFilesRecursive(currentDirectory);
        
        boolean isClean = true;
        // Check for unmerged paths (conflicts)
        if (ConflictHandler.hasUnresolvedConflicts()) {
            System.out.println("Unmerged paths:");
            System.out.println("  (fix conflicts and run \"lit commit\")");
            // conflicthandler would list the files here
            isClean = false;
        }

        // Changes to be committed (Index vs. HEAD)
        System.out.println("\nChanges to be committed:");
        // Find changes in the index that are not in the HEAD commit
        Set<String> stagedFiles = indexMap.keySet();
        for (String filePath : stagedFiles) {
            IndexEntry entry = indexMap.get(filePath);

            // Staged for deletion
            if (entry.isDeleted()) {
                if (headTreeEntries != null && headTreeEntries.containsKey(filePath)) {
                    System.out.println("  deleted:    " + filePath);
                    isClean = false;
                }
                continue;
            }

            // Staged new file
            if (headTreeEntries == null || !headTreeEntries.containsKey(filePath)) {
                System.out.println("  new file:   " + filePath);
                isClean = false;
            } 
            // Staged modification
            else if (!headTreeEntries.get(filePath).equals(entry.getSha1())) {
                System.out.println("  modified:   " + filePath);
                isClean = false;
            }
        }

        // Changes not staged for commit (Working Dir vs. Index)
        System.out.println("\nChanges not staged for commit:");
        
        // Find changes in the working directory that are not yet staged
        for (String filePath : workingDirFiles) {
            Path absolutePath = Paths.get(filePath);
            
            // Skip the .lit directory itself
            if (absolutePath.startsWith(litPath)) {
                continue;
            }
            try {
                String workingFileSha = new BlobObject(filePath).getSha1();
                IndexEntry indexEntry = indexMap.get(filePath);

                // File is in the index but modified in the working directory
                if (indexEntry != null && !workingFileSha.equals(indexEntry.getSha1())) {
                    System.out.println("  modified:   " + filePath);
                    isClean = false;
                }
            } catch (Exception e) {
                // Ignoring any issues with calculating SHA for now
            }
        }

        // Untracked files
        System.out.println("\nUntracked files:");
        
        // Find files in the working directory that are not in the index
        for (String filePath : workingDirFiles) {
            if (!indexMap.containsKey(filePath)) {
                System.out.println("  " + filePath);
                isClean = false;
            }
        }

        if (isClean) {
            System.out.println("\nworking tree clean");
        }
    }

    // Helper to get a set of all file paths in the current directory and subdirectories.
    private static Set<String> listFilesRecursive(Path rootDir) throws IOException {
        Set<String> filePaths = new java.util.HashSet<>();
        try (Stream<Path> walk = Files.walk(rootDir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> !p.startsWith(rootDir.resolve(".lit")))
                .forEach(path -> {
                    String relativePath = rootDir.relativize(path).toString().replace("\\", "/");
                    filePaths.add(relativePath);
                });
        }
        return filePaths;
    }
}