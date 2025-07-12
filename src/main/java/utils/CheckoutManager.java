package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream; 
import objects.CommitObject;
import objects.TreeObject;
import objects.TreeEntry;
import objects.IndexEntry; 

public class CheckoutManager {

    public static void checkout(String targetRef) throws IOException, IllegalArgumentException {
        Path currentWorkingDir = Paths.get("").toAbsolutePath();
        Path litPath = currentWorkingDir.resolve(".lit");

        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            throw new IOException("Not a Lit repository (or any of the parent directories): .lit");
        }

        ReferenceManager refManager = new ReferenceManager();

        String targetCommitSha;
        boolean isTargetBranch = false;
        Path branchPathFromRef = litPath.resolve("refs").resolve("heads").resolve(targetRef); // Potential branch path

        if (Files.exists(branchPathFromRef) && Files.isRegularFile(branchPathFromRef)) {
            targetCommitSha = Files.readString(branchPathFromRef).trim();
            if (targetCommitSha.isEmpty()) {
                throw new IllegalArgumentException("Branch '" + targetRef + "' exists but points to no commit.");
            }
            isTargetBranch = true;
            System.out.println("Switching to branch: " + targetRef);
        } else if (targetRef.matches("[0-9a-fA-F]{40}")) {
            targetCommitSha = targetRef;
            System.out.println("Switching to commit: " + targetRef);
        } else {
            throw new IllegalArgumentException("Reference '" + targetRef + "' not found or is invalid.");
        }

        // Load the Target Commit Object and its Root Tree
        CommitObject targetCommit = ObjectLoader.loadCommit(targetCommitSha);
        if (targetCommit == null) {
            throw new IOException("Could not load target commit object: " + targetCommitSha);
        }
        String targetTreeSha = targetCommit.getTreeSha1();
        TreeObject targetTree = ObjectLoader.loadTree(targetTreeSha);
        if (targetTree == null) {
            throw new IOException("Could not load target tree object: " + targetTreeSha + " from commit " + targetCommitSha);
        }


        // Working Directory Reconciliation 
        System.out.println("Clearing working directory (excluding .lit folder)...");
        // Iterate through all items in the current working directory
        try (Stream<Path> walk = Files.list(currentWorkingDir)) {
            walk.filter(p -> !p.equals(litPath) && !p.equals(Paths.get(currentWorkingDir.toString(), ".git"))) // Exclude .lit and potential .git folders
                .forEach(p -> {
                    try {
                        if (Files.isDirectory(p)) {
                            WorkingDirManager.deleteDirectory(p); // Recursively delete directories
                        } else {
                            WorkingDirManager.deleteFile(p);      // Delete individual files
                        }
                    } catch (IOException | RuntimeException e) {
                        System.err.println("Error deleting " + p + " during checkout: " + e.getMessage());
                    }
                });
        }

        System.out.println("Reconstructing working directory from commit: " + targetCommitSha);
        // Recursively reconstruct the working directory based on the targetTree
        reconstructWorkingDirectory(targetTree, currentWorkingDir);


        // Update HEAD and Index 
        System.out.println("Updating HEAD...");
        // For now, updating HEAD based on whether targetRef was a branch or direct SHA.
        refManager.setHead(targetRef, isTargetBranch);

        System.out.println("Rebuilding index...");
        // Rebuild the index based on the new tree.
        IndexManager indexManager = new IndexManager(); // Loads existing index (now mostly empty)
        // Clear the current index in memory before rebuilding from the tree
        indexManager.getIndexEntries().clear(); // Clear existing entries in memory
        rebuildIndexFromTree(targetTree, indexManager, Paths.get("")); // Pass empty path for initial relative path

        indexManager.writeIndex(); 

        System.out.println("Switched to '" + targetRef + "' successfully.");
    }

    private static void reconstructWorkingDirectory(TreeObject tree, Path currentPath) throws IOException {
        for (TreeEntry entry : tree.getEntries()) {
            Path entryPath = currentPath.resolve(entry.getName());
            if (entry.getType().equals("blob")) {
                // It's a file, write its content to the working directory
                WorkingDirManager.writeBlobToWorkingDir(entry.getObjectSha1Id(), entryPath);
            } else if (entry.getType().equals("tree")) {
                // It's a subdirectory, create it and recurse
                Files.createDirectories(entryPath); // Ensure directory exists
                TreeObject subTree = ObjectLoader.loadTree(entry.getObjectSha1Id());
                reconstructWorkingDirectory(subTree, entryPath); // Recursive call for subtree
            }
        }
    }

    private static void rebuildIndexFromTree(TreeObject tree, IndexManager indexManager, Path currentRelativePath) throws IOException {
        for (TreeEntry entry : tree.getEntries()) {
            Path entryRelativePath = currentRelativePath.resolve(entry.getName());
            String gitStylePath = entryRelativePath.toString().replace("\\", "/"); // Ensure Git-style path separators

            if (entry.getType().equals("blob")) {
                // Add blob entry to index
                indexManager.addEntry(new IndexEntry(entry.getMode(), entry.getObjectSha1Id(), gitStylePath));
            } else if (entry.getType().equals("tree")) {
                // Recursively rebuild index for subtree
                TreeObject subTree = ObjectLoader.loadTree(entry.getObjectSha1Id());
                rebuildIndexFromTree(subTree, indexManager, entryRelativePath);
            }
        }
    }
}