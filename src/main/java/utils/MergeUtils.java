package utils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import objects.CommitObject;

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
}
