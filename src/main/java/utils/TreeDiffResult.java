package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import objects.TreeEntry;

public class TreeDiffResult {

    public static class TreeEntryWithPath {
    private final TreeEntry entry;
    private final String fullPath;
    
    public TreeEntryWithPath(TreeEntry entry, String fullPath) {
        this.entry = entry;
        this.fullPath = fullPath;
    }
    
    public TreeEntry getEntry() {
        return entry;
    }
    
    public String getFullPath() {
        return fullPath;
    }
}

    private final List<TreeEntryWithPath> addedFiles;
    private final List<TreeEntryWithPath> deletedFiles;
    private final List<TreeEntryWithPath> modifiedFiles;

    public TreeDiffResult() {
        this.addedFiles = new ArrayList<>();
        this.deletedFiles = new ArrayList<>();
        this.modifiedFiles = new ArrayList<>();
    }

    // Getters for the lists
    public List<TreeEntryWithPath> getAddedFiles() {
        return addedFiles;
    }

    public List<TreeEntryWithPath> getDeletedFiles() {
        return deletedFiles;
    }

    public List<TreeEntryWithPath> getModifiedFiles() {
        return modifiedFiles;
    }

    // Methods to add entries to the appropriate list
    public void addAddedFile(TreeEntry entry, String fullPath) {
        this.addedFiles.add(new TreeEntryWithPath(entry, fullPath));
    }

    public void addDeletedFile(TreeEntry entry, String fullPath) {
        this.deletedFiles.add(new TreeEntryWithPath(entry, fullPath));
    }

    public void addModifiedFile(TreeEntry entry, String fullPath) {
        this.modifiedFiles.add(new TreeEntryWithPath(entry, fullPath));
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

    public Set<String> getAllFilePaths() {
        Set<String> paths = new HashSet<>();
        addedFiles.forEach(e -> paths.add(e.getFullPath()));
        modifiedFiles.forEach(e -> paths.add(e.getFullPath()));
        deletedFiles.forEach(e -> paths.add(e.getFullPath()));
        return paths;
    }

    public Map<String, TreeEntry> getAllFilesAsMap() {
        Map<String, TreeEntry> map = new HashMap<>();
        addedFiles.forEach(e -> map.put(e.getFullPath(), e.getEntry()));
        modifiedFiles.forEach(e -> map.put(e.getFullPath(), e.getEntry()));
        deletedFiles.forEach(e -> map.put(e.getFullPath(), e.getEntry()));
        return map;
    }
}