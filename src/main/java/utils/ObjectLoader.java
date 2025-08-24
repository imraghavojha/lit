package utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import objects.CommitObject;
import objects.TreeEntry;
import objects.TreeObject;

public class ObjectLoader {

    public static byte[] readObject(String sha1) throws IOException {
        if (sha1 == null || sha1.length() != 40 || !sha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid SHA-1 hash format: " + sha1);
        }

        String dirName = sha1.substring(0, 2);
        String fileName = sha1.substring(2);
        Path objectFilePath = Paths.get(".lit", "objects", dirName, fileName);

        if (!Files.exists(objectFilePath)) {
            throw new IOException("Object not found: " + sha1 + " at " + objectFilePath.toAbsolutePath());
        }

        return Files.readAllBytes(objectFilePath);
    }

   public static CommitObject loadCommit(String commitSha1) throws IOException, IllegalArgumentException {
        // Validate SHA-1 format
        if (commitSha1 == null || commitSha1.length() != 40 || !commitSha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid commit SHA-1 format: " + commitSha1);
        }

        byte[] rawContent = readObject(commitSha1);
        String contentString = new String(rawContent);

        String treeSha1 = null;
        List<String> parentSha1s = new ArrayList<>();
        String authorName = null;
        String authorEmail = null;
        long timestamp = 0;
        String commitMessage = "";

        String[] lines = contentString.split("\n");
        int messageStartIndex = -1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("tree ")) {
                treeSha1 = line.substring("tree ".length());
            } else if (line.startsWith("parent ")) {
                parentSha1s.add(line.substring("parent ".length()));
            } else if (line.startsWith("author ")) {
                String authorLine = line.substring("author ".length());
                int emailStart = authorLine.indexOf("<");
                int emailEnd = authorLine.indexOf(">");
                int timestampStart = authorLine.indexOf(">", emailEnd) + 2;
                int timestampEnd = authorLine.indexOf(" ", timestampStart);

                if (emailStart != -1 && emailEnd != -1 && timestampStart > 1 && timestampEnd > timestampStart) {
                    authorName = authorLine.substring(0, emailStart).trim();
                    authorEmail = authorLine.substring(emailStart + 1, emailEnd);
                    timestamp = Long.parseLong(authorLine.substring(timestampStart, timestampEnd));
                }
            } else if (line.trim().isEmpty() && messageStartIndex == -1) {
                messageStartIndex = i + 1;
            }
        }

        if (messageStartIndex != -1) {
            StringBuilder msgBuilder = new StringBuilder();
            for (int i = messageStartIndex; i < lines.length; i++) {
                msgBuilder.append(lines[i]).append(i < lines.length - 1 ? "\n" : "");
            }
            commitMessage = msgBuilder.toString();
        }

        if (treeSha1 == null || authorName == null || authorEmail == null) {
            throw new IOException("Malformed commit object: Missing essential fields for SHA-1 " + commitSha1);
        }

        CommitObject commit = new CommitObject(treeSha1, parentSha1s, authorName, authorEmail, commitMessage);
        commit.setAuthorTimestamp(timestamp);

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
            StringBuilder modeBuilder = new StringBuilder();
            int b;
            while ((b = bis.read()) != -1 && b != ' ') {
                modeBuilder.append((char) b);
            }
            String mode = modeBuilder.toString();
            if (mode.isEmpty()) {
                throw new IOException("Malformed tree object: Empty mode found for tree SHA-1 " + treeSha1);
            }

            StringBuilder nameBuilder = new StringBuilder();
            while ((b = bis.read()) != -1 && b != 0x00) {
                nameBuilder.append((char) b);
            }
            String name = nameBuilder.toString();
            if (name.isEmpty()) {
                throw new IOException("Malformed tree object: Empty name found for tree SHA-1 " + treeSha1);
            }

            byte[] sha1Bytes = new byte[20];
            int bytesRead = bis.read(sha1Bytes, 0, 20);
            if (bytesRead != 20) {
                throw new IOException("Malformed tree object: Incomplete SHA-1 hash for entry in tree SHA-1 " + treeSha1);
            }
            String objectSha1Id = bytesToHex(sha1Bytes);

            String type = mode.startsWith("100") ? "blob" : "tree";
            entries.add(new TreeEntry(mode, type, objectSha1Id, name));
        }

        TreeObject tree = new TreeObject(entries);

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
        if (blobSha1 == null || blobSha1.length() != 40 || !blobSha1.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalArgumentException("Invalid blob SHA-1 format: " + blobSha1);
        }
        return readObject(blobSha1);
    }
}