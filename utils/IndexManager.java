package utils;

import objects.IndexEntry; 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; 

public class IndexManager {
    private List<IndexEntry> indexEntries;
    private Path indexPath;

    public IndexManager() throws IOException {
        // Resolve the .lit/index path relative to the current working directory
        Path currentDirectory = Paths.get("").toAbsolutePath();
        Path litPath = currentDirectory.resolve(".lit");
        this.indexPath = litPath.resolve("index");
        this.indexEntries = new ArrayList<>(); // Initialize with an empty list

        // Attempt to read existing index entries
        readIndex();
    }

    private void readIndex() throws IOException {
        if (Files.exists(indexPath)) {
            // Read all lines from the index file
            List<String> lines = Files.readAllLines(indexPath);

            // Parse each line into an IndexEntry and add to the list
            for (String line : lines) {
                try {
                    IndexEntry entry = IndexEntry.fromString(line);
                    indexEntries.add(entry);
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Skipping malformed index entry: " + line + " - " + e.getMessage());
                    // Continue processing other lines even if one is malformed
                }
            }
        }
        // If the file doesn't exist, indexEntries remains an empty ArrayList.
    }

    public void addEntry(IndexEntry newEntry) {
        // Iterate through existing entries to find if this file path already exists
        boolean found = false;
        for (int i = 0; i < indexEntries.size(); i++) {
            if (indexEntries.get(i).getFilePath().equals(newEntry.getFilePath())) {
                // If found, remove the old entry and add the new one
                indexEntries.set(i, newEntry); // Replace existing entry
                found = true;
                break;
            }
        }
        if (!found) {
            // If not found, add the new entry to the list
            indexEntries.add(newEntry);
        }
    }

    public void writeIndex() throws IOException {
        // Convert the list of IndexEntry objects to a list of strings
        // using the toString() method implemented in IndexEntry.
        List<String> linesToWrite = indexEntries.stream()
                                              .map(IndexEntry::toString)
                                              .collect(Collectors.toList());

        // Write all lines to the index file, overwriting existing content.
        // Files.write creates the file if it doesn't exist.
        Files.write(indexPath, linesToWrite);
    }

    // getter to inspect the current entries for debugging or other commands
    public List<IndexEntry> getIndexEntries() {
        return new ArrayList<>(indexEntries); // Return a copy to prevent external modification
    }
}