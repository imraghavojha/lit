package utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Content {

    public static byte[] fileToByte(String filePath) throws IOException{
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    /**
     * saves an object to the .lit/objects directory.
     * The object is saved in a directory named with the first two characters of its SHA-1,
     * and the file is named with the remaining characters.
     */
    public static void saveObject(String sha1, byte[] data) throws IOException {
        if (sha1 == null || sha1.isEmpty() || data == null) {
            throw new IllegalArgumentException("Invalid object data or SHA-1 for saving.");
        }

        Path objectsDir = Paths.get(".lit", "objects");
        String dirName = sha1.substring(0, 2);
        String fileName = sha1.substring(2);
        Path subDir = objectsDir.resolve(dirName);
        Path objectFile = subDir.resolve(fileName);

        if (!Files.exists(subDir)) {
            Files.createDirectories(subDir);
        }

        if (!Files.exists(objectFile)) {
            Files.write(objectFile, data);
            System.out.println("Saved object: " + sha1);
        }
    }
}