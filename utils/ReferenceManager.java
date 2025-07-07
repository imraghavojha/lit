package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReferenceManager {
    private Path litPath;           // Path to .lit directory
    private Path headPath;          // Path to .lit/HEAD
    private Path refsHeadsPath;     // Path to .lit/refs/heads

    public ReferenceManager() throws IOException {
        Path currentDirectory = Paths.get("").toAbsolutePath();
        this.litPath = currentDirectory.resolve(".lit");

        // Validate that .lit repository exists
        if (!Files.exists(litPath) || !Files.isDirectory(litPath)) {
            throw new IOException("Not a Lit repository (or any of the parent directories): .lit");
        }

        this.headPath = litPath.resolve("HEAD");
        this.refsHeadsPath = litPath.resolve("refs").resolve("heads");

        // Basic check for HEAD existence during construction
        if (!Files.exists(headPath)) {
            throw new IOException(".lit/HEAD file not found. Repository might be corrupted or not properly initialized.");
        }
    }

    public String getHeadCommit() throws IOException {
        // Make sure repo is valid
        if (!Files.exists(headPath) || !Files.exists(refsHeadsPath)) {
            throw new IOException("Repository structure invalid. Missing HEAD or refs/heads.");
        }

        String headContent = Files.readString(headPath).trim();

        if (headContent.startsWith("ref: ")) {
            String refPathString = headContent.substring("ref: ".length());
            Path branchFilePath = litPath.resolve(refPathString);
            
            if (Files.exists(branchFilePath)) {
                String commitSha = Files.readString(branchFilePath).trim();
                if (commitSha.isEmpty()) {
                    return null; // Branch exists but points to no commit (e.g., after init, before first commit)
                }
                return commitSha;
            } else {
                // If branch file doesn't exist, HEAD points to a non-existent branch
                System.err.println("Warning: HEAD points to a non-existent branch: " + refPathString);
                return null;
            }
        } else if (headContent.matches("[0-9a-fA-F]{40}")) {
            // It's a detached HEAD, pointing directly to a commit SHA-1
            return headContent;
        } else {
            System.err.println("Warning: Invalid content in .lit/HEAD file: " + headContent);
            return null; // Invalid HEAD content
        }
    }

    public void updateHead(String newCommitSha) throws IOException, IllegalArgumentException {
        // Basic validation for SHA-1 format
        if (newCommitSha == null || !newCommitSha.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid commit SHA-1: " + newCommitSha);
        }

        // Ensure the repository is valid before proceeding
        if (!Files.exists(headPath) || !Files.exists(refsHeadsPath)) {
            throw new IOException("Repository structure invalid. Missing HEAD or refs/heads.");
        }

        String headContent = Files.readString(headPath).trim();

        if (headContent.startsWith("ref: ")) {
            // HEAD points to a branch (e.g., "ref: refs/heads/main")
            String refPathString = headContent.substring("ref: ".length());
            Path branchFilePath = litPath.resolve(refPathString);

            // Ensure the branch file exists before attempting to write to it
            if (!Files.exists(branchFilePath)) {
                throw new IOException("Cannot update branch: '" + refPathString + "' does not exist.");
            }

            // Write the new commit SHA-1 to the branch file
            Files.writeString(branchFilePath, newCommitSha);
            System.out.println("Branch updated: " + refPathString + " now points to " + newCommitSha);

        } else if (headContent.matches("[0-9a-fA-F]{40}")) {
            // Detached HEAD, so write the new SHA-1 directly to the HEAD file
            Files.writeString(headPath, newCommitSha);
            System.out.println("Detached HEAD updated to: " + newCommitSha);
        } else {
            throw new IOException("Invalid content in .lit/HEAD file. Cannot update HEAD.");
        }
    }

    public void createBranch(String branchName) throws IOException, IllegalArgumentException {
        // Basic validation for branch name
        if (branchName == null || branchName.trim().isEmpty() || branchName.contains(" ") || branchName.contains("/") || branchName.equals("HEAD") || branchName.equals("main")) {
            throw new IllegalArgumentException("Invalid branch name: " + branchName + ". Branch names cannot contain spaces, slashes, or be 'HEAD' or 'main'.");
        }

        // Ensure refs/heads directory exists
        if (!Files.exists(refsHeadsPath) || !Files.isDirectory(refsHeadsPath)) {
            throw new IOException("Repository structure invalid. Missing refs/heads directory.");
        }

        Path newBranchFilePath = refsHeadsPath.resolve(branchName);

        // Check if branch already exists
        if (Files.exists(newBranchFilePath)) {
            throw new IOException("Branch '" + branchName + "' already exists.");
        }

        // Get the SHA-1 of the current HEAD commit
        String headCommitSha = getHeadCommit();

        // If there are no commits yet (e.g., after init, before first commit), the new branch points to nothing initially
        // This is how Git behaves: it creates the file but it's empty.
        String contentToWrite = (headCommitSha != null) ? headCommitSha : "";

        // Create the new branch file and write the HEAD commit SHA-1 (or empty string) to it
        Files.writeString(newBranchFilePath, contentToWrite);
        System.out.println("Branch '" + branchName + "' created pointing to: " + (headCommitSha != null ? headCommitSha : "(initial commit)"));
    }
}