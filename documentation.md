# Lit: Comprehensive Documentation

This document provides a detailed overview of the design, features, and internal workings of the Lit version control system.

## 📂 Project Structure

The project follows a modular and clean architecture with clear separation of concerns across multiple packages:
* **`commands`**: Contains the entry point for each command-line action (e.g., `AddCommand`, `CommitCommand`) using the `picocli` framework. These classes handle argument parsing and delegate the core logic to the `CommandHandler`.
* **`objects`**: Defines the fundamental objects of the version control system, including `BlobObject` (file content), `TreeObject` (directory structure), and `CommitObject` (a snapshot of the repository history).
* **`utils`**: Houses utility classes that perform the heavy lifting of the system, such as handling file I/O, managing the index, and performing diffs and merges.

## 🚀 Key Features and Methods

### 1. `init`
* **Command**: `lit init`
* **Description**: Initializes a new repository in the current directory. It creates a `.lit` directory with subdirectories for `objects` and `refs/heads`, along with the `HEAD` and `index` files.

### 2. `add`
* **Command**: `lit add <file>`
* **Description**: Adds a file to the staging area.
* **Internal Logic**: The `CommandHandler.handleAdd()` method takes a file path, creates a `BlobObject` from its contents, and calculates its SHA-1 hash. It then saves the blob to the `.lit/objects` directory and creates or updates an `IndexEntry` in the `.lit/index` file.

### 3. `commit`
* **Command**: `lit commit -m "<message>"`
* **Description**: Records staged changes as a new commit.
* **Internal Logic**:
    * `CommandHandler.handleCommit()` checks for staged changes in the index.
    * It uses `TreeBuilder` to construct a `TreeObject` from the staged `IndexEntry` objects, saving the tree and its children to the object database.
    * A new `CommitObject` is created, linking to the new tree and the parent commit (if one exists).
    * The `ReferenceManager` updates the `HEAD` reference to point to the new commit SHA-1.

### 4. `status`
* **Command**: `lit status`
* **Description**: Displays the state of the working tree, index, and repository.
* **Internal Logic**:
    * `CommandHandler.handleStatus()` compares three versions of the repository: the working directory, the index, and the `HEAD` commit's tree.
    * It identifies and reports files that are:
        * Staged for commit (in the index, but different from `HEAD`)
        * Modified but not staged (in the working directory, but different from the index)
        * Untracked (present in the working directory but not in the index)

### 5. `log`
* **Command**: `lit log`
* **Description**: Shows the commit history.
* **Internal Logic**:
    * `CommandHandler.handleLog()` gets the current `HEAD` commit from `ReferenceManager`.
    * It then enters a loop, loading each `CommitObject` using `ObjectLoader`, printing its details (SHA, author, message), and moving to its first parent. This process continues until a commit with no parent is found.

### 6. `branch` and `switch`
* **`branch <name>`**: Creates a new branch file in `.lit/refs/heads/` that points to the current commit.
* **`switch <name>`**: Changes the `HEAD` reference to point to the new branch or a specific commit SHA. The `CheckoutManager` updates the working directory to match the state of the target commit.

### 7. `merge`
* **Command**: `lit merge <branch-name>`
* **Description**: Combines the history of a specified branch into the current one.
* **Internal Logic**:
    * `MergeUtils.merge()` finds the common ancestor between the `HEAD` commit and the target branch's commit.
    * It then performs a three-way diff between the ancestor, `HEAD`, and the other branch to identify added, modified, or deleted files.
    * For files with conflicting changes, the `ConflictHandler` adds conflict markers to the file in the working directory.
    * If the merge is successful, a new merge commit with two parents is created.

### 8. `rm`
* **Command**: `lit rm <file>`
* **Description**: Removes a file from the index and the working directory.
* **Internal Logic**: `CommandHandler.handleRm()` checks if the file is in the index and, if so, marks it for deletion by setting its SHA-1 to "0" in the index. It then deletes the file from the working directory.

### 9. `diff`
* **Command**: `lit diff [commit1] [commit2]`
* **Description**: Displays differences between versions of files.
* **Internal Logic**:
    * **No arguments**: Compares the index and the working directory.
    * **One argument**: Compares a specific commit with the working directory.
    * **Two arguments**: Compares two specific commits.
    * The `FileDiffer` utility performs the actual line-by-line comparison by analyzing the file contents. The output is similar to standard diff format, with `+` for added lines and `-` for deleted lines.