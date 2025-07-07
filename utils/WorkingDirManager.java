package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Handles all interactions with the user's working directory.
 * This class is responsible for creating, deleting, and modifying files and folders
 * to match the state of a checked-out commit.
 */
public class WorkingDirManager {

    /**
     * Writes the content of a blob object to a specific file path in the working directory.
     * It will create any necessary parent directories if they do not already exist.
     *
     * @param blobSha1 The SHA-1 hash of the blob object to read.
     * @param targetFilePath The destination path in the working directory where the file should be written.
     * @throws IOException If there's an error reading the blob or writing the file.
     */
    public static void writeBlobToWorkingDir(String blobSha1, Path targetFilePath) throws IOException {
        // Assume ObjectLoader.loadBlob returns the raw byte content of the blob.
        byte[] blobContent = ObjectLoader.loadBlob(blobSha1);

        // Ensure the parent directory for the target file exists.
        // For example, if targetFilePath is "src/com/app/Main.java", this creates "src/com/app".
        Path parentDir = targetFilePath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        // Write the blob's content to the target file.
        // If the file already exists, it will be overwritten.
        Files.write(targetFilePath, blobContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Deletes a single file from the working directory.
     * If the file does not exist, this method does nothing.
     *
     * @param filePath The path to the file to be deleted.
     */
    public static void deleteFile(Path filePath) throws IOException {
        Files.deleteIfExists(filePath);
    }

    /**
     * Recursively deletes a directory and all of its contents.
     * If the directory does not exist, this method does nothing.
     *
     * @param dirPath The path to the directory to be deleted.
     */
    public static void deleteDirectory(Path dirPath) throws IOException {
        if (!Files.exists(dirPath)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(dirPath)) {
            walk.sorted(Comparator.reverseOrder()) // Reverse order to delete contents before the directory itself
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Encapsulate checked exception in an unchecked one to use in lambda
                        throw new RuntimeException("Failed to delete path: " + path, e);
                    }
                });
        } catch (RuntimeException e) {
            // Unwrap and re-throw the original IOException
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    /**
     * Deletes a directory and its contents if it exists, and then recreates it as an empty directory.
     * This is useful for ensuring a clean state before checking out a new tree.
     *
     * @param dirPath The path of the directory to recreate.
     * @throws IOException If an I/O error occurs during deletion or creation.
     */
    public static void recreateDirectory(Path dirPath) throws IOException {
        // First, ensure the directory and its contents are gone.
        deleteDirectory(dirPath);
        // Then, create a new, empty directory at that path.
        Files.createDirectories(dirPath);
    }
}


// ===================================================================================
//  PLACEHOLDER - DO NOT KEEP
//  This is a temporary class to make the code above compile.
//  DELETE THIS CLASS and use actual ObjectLoader class once it's ready.
// ===================================================================================
class ObjectLoader {
    /**
     * (Placeholder) Simulates loading a blob's content from the .lit/objects directory.
     * @param blobSha1 The SHA-1 hash of the blob.
     * @return The content of the blob as a byte array.
     */
    public static byte[] loadBlob(String blobSha1) {
        // In the real implementation, this will find a file named `blobSha1` in the
        // `.lit/objects` directory, decompress it, and return its raw byte content.
        System.out.println("--- (Placeholder) Loading blob: " + blobSha1 + " ---");
        String fakeContent = "This is the content for blob " + blobSha1;
        return fakeContent.getBytes();
    }
}
