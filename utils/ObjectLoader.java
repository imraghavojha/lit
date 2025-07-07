package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import objects.CommitObject; 
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

}