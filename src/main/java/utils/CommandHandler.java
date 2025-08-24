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

    public static void handleLog() throws IOException {
        Path litPath = Paths.get("").toAbsolutePath().resolve(".lit");
        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            System.err.println("fatal: not a lit repository (or any of the parent directories)");
            return;
        }

        ReferenceManager refManager = new ReferenceManager();
        String currentCommitSha = refManager.getHeadCommit();

        if (currentCommitSha == null) {
            System.out.println("No commits yet.");
            return;
        }

        while (currentCommitSha != null) {
            CommitObject commit = ObjectLoader.loadCommit(currentCommitSha);
            if (commit == null) {
                System.err.println("Error: Could not load commit object " + currentCommitSha);
                break;
            }

            System.out.println("Commit " + commit.getSha1());
            System.out.println("Author: " + commit.getAuthor()); 
            System.out.println("Date: " + commit.getAuthorTimestamp());
            System.out.println("\n    " + commit.getCommitMessage() + "\n");

            // Move to the first parent to continue the traversal
            if (commit.getParentSha1s() != null && !commit.getParentSha1s().isEmpty()) {
                currentCommitSha = commit.getParentSha1s().get(0);
            } else {
                currentCommitSha = null; // No more parents, end the traversal
            }
        }
    }

    // Handles the 'diff' command with no arguments: compares the index and the working directory
    public static void handleDiffIndexAndWorkingDir() throws IOException {
        System.out.println("Comparing index with working directory...");
        IndexManager indexManager = new IndexManager();
        List<IndexEntry> indexEntries = indexManager.getIndexEntries();
        FileDiffer fileDiffer = new FileDiffer();

        boolean hasDiff = false;
        for (IndexEntry entry : indexEntries) {
            Path filePath = Paths.get(entry.getFilePath());
            if (Files.exists(filePath)) {
                String indexContent = new String(ObjectLoader.loadBlob(entry.getSha1()));
                String workingContent = new String(Files.readAllBytes(filePath));

                DiffResult diffResult = fileDiffer.calculateDiff(indexContent, workingContent);
                if (diffResult.hasChanges()) {
                    System.out.println("--- a/" + entry.getFilePath());
                    System.out.println("+++ b/" + entry.getFilePath());
                    diffResult.getDiffLines().forEach(line -> System.out.println(line.type == ChangeType.ADDED ? "+" + line.text : "-" + line.text));
                    System.out.println();
                    hasDiff = true;
                }
            }
        }
        if (!hasDiff) {
            System.out.println("No changes found.");
        }
    }

    //Handles 'diff' with one argument: compares a commit and the working directory
    public static void handleDiffCommitAndWorkingDir(String commitOrBranch) throws IOException {
        System.out.println("Comparing " + commitOrBranch + " with working directory...");
        ReferenceManager refManager = new ReferenceManager();
        String commitSha = refManager.getBranchCommit(commitOrBranch);
        if (commitSha == null) {
            // Assume it's a direct SHA
            commitSha = commitOrBranch;
        }

        CommitObject commit = ObjectLoader.loadCommit(commitSha);
        if (commit == null) {
            System.err.println("Error: Commit '" + commitOrBranch + "' not found.");
            return;
        }

        TreeObject tree = ObjectLoader.loadTree(commit.getTreeSha1());
        FileDiffer fileDiffer = new FileDiffer();

        boolean hasDiff = false;
        for (TreeEntry entry : tree.getEntries()) {
            if ("blob".equals(entry.getType())) {
                Path filePath = Paths.get(entry.getName());
                if (Files.exists(filePath)) {
                    String commitContent = new String(ObjectLoader.loadBlob(entry.getObjectSha1Id()));
                    String workingContent = new String(Files.readAllBytes(filePath));

                    DiffResult diffResult = fileDiffer.calculateDiff(commitContent, workingContent);
                    if (diffResult.hasChanges()) {
                        System.out.println("--- a/" + entry.getName());
                        System.out.println("+++ b/" + entry.getName());
                        diffResult.getDiffLines().forEach(line -> System.out.println(line.type == ChangeType.ADDED ? "+" + line.text : "-" + line.text));
                        System.out.println();
                        hasDiff = true;
                    }
                }
            }
        }
        if (!hasDiff) {
            System.out.println("No changes found.");
        }
    }

    // Handles 'diff' with two arguments: compares two commits
    public static void handleDiffCommits(String commit1, String commit2) throws IOException {
        System.out.println("Comparing commits " + commit1 + " and " + commit2 + "...");
        ReferenceManager refManager = new ReferenceManager();
        
        String sha1 = refManager.getBranchCommit(commit1);
        if (sha1 == null) {
            sha1 = commit1;
        }
        
        String sha2 = refManager.getBranchCommit(commit2);
        if (sha2 == null) {
            sha2 = commit2;
        }

        CommitObject commitObj1 = ObjectLoader.loadCommit(sha1);
        CommitObject commitObj2 = ObjectLoader.loadCommit(sha2);

        if (commitObj1 == null || commitObj2 == null) {
            System.err.println("Error: Could not find one or both commits.");
            return;
        }

        TreeObject tree1 = ObjectLoader.loadTree(commitObj1.getTreeSha1());
        TreeObject tree2 = ObjectLoader.loadTree(commitObj2.getTreeSha1());
        FileDiffer fileDiffer = new FileDiffer();

        TreeDiffResult diffResult = MergeUtils.diffTrees(tree1.getSha1Id(), tree2.getSha1Id());
        
        boolean hasDiff = false;
        for (TreeDiffResult.TreeEntryWithPath entry : diffResult.getModifiedFiles()) {
            String filePath = entry.getFullPath();
            String content1 = new String(ObjectLoader.loadBlob(entry.getEntry().getObjectSha1Id()));
            String content2 = new String(ObjectLoader.loadBlob(ObjectLoader.loadTree(tree2.getSha1Id()).getEntries().stream().filter(e -> e.getName().equals(entry.getEntry().getName())).findFirst().get().getObjectSha1Id()));
            
            DiffResult fileDiff = fileDiffer.calculateDiff(content1, content2);
            if (fileDiff.hasChanges()) {
                System.out.println("--- a/" + filePath);
                System.out.println("+++ b/" + filePath);
                fileDiff.getDiffLines().forEach(line -> System.out.println(line.type == ChangeType.ADDED ? "+" + line.text : "-" + line.text));
                System.out.println();
                hasDiff = true;
            }
        }
        if (!hasDiff) {
            System.out.println("No changes found.");
        }
    }
}