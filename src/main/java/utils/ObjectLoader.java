package utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import objects.CommitObject;
import objects.TreeObject;   
import objects.TreeEntry;

public class ObjectLoader {

    public static byte[] readObject(String sha1) throws IOException {
        if (sha1 == null || sha1.length() != 40 || !sha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid SHA-1 hash format: " + sha1);
        }

        // Construct the path to the object file: .lit/objects/firstTwo/restOfSha1
        Path objectFilePath = Paths.get(".lit", "objects", sha1);

        if (!Files.exists(objectFilePath)) {
            throw new IOException("Object not found: " + sha1 + " at " + objectFilePath.toAbsolutePath());
        }

        return Files.readAllBytes(objectFilePath);
    }

    public static CommitObject loadCommit(String commitSha1) throws IOException, IllegalArgumentException {
        // Validate SHA-1 format (readObject also validates, but good to have here too)
        if (commitSha1 == null || commitSha1.length() != 40 || !commitSha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid commit SHA-1 format: " + commitSha1);
        }

        byte[] rawContent = readObject(commitSha1);
        String contentString = new String(rawContent); // Assuming commit objects are UTF-8 encoded, or ASCII for basic fields

        String treeSha1 = null;
        String parentSha1 = null;
        String authorName = null;
        String authorEmail = null;
        String commitMessage = "";

        // Split the content by lines
        String[] lines = contentString.split("\n");
        int messageStartIndex = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("tree ")) {
                treeSha1 = line.substring("tree ".length());
            } else if (line.startsWith("parent ")) {
                parentSha1 = line.substring("parent ".length());
            } else if (line.startsWith("author ")) {
                // The author line is like "author User Name <user@example.com> 1234567890 +0000"
                // The CommitObject constructor needs name and email separately.
                String authorLine = line.substring("author ".length());
                int emailStart = authorLine.indexOf("<");
                int emailEnd = authorLine.indexOf(">");
                if (emailStart != -1 && emailEnd != -1 && emailEnd > emailStart) {
                    authorName = authorLine.substring(0, emailStart).trim();
                    authorEmail = authorLine.substring(emailStart + 1, emailEnd).trim();
                } else {
                    // Fallback if email format is unexpected
                    authorName = authorLine.split(" ")[0]; // Just take first word as name
                    authorEmail = "unknown@example.com";
                }
            } else if (line.trim().isEmpty() && messageStartIndex == -1) {
                // This is the blank line before the commit message
                messageStartIndex = i + 1;
            }
        }

        // Reconstruct the commit message from the identified start index
        if (messageStartIndex != -1) {
            StringBuilder msgBuilder = new StringBuilder();
            for (int i = messageStartIndex; i < lines.length; i++) {
                msgBuilder.append(lines[i]).append("\n");
            }
            commitMessage = msgBuilder.toString().trim(); // Trim trailing newline if any
        }
        
        // Basic validation before creating the CommitObject
        if (treeSha1 == null || authorName == null || authorEmail == null) {
            throw new IOException("Malformed commit object: Missing essential fields for SHA-1 " + commitSha1);
        }

        // CommitObject's constructor takes authorName, authorEmail, it will format itself.
        // It also recalculates its SHA-1 upon construction, which should match the commitSha1 loaded.
        CommitObject commit = new CommitObject(treeSha1, parentSha1, authorName, authorEmail, commitMessage);
        
        // Verify loaded SHA-1 matches calculated SHA-1
        if (!commit.getSha1().equals(commitSha1)) {
            System.err.println("Warning: Loaded commit SHA-1 " + commitSha1 + " does not match calculated SHA-1 " + commit.getSha1() + " for content.");
        }
        
        return commit;
    }

    public static TreeObject loadTree(String treeSha1) throws IOException, IllegalArgumentException {
        if (treeSha1 == null || treeSha1.length() != 40 || !treeSha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid tree SHA-1 format: " + treeSha1);
        }

        byte[] rawContent = readObject(treeSha1);
        List<TreeEntry> entries = new ArrayList<>();

        ByteArrayInputStream bis = new ByteArrayInputStream(rawContent);
        
        while (bis.available() > 0) {
            // Read mode (ASCII string until space)
            StringBuilder modeBuilder = new StringBuilder();
            int b;
            while ((b = bis.read()) != -1 && b != ' ') {
                modeBuilder.append((char) b);
            }
            String mode = modeBuilder.toString();
            if (mode.isEmpty()) {
                throw new IOException("Malformed tree object: Empty mode found for tree SHA-1 " + treeSha1);
            }

            // Read name (UTF-8 string until null byte)
            StringBuilder nameBuilder = new StringBuilder();
            while ((b = bis.read()) != -1 && b != 0x00) { // 0x00 is the null byte delimiter
                nameBuilder.append((char) b);
            }
            String name = nameBuilder.toString();
            if (name.isEmpty()) {
                throw new IOException("Malformed tree object: Empty name found for tree SHA-1 " + treeSha1);
            }

            // Read 20-byte SHA-1 hash
            byte[] sha1Bytes = new byte[20];
            int bytesRead = bis.read(sha1Bytes, 0, 20);
            if (bytesRead != 20) {
                throw new IOException("Malformed tree object: Incomplete SHA-1 hash for entry in tree SHA-1 " + treeSha1);
            }
            String objectSha1Id = bytesToHex(sha1Bytes);

            // Determine type from mode
            String type;
            if (mode.startsWith("100")) { // e.g., 100644, 100755
                type = "blob";
            } else if (mode.startsWith("040")) { // e.g., 040000
                type = "tree";
            } else {
                System.err.println("Warning: Unknown tree entry mode detected: " + mode + " for entry " + name + " in tree " + treeSha1);
                type = "unknown"; // Default type
            }

            entries.add(new TreeEntry(mode, type, objectSha1Id, name));
        }

        // The TreeObject constructor will sort entries and calculate its own SHA-1.
        TreeObject tree = new TreeObject(entries);

        // Verify loaded SHA-1 matches calculated SHA-1
        if (!tree.getSha1Id().equals(treeSha1)) {
            System.err.println("Warning: Loaded tree SHA-1 " + treeSha1 + " does not match calculated SHA-1 " + tree.getSha1Id() + " for content.");
        }

        return tree;
    }
    //  helper for converting bytes to hex 
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

    public static byte[] loadBlob(String blobSha1) throws IOException, IllegalArgumentException {
        // Validate SHA-1 format (readObject also validates, but just to be safe)
        if (blobSha1 == null || blobSha1.length() != 40 || !blobSha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid blob SHA-1 format: " + blobSha1);
        }

        // Blobs are just raw content, so we can simply read the object file directly
        return readObject(blobSha1);
    }
}