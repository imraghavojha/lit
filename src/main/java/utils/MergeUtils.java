package utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        // First, load the actual TreeObjects from their SHA-1 hashes.
        TreeObject baseTree = ObjectLoader.loadTree(baseTreeSha);
        TreeObject otherTree = ObjectLoader.loadTree(otherTreeSha);

        // Flatten each tree into a map of {filepath -> TreeEntry}. This makes comparison easy.
        Map<String, TreeEntry> baseFiles = flattenTreeToMap(baseTree);
        Map<String, TreeEntry> otherFiles = flattenTreeToMap(otherTree);

        // the comparison logic will be here...
        
        // returning an empty result for now
        return new TreeDiffResult(); 
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
}
