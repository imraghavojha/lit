package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import objects.CommitObject;
//import objects.TreeObject;   
//import objects.TreeEntry;  

public class ObjectLoader {

    public static byte[] readObject(String sha1) throws IOException {
        if (sha1 == null || sha1.length() != 40 || !sha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid SHA-1 hash format: " + sha1);
        }

        // Construct the path to the object file: .lit/objects/firstTwo/restOfSha1
        String dirName = sha1.substring(0, 2);
        String fileName = sha1.substring(2);

        Path objectFilePath = Paths.get(".lit", "objects", dirName, fileName);

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
}