package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import objects.CommitObject;
import objects.TreeObject;
import objects.TreeEntry;
import java.util.List;

public class CheckoutManager {
    public static void checkout(String targetRef) throws IOException, IllegalArgumentException {
        Path litPath = Paths.get("").toAbsolutePath().resolve(".lit");
        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            throw new IOException("Not a Lit repository (or any of the parent directories): .lit");
        }

        ReferenceManager refManager = new ReferenceManager();

        String targetCommitSha;
        Path branchPath = litPath.resolve("refs").resolve("heads").resolve(targetRef);
        if (Files.exists(branchPath) && Files.isRegularFile(branchPath)) {
            targetCommitSha = Files.readString(branchPath).trim();
            if (targetCommitSha.isEmpty()) {
                throw new IllegalArgumentException("Branch '" + targetRef + "' exists but points to no commit.");
            }
            System.out.println("Switching to branch: " + targetRef);
        } else if (targetRef.matches("[0-9a-fA-F]{40}")) {
            targetCommitSha = targetRef;
            System.out.println("Switching to commit: " + targetRef);
        } else {
            throw new IllegalArgumentException("Reference '" + targetRef + "' not found or is invalid.");
        }

        // Load the Target Commit Object
        CommitObject targetCommit = ObjectLoader.loadCommit(targetCommitSha);
        if (targetCommit == null) {
            throw new IOException("Could not load target commit object: " + targetCommitSha);
        }

        // Load the Target Root Tree Object
        String targetTreeSha = targetCommit.getTreeSha1();
        TreeObject targetTree = ObjectLoader.loadTree(targetTreeSha);
        if (targetTree == null) {
            throw new IOException("Could not load target tree object: " + targetTreeSha + " from commit " + targetCommitSha);
        }
    }

}
