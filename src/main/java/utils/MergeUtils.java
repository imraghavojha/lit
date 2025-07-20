package utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import objects.CommitObject;
import objects.TreeEntry;
import objects.TreeObject; 

public class MergeUtils {

    public static String findCommonAncestor(String commitSha1, String commitSha2) throws IOException {

        // Gather all ancestors of the first commit.

        System.out.println("Gathering history for commit: " + commitSha1);
        Set<String> ancestorsOf1 = new HashSet<>();
        String currentSha1 = commitSha1;

        // Loop backwards from the starting commit until we reach the root (which has no parent)
        while (currentSha1 != null) {
            ancestorsOf1.add(currentSha1);

            CommitObject currentCommit = ObjectLoader.loadCommit(currentSha1);

            // Check: Ensure the commit object was loaded successfully.
            if (currentCommit == null) {
                throw new IOException("Fatal: Failed to load commit object " + currentSha1 + " while traversing history.");
            }

            // Move to the next parent in the chain.
            currentSha1 = currentCommit.getParentSha1();
        }
        System.out.println("-> Found " + ancestorsOf1.size() + " ancestors.");


        // Walk back through the second commit's history to find the first match
        System.out.println("Searching for common ancestor in history of: " + commitSha2);
        String currentSha2 = commitSha2;
        while (currentSha2 != null) {
            // Check if the current commit is in hashset.
            if (ancestorsOf1.contains(currentSha2)) {
                System.out.println("-> Found common ancestor: " + currentSha2);
                return currentSha2; // SUCCESS: This is the first commit shared by both.
            } 

            CommitObject currentCommit = ObjectLoader.loadCommit(currentSha2);

            // Defensive Check
            if (currentCommit == null) {
                throw new IOException("Fatal: Failed to load commit object " + currentSha2 + " while traversing history.");
            }

            // Move to the next parent in the chain.
            currentSha2 = currentCommit.getParentSha1();
        }

        // If the second loop completes without a match, the histories are unrelated.
        System.out.println("-> No common ancestor found.");
        return null;
    }

    public static TreeDiffResult diffTrees(String baseTreeSha, String otherTreeSha) throws IOException {
        TreeDiffResult result = new TreeDiffResult();
        // First, load the actual TreeObjects from their SHA-1 hashes.
        TreeObject baseTree = ObjectLoader.loadTree(baseTreeSha);
        TreeObject otherTree = ObjectLoader.loadTree(otherTreeSha);

        // Flatten each tree into a map of {filepath -> TreeEntry} to make comparison easy.
        Map<String, TreeEntry> baseFiles = flattenTreeToMap(baseTree);
        Map<String, TreeEntry> otherFiles = flattenTreeToMap(otherTree);

        for (Map.Entry<String, TreeEntry> baseEntry : baseFiles.entrySet()) {
            String filePath = baseEntry.getKey();
            TreeEntry baseFile = baseEntry.getValue();

            // Check if the file from the base tree exists in the new tree
            if (!otherFiles.containsKey(filePath)) {
                // If it doesn't, its deleted
                result.addDeletedFile(baseFile);
            } else {
                // if the file exists in both trees, now checking if its modified
                TreeEntry otherFile = otherFiles.get(filePath);

                // Compare the SHA-1 hashes. If they are different, the file content is modified.
                if (!baseFile.getObjectSha1Id().equals(otherFile.getObjectSha1Id())) {
                    result.addModifiedFile(otherFile); 
                }
            }
        }

        for (Map.Entry<String, TreeEntry> otherEntry : otherFiles.entrySet()) {
            String filePath = otherEntry.getKey();
            TreeEntry otherFile = otherEntry.getValue();

            // If a file from the new tree doesnt exist in the base tree, its a new added file.
            if (!baseFiles.containsKey(filePath)) {
                result.addAddedFile(otherFile);
            }
        }
        
        return result;
    }


    //A recursive helper method to flatten a TreeObject into a map of file paths to their TreeEntry objects.
    private static void flattenTree(TreeObject tree, Path currentPath, Map<String, TreeEntry> fileMap) throws IOException {
        for (TreeEntry entry : tree.getEntries()) {
            Path newPath = currentPath.resolve(entry.getName());
            
            if ("blob".equals(entry.getType())) {
                // Adding only blobs to the map
                fileMap.put(newPath.toString().replace("\\", "/"), entry);
            } else if ("tree".equals(entry.getType())) {
                // If it's a subdirectory (tree), we need to recurse deeper
                TreeObject subTree = ObjectLoader.loadTree(entry.getObjectSha1Id());
                flattenTree(subTree, newPath, fileMap); // Recursive call
            }
        }
    }
    
