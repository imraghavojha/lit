package objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;

public class TreeObject {
    private String treeSha1Id;
    private List<TreeEntry> entries;

    public TreeObject(List<TreeEntry> entries) {
        //Sort entries lexicographically by name
        entries.sort(Comparator.comparing(TreeEntry::getName));

        this.entries = entries;
        this.treeSha1Id = calculateTreeObjectSha1();
    }

    public String getSha1Id() {
        return treeSha1Id;
    }

    public List<TreeEntry> getEntries() {
        return entries;
    }

    private byte[] serializeContentToBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // Iterate through each sorted TreeEntry
            for (TreeEntry entry : entries) {
                // 1. Append mode (ASCII string)
                outputStream.write(entry.getMode().getBytes(StandardCharsets.US_ASCII));

                // 2. Append a space character
                outputStream.write(' '); // ASCII space byte

                // 3. Append name (UTF-8 string). Git typically uses UTF-8 for filenames.
                outputStream.write(entry.getName().getBytes(StandardCharsets.UTF_8));

                // 4. Append a null byte (important delimiter)
                outputStream.write(0x00); // Null byte

                // 5. Append the raw 20-byte SHA-1 hash (converted from hex string)
                byte[] rawSha1 = hexToBytes(entry.getObjectSha1Id());
                outputStream.write(rawSha1);
            }
        } catch (IOException e) {
            System.err.println("Error writing to ByteArrayOutputStream: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        return outputStream.toByteArray();
    }

    private byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.length() != 40) {
            throw new IllegalArgumentException("SHA-1 hash must be a 40-character hex string.");
        }
        byte[] bytes = new byte[20];
        for (int i = 0; i < 40; i += 2) {
            // Parse two hex characters as a single byte
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                 + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }

    private String calculateTreeObjectSha1() {
        try {
            byte[] contentBytes = serializeContentToBytes();
            if (contentBytes == null) {
                return null;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(contentBytes);

            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            String hexSha1 = formatter.toString();
            formatter.close();
            return hexSha1;

        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-1 algorithm not found.");
            e.printStackTrace();
            return null;
        }
    }
}