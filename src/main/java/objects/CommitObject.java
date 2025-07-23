package objects;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import utils.Content;

public class CommitObject {

    private final String treeSha1;
    private final List<String> parentSha1s; 
    private final String author;
    private long authorTimestamp; // made this non-final
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
    this(treeSha1, (parentSha1 == null || parentSha1.isEmpty()) ? java.util.Collections.emptyList() : java.util.Collections.singletonList(parentSha1), authorName, authorEmail, commitMessage);
    }

    // new constructor for multiple parents
    public CommitObject(String treeSha1, List<String> parentSha1s, String authorName, String authorEmail, String commitMessage) {
        this.treeSha1 = treeSha1;
        this.parentSha1s = parentSha1s;
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

        // handle multiple parents
        if (this.parentSha1s != null) {
            for (String parent : this.parentSha1s) {
                if (parent != null && !parent.isEmpty()) {
                    content.append("parent ").append(parent).append("\n");
                }
            }
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

    public void setAuthorTimestamp(long timestamp) {
        this.authorTimestamp = timestamp;
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

     public List<String> getParentSha1s() {
        return parentSha1s;
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
        try {
            Content.saveObject(sha1, serializeContentToBytes());
        } catch (java.io.IOException e) {
            System.err.println("Failed to save commit: " + e.getMessage());
        }
    }
}