    private static Map<String, TreeEntry> flattenTreeToMap(TreeObject tree) throws IOException {
        Map<String, TreeEntry> fileMap = new HashMap<>();
        flattenTree(tree, Paths.get(""), fileMap);
        return fileMap;
    }

    // for a three-way merge between two commits
    public static MergeResult merge(String headCommitSha, String otherCommitSha) throws IOException {
        System.out.println("--- Starting Three-Way Merge ---");
        System.out.println("HEAD:  " + headCommitSha);
        System.out.println("OTHER: " + otherCommitSha);

        // Find the common ancestor first (merge base)
        String ancestorSha = findCommonAncestor(headCommitSha, otherCommitSha);

        // Edge cases
        if (ancestorSha == null) {
            System.err.println("Merge failed: No common ancestor found between branches.");
            // Returning a failure with a generic error message.
            return new MergeResult(List.of("FATAL: No common history.")); 
        }

        if (ancestorSha.equals(otherCommitSha)) {
            System.out.println("Merge not necessary: The other branch is already an ancestor of HEAD.");
            return new MergeResult(new ArrayList<>()); // Success without any conflict
        }

        if (ancestorSha.equals(headCommitSha)) {
            System.out.println("Fast-forward merge detected.");
            // the command handler would just move the branch pointer.
            // For the engine, this is a success with no conflicts.
            return new MergeResult(new ArrayList<>()); // Success
        }
        
        System.out.println("Merge Base (Ancestor): " + ancestorSha);

        // Loading all the trees for all 3 commits
        CommitObject headCommit = ObjectLoader.loadCommit(headCommitSha);
        CommitObject otherCommit = ObjectLoader.loadCommit(otherCommitSha);
        CommitObject ancestorCommit = ObjectLoader.loadCommit(ancestorSha);

        String headTreeSha = headCommit.getTreeSha1();
        String otherTreeSha = otherCommit.getTreeSha1();
        String ancestorTreeSha = ancestorCommit.getTreeSha1();

        // changes for each branch relative to the ancestor
        TreeDiffResult headChanges = diffTrees(ancestorTreeSha, headTreeSha);
        TreeDiffResult otherChanges = diffTrees(ancestorTreeSha, otherTreeSha);

        List<String> conflictedFiles = new ArrayList<>();

        // sets of filepaths for efficient lookup
        Set<String> headAdded = headChanges.getAddedFiles().stream().map(TreeEntry::getName).collect(Collectors.toSet());
        Set<String> headModified = headChanges.getModifiedFiles().stream().map(TreeEntry::getName).collect(Collectors.toSet());
        Set<String> headDeleted = headChanges.getDeletedFiles().stream().map(TreeEntry::getName).collect(Collectors.toSet());

        Set<String> otherAdded = otherChanges.getAddedFiles().stream().map(TreeEntry::getName).collect(Collectors.toSet());
        Set<String> otherModified = otherChanges.getModifiedFiles().stream().map(TreeEntry::getName).collect(Collectors.toSet());
        Set<String> otherDeleted = otherChanges.getDeletedFiles().stream().map(TreeEntry::getName).collect(Collectors.toSet());

        // all unique file paths from both sets of changes combined into a master list.
        Set<String> allChangedFiles = new HashSet<>();
        allChangedFiles.addAll(headAdded);
        allChangedFiles.addAll(headModified);
        allChangedFiles.addAll(headDeleted);
        allChangedFiles.addAll(otherAdded);
        allChangedFiles.addAll(otherModified);
        allChangedFiles.addAll(otherDeleted);

        // analyzing file changes
        for (String file : allChangedFiles) {
            boolean isModifiedInHead = headModified.contains(file);
            boolean isModifiedInOther = otherModified.contains(file);
            boolean isDeletedInHead = headDeleted.contains(file);
            boolean isDeletedInOther = otherDeleted.contains(file);
            boolean isAddedInHead = headAdded.contains(file);
            boolean isAddedInOther = otherAdded.contains(file);

            // conflict if a file modified is in both branches
            if ((isModifiedInHead && isModifiedInOther) ||
                (isModifiedInHead && isDeletedInOther) ||
                (isDeletedInHead && isModifiedInOther) ||
                (isAddedInHead && isAddedInOther)) {
                
                System.out.println("CONFLICT: '" + file + "' requires resolution.");
                conflictedFiles.add(file);
                // handleConflict to be called here, needs to be implemented by RO.
                // just recording the conflicted files for now.
                continue;
            }
        }

           // rest of the logic to be added
        System.out.println("\n--- Analysis complete. Conflict resolution and merge application will happen next. ---");

        return new MergeResult(conflictedFiles);
    }
}

