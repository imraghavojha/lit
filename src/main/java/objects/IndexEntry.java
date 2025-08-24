package objects;

public class IndexEntry {
    private final String mode;
    private final String sha1;
    private final String filePath; // Relative path to the repository root

    public IndexEntry(String mode, String sha1, String filePath) {
        if (mode == null || mode.isEmpty()) {
            throw new IllegalArgumentException("Mode cannot be null or empty.");
        }
        if (sha1 == null || (!"0".equals(sha1) && sha1.length() != 40)) { //  "0" for deleted files
            throw new IllegalArgumentException("SHA-1 must be a 40-character hexadecimal string or '0' for deleted files.");
        }
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty.");
        }

        this.mode = mode;
        this.sha1 = sha1;
        this.filePath = filePath;
    }

    public String getMode() {
        return mode;
    }

    public String getSha1() {
        return sha1;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isDeleted() {
        return "0".equals(sha1);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", mode, sha1, filePath);
    }

    public static IndexEntry fromString(String line) {
        String[] parts = line.split(" ", 3); // Split into 3 parts: mode, sha1, filePath
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid index entry format: " + line);
        }
        return new IndexEntry(parts[0], parts[1], parts[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntry that = (IndexEntry) o;
        // Two IndexEntries are considered equal if their file paths are the same.
        // This is crucial for updating entries based on file path.
        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return filePath.hashCode();
    }
}