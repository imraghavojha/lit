package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import objects.CommitObject;
import objects.TreeEntry;
import objects.TreeObject;


public class ConflictHandler {
    
    /**
     * basically writes the file to the working directory with conflict markers during merge confilt
     * 
     * @param filePath path of the conflicted file
     * @param headCommitSha SHA-1 of the HEAD commit
     * @param otherCommitSha SHA-1 of the other branch's commit
     * @param ancestorCommitSha SHA-1 of the common ancestor commit
     * @param otherBranchName name of the branch being merged
     * @throws IOException If there's an error reading blobs or writing files
     */
    public static void handleConflict(String filePath, String headCommitSha, 
                                     String otherCommitSha, String ancestorCommitSha,
                                     String otherBranchName) throws IOException {
        
        System.out.println("Handling conflict for: " + filePath);
        
        // get the file content from each version
        String headContent = getFileContentFromCommit(headCommitSha, filePath);
        String otherContent = getFileContentFromCommit(otherCommitSha, filePath);
        String ancestorContent = getFileContentFromCommit(ancestorCommitSha, filePath);
        
        // build the conflicted file content with markers
        String conflictedContent = buildConflictedContent(filePath, headContent, 
                                                         otherContent, otherBranchName);
        
        // write the conflicted file to the working directory
        Path targetPath = Path.of(filePath);
        writeConflictedFile(targetPath, conflictedContent);
    }
    
    /**
     * gets file content from a specific commit
     * 
     * @param commitSha commit SHA-1
     * @param filePath file path
     * @return file content as a string, or null if file doesn't exist in that commit
     */
    private static String getFileContentFromCommit(String commitSha, String filePath) 
            throws IOException {
        
        if (commitSha == null) {
            return null; 
        }
        
        // load the commit and its tree
        CommitObject commit = ObjectLoader.loadCommit(commitSha);
        if (commit == null) {
            return null;
        }
        
        TreeObject rootTree = ObjectLoader.loadTree(commit.getTreeSha1());
        if (rootTree == null) {
            return null;
        }
        
        // find the blob SHA for this file path
        String blobSha = findBlobShaInTree(rootTree, filePath);
        if (blobSha == null) {
            return null; // File doesn't exist in this commit
        }
        
        // load the blob content
        byte[] blobContent = ObjectLoader.loadBlob(blobSha);
        return new String(blobContent);
    }
    
    /**
     * searches for a file's blob SHA in a tree structure using recursion
     * 
     * @param tree The tree to search in
     * @param filePath The file path to find
     * @return blob SHA-1 if found, null otherwise
     */
    private static String findBlobShaInTree(TreeObject tree, String filePath) 
            throws IOException {
        
        String[] pathParts = filePath.split("/");
        return findBlobShaInTreeRecursive(tree, pathParts, 0);
    }
    
    private static String findBlobShaInTreeRecursive(TreeObject tree, String[] pathParts, 
                                                     int currentIndex) throws IOException {
        if (currentIndex >= pathParts.length) {
            return null;
        }
        
        String currentPart = pathParts[currentIndex];
        
        for (TreeEntry entry : tree.getEntries()) {
            if (entry.getName().equals(currentPart)) {
                if (currentIndex == pathParts.length - 1) {
                    // should be the file we are looking for
                    if (entry.getType().equals("blob")) {
                        return entry.getObjectSha1Id();
                    }
                } else {
                    if (entry.getType().equals("tree")) {
                        TreeObject subTree = ObjectLoader.loadTree(entry.getObjectSha1Id());
                        return findBlobShaInTreeRecursive(subTree, pathParts, currentIndex + 1);
                    }
                }
            }
        }
        
        return null; 
    }
    
    /**
     * writes the content of a conflicted file with proper conflict markers
     */
    private static String buildConflictedContent(String filePath, String headContent, 
                                                String otherContent, String otherBranchName) {
        StringBuilder conflicted = new StringBuilder();
        
        if (headContent == null && otherContent != null) {
            conflicted.append("<<<<<<< HEAD\n");
            conflicted.append("=======\n");
            conflicted.append(otherContent);
            if (!otherContent.endsWith("\n")) {
                conflicted.append("\n");
            }
            conflicted.append(">>>>>>> ").append(otherBranchName).append("\n");
        } else if (headContent != null && otherContent == null) {
            conflicted.append("<<<<<<< HEAD\n");
            conflicted.append(headContent);
            if (!headContent.endsWith("\n")) {
                conflicted.append("\n");
            }
            conflicted.append("=======\n");
            conflicted.append(">>>>>>> ").append(otherBranchName).append("\n");
        } else if (headContent != null && otherContent != null) {
            conflicted.append("<<<<<<< HEAD\n");
            conflicted.append(headContent);
            if (!headContent.endsWith("\n")) {
                conflicted.append("\n");
            }
            conflicted.append("=======\n");
            conflicted.append(otherContent);
            if (!otherContent.endsWith("\n")) {
                conflicted.append("\n");
            }
            conflicted.append(">>>>>>> ").append(otherBranchName).append("\n");
        }
        
        return conflicted.toString();
    }
    
    /**
     * writes the conflicted content to a file in the working directory
     * makes parent directories if necessary
     * 
     * @param targetPath path where to write the file
     * @param content conflicted content with markers
     */
    private static void writeConflictedFile(Path targetPath, String content) 
            throws IOException {
        
        // checking if parent directories exist
        Path parentDir = targetPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        
        // writing the conflicted file
        Files.writeString(targetPath, content, 
                         StandardOpenOption.CREATE, 
                         StandardOpenOption.TRUNCATE_EXISTING);
        
        System.out.println("Wrote conflicted file: " + targetPath);
    }
    
    /**
     * checks if there are any unresolved conflicts in the working directory.
     * this can be used to prevent commits when conflicts exist.
     * 
     * @return true if conflicts exist, false otherwise
     */
    public static boolean hasUnresolvedConflicts() throws IOException {
        return false; // Placeholder
    }
}