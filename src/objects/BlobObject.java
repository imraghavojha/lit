package objects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import utils.Content;

public class BlobObject {
    private String blobSha1;
    private byte[] content;
    private final String path;

    public BlobObject(String filePath) {
        this.path = filePath;
        try {
            this.content = Content.fileToByte(path);
        } catch (IOException e) {
            System.err.println("Error getting content.");
        } catch (Exception e){
            System.err.println("An unexpected error occurred while loading blob content: " + e.getMessage());
        }
    }

    private String calculateSha1() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(content);
            blobSha1 = bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return blobSha1;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String getSha1(){
        return calculateSha1();
    } 

    public void save(){
        String sha1 = getSha1();
        
        if (sha1 == null || sha1.isEmpty()) {
            System.err.println("Blob SHA-1 is null or empty. Cannot save blob.");
            return;
        }
        
        Path objectsDir = Paths.get(".lit", "objects");
        Path blobPath = objectsDir.resolve(sha1);

        try{

            if(!Files.exists(objectsDir)) {
                Files.createDirectories(objectsDir);
            }

            if (!Files.exists(blobPath)) {
                Files.write(blobPath, content);
                System.out.println("Blob saved: " + blobPath.toString());

             } else {
                System.out.println("Blob already exists: " + blobPath.toString());
             }
        } catch (IOException e){
            System.err.println("Failed to save blob: " + e.getMessage());
        }
    }
}