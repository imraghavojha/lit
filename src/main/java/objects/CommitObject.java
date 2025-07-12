package objects;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;

public class CommitObject {

    private final String treeSha1;
    private final String parentSha1; //null for the initial commit
    private final String author;
    private final long authorTimestamp;
    private final String commitMessage;
    private String commitSha1;

    /**
     * Constructs a new CommitObject.
     *
     * @param treeSha1      The SHA-1 hash of the root tree object.
     * @param parentSha1    The SHA-1 hash of the parent commit (null if it's the first commit).
     * @param authorName    The name of the author.
     * @param authorEmail   The email of the author.
     * @param commitMessage The commit message.
     */
    public CommitObject(String treeSha1, String parentSha1, String authorName, String authorEmail, String commitMessage) {
        this.treeSha1 = treeSha1;
        this.parentSha1 = parentSha1;
        this.commitMessage = commitMessage;
        
        this.authorTimestamp = Instant.now().getEpochSecond();

        String timezone = ZoneId.systemDefault().getRules().getOffset(Instant.now()).toString();
        this.author = String.format("%s <%s> %d %s", authorName, authorEmail, authorTimestamp, timezone);
        
        this.commitSha1 = calculateCommitSha1();
    }

    /**
     * Serializes the commit data into the format used by Git for hashing.
     * @return The commit content as a byte array.
     */
    private byte[] serializeContentToBytes() {
        StringBuilder content = new StringBuilder();
        content.append("tree ").append(this.treeSha1).append("\n");

        if (this.parentSha1 != null && !this.parentSha1.isEmpty()) {
            content.append("parent ").append(this.parentSha1).append("\n");
        }

        content.append("author ").append(this.author).append("\n");
        content.append("\n"); // Blank line before commit message
        content.append(this.commitMessage).append("\n");

        try {
            return content.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates the SHA-1 hash for this commit object.
     * @return The 40-character hexadecimal SHA-1 string.
     */
    private String calculateCommitSha1() {
        byte[] contentBytes = serializeContentToBytes();
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(contentBytes);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Helper method to convert a byte array to a hex string.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String getSha1() {
        return this.commitSha1;
    }

    public String getTreeSha1() {
        return treeSha1;
    }

    public String getParentSha1() {
        return parentSha1;
    }
    
    public String getCommitMessage() {
        return commitMessage;
    }

    public void save() {
        String sha1 = getSha1();
        if (sha1 == null || sha1.isEmpty()) {
            System.err.println("Commit SHA-1 is null or empty. Cannot save commit.");
            return;
        }

        java.nio.file.Path objectsDir = java.nio.file.Paths.get(".lit", "objects");
        java.nio.file.Path commitPath = objectsDir.resolve(sha1);

        try {
            if (!java.nio.file.Files.exists(objectsDir)) {
                java.nio.file.Files.createDirectories(objectsDir);
            }
            if (!java.nio.file.Files.exists(commitPath)) {
                byte[] contentBytes = serializeContentToBytes();
                java.nio.file.Files.write(commitPath, contentBytes);
                System.out.println("Commit saved: " + commitPath.toString());
            } else {
                System.out.println("Commit already exists: " + commitPath.toString());
            }
        } catch (java.io.IOException e) {
            System.err.println("Failed to save commit: " + e.getMessage());
        }
    }
}
