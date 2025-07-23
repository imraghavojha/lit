package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import objects.BlobObject;
import objects.IndexEntry;
import objects.TreeEntry;
import objects.TreeObject;

public class TreeBuilder {

    /**
     * Recursively builds a TreeObject representing a directory and its contents (files and subdirectories).
     *
     * @param directoryPath The Path to the directory to be processed.
     * @return A TreeObject representing the directory's content, or null if an error occurs.
     * @throws IOException If an I/O error occurs during directory traversal or file reading.
     */
    public static TreeObject buildTree(Path directoryPath) throws IOException {
        if (!Files.isDirectory(directoryPath)) {
            throw new IllegalArgumentException("Path must be a directory: " + directoryPath);
        }

        List<TreeEntry> entries = new ArrayList<>();

        // Use try-with-resources for the Stream to ensure it's closed
        try (Stream<Path> pathStream = Files.list(directoryPath)) {
            pathStream.forEach(path -> {
                try {
                    String name = path.getFileName().toString(); // Get just the file/directory name
                    String objectSha1Id;
                    String mode;
                    String type;

                    if (Files.isRegularFile(path)) {
                        // It's a file (blob)
                        BlobObject blob = new BlobObject(path.toString());
                        objectSha1Id = blob.getSha1();
                        mode = "100644"; // Standard file mode (read/write for owner, read for others)
                        type = "blob";
                    } else if (Files.isDirectory(path)) {
                        // It's a subdirectory (tree) - RECURSIVE CALL
                        // We need the SHA-1 of the TreeObject representing this subdirectory
                        TreeObject subTree = buildTree(path); // Recursively call buildTree for the subdirectory
                        objectSha1Id = subTree.getSha1Id();
                        mode = "040000"; // Directory mode
                        type = "tree";
                    } else {
                        // Handle other types of files (e.g., symlinks, though Git handles them specifically)
                        // For this prototype, we'll skip them.
                        System.out.println("Skipping non-regular file or directory: " + path);
                        return; // Continue to the next path in the stream
                    }

                    // Only add entry if SHA-1 was successfully generated
                    if (objectSha1Id != null) {
                        entries.add(new TreeEntry(mode, type, objectSha1Id, name));
                    } else {
                        System.err.println("Could not generate SHA-1 for: " + path);
                    }

                } catch (IOException e) {
                    System.err.println("I/O error processing path " + path + ": " + e.getMessage());
                    // Re-throw as unchecked to be caught by the outer try-catch, or handle gracefully
                    throw new RuntimeException("Error during tree building", e);
                } catch (Exception e) {
                    System.err.println("An unexpected error occurred processing path " + path + ": " + e.getMessage());
                    throw new RuntimeException("Unexpected error during tree building", e);
                }
            });
        } catch (RuntimeException e) {
            // Unpack the RuntimeException to re-throw the original IOException or other exceptions
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e; // Re-throw other RuntimeExceptions
        }

        // Create and return the TreeObject for the current directory
        return new TreeObject(entries);
    }

    /**
     * Builds a hierarchical TreeObject structure from a flat list of index entries.
     * This is the new method required for the commit command.
     */
    public TreeObject buildTreeFromIndex(List<IndexEntry> entries) throws IOException {
        Map<String, Object> root = new HashMap<>();

        for (IndexEntry entry : entries) {
            // skip deleted entries when building the tree
            if (entry.isDeleted()) {
                continue;
            }
            
            Path path = Paths.get(entry.getFilePath());
            Map<String, Object> currentNode = root;

            for (int i = 0; i < path.getNameCount() - 1; i++) {
                String part = path.getName(i).toString();
                currentNode = (Map<String, Object>) currentNode.computeIfAbsent(part, k -> new HashMap<String, Object>());
            }
            
            currentNode.put(path.getFileName().toString(), entry);
        }
        
        return createTreeFromMap(root);
    }

    /**
     * Private helper method to recursively convert a map structure into a TreeObject.
     */
    private TreeObject createTreeFromMap(Map<String, Object> nodeMap) throws IOException {
        List<TreeEntry> treeEntries = new ArrayList<>();

        for (Map.Entry<String, Object> mapEntry : nodeMap.entrySet()) {
            String name = mapEntry.getKey();
            Object value = mapEntry.getValue();

            if (value instanceof IndexEntry) {
                IndexEntry entry = (IndexEntry) value;
                // Your TreeEntry constructor has (mode, type, sha, name).
                // We assume type is "blob" for any file in the index.
                treeEntries.add(new TreeEntry(entry.getMode(), "blob", entry.getSha1(), name));
            } else if (value instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) value;
                TreeObject subTree = createTreeFromMap(subMap);
                
                // A sub-tree must be saved to disk to calculate its SHA-1 before being added.
                subTree.save(); 
                
                // Your TreeEntry constructor has (mode, type, sha, name).
                treeEntries.add(new TreeEntry("040000", "tree", subTree.getSha1Id(), name));
            }
        }
        // Creates the TreeObject, which automatically sorts and calculates its own SHA-1.
        return new TreeObject(treeEntries);
    }
}