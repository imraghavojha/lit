package objects;

import java.io.IOException;
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
        try {
            Content.saveObject(getSha1(), content);
        } catch (IOException e) {
            System.err.println("Failed to save blob: " + e.getMessage());
        }
    }
}