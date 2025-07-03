package utils;

import objects.IndexEntry; 
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
//import java.util.stream.Collectors; 

public class IndexManager {
    private List<IndexEntry> indexEntries;
    private Path indexPath;

    /**
     * Initializes the IndexManager, setting up the path to the .lit/index file
     * and reading any existing entries into memory.
     * @throws IOException If an I/O error occurs during index file operations.
     */
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

}