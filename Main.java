import objects.BlobObject;

public class Main{
    public static void main(String[] args) {

        BlobObject blob1 = new BlobObject("sample/sampledir2/blobSample1.txt");

        String sample_sha1 = blob1.getSha1();

        System.out.println("SHA-1: "+ sample_sha1);
    }
}