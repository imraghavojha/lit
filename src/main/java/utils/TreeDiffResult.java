package utils;

import java.util.ArrayList;
import java.util.List;
import objects.TreeEntry;

public class TreeDiffResult {

    private final List<TreeEntry> addedFiles;
    private final List<TreeEntry> deletedFiles;
    private final List<TreeEntry> modifiedFiles;

    public TreeDiffResult() {
        this.addedFiles = new ArrayList<>();
        this.deletedFiles = new ArrayList<>();
        this.modifiedFiles = new ArrayList<>();
    }

    // Getters for the lists
    public List<TreeEntry> getAddedFiles() {
        return addedFiles;
    }

    public List<TreeEntry> getDeletedFiles() {
        return deletedFiles;
    }

    public List<TreeEntry> getModifiedFiles() {
        return modifiedFiles;
    }

    // Methods to add entries to the appropriate list
    public void addAddedFile(TreeEntry entry) {
        this.addedFiles.add(entry);
    }

    public void addDeletedFile(TreeEntry entry) {
        this.deletedFiles.add(entry);
    }

    public void addModifiedFile(TreeEntry entry) {
        this.modifiedFiles.add(entry);
    }

    // A helper method to check if there are any changes
    public boolean hasChanges() {
        return !addedFiles.isEmpty() || !deletedFiles.isEmpty() || !modifiedFiles.isEmpty();
    }

    // A nice toString() for easy debugging
    @Override
    public String toString() {
        return "TreeDiffResult{\n" +
               "  added=" + addedFiles.size() + ",\n" +
               "  deleted=" + deletedFiles.size() + ",\n" +
               "  modified=" + modifiedFiles.size() + "\n" +
               '}';
    }
}