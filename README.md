# Lit: A Git Clone in Java

`Lit` is a learning project that aims to recreate the core functionality of the popular version control system, Git, from the ground up using Java. This project explores the fundamental data structures and concepts that power Git.

### What's Currently Working:

The prototype has evolved into a functional command-line tool that successfully manages a repository's lifecycle on the file system.

-   **Repository Initialization (`lit init`):** The system can create a `.lit` directory structure, including the `objects` database, `refs/heads` for branches, and the `HEAD` file to manage the current state.

-   **Core Git Objects:**
    -   **Blob Objects:** The system generates a `BlobObject` from a file's content, calculates its SHA-1 hash, and saves it to the object database.
    -   **Tree Objects:** The system recursively creates `TreeObject`s from the staging area (the index), representing a complete directory snapshot.
    -   **Commit Objects:** The system creates `CommitObject`s that link a root `TreeObject` to metadata, including an author, a commit message, and a parent commit, effectively saving a snapshot of the project.

-   **Core Workflow Commands:**
    -   **Staging Area (`lit add`):** A `.lit/index` file acts as a staging area. The `add` command hashes a file, saves it as a blob, and adds it to the index for the next commit.
    -   **Committing (`lit commit`):** The `commit` command orchestrates the entire commit process: building a tree from the index, identifying the parent commit, creating a new commit object, and updating the current branch to point to the new commit.

-   **Branching and Navigation:**
    -   **Branching (`lit branch`):** The system supports creating new branches, which are pointers to specific commits.
    -   **Switching (`lit switch`):** Users can switch between different branches or check out a specific commit. This command updates the `HEAD`, reconstructs the working directory to match the target commit's state, and updates the index accordingly.

---

### Next Steps & Future Vision

Our vision is to enhance `Lit` with more advanced features and robust tooling, making it a more powerful and user-friendly version control system.

#### Phase 1: Advanced Commands & Workflow

We are now focused on implementing more complex and helpful Git commands to enrich the user's workflow.

-   **A "Cool" `lit log` command:** We will implement a `log` command to display the history of commits. The goal is to go beyond a simple list and create a well-formatted output showing the commit hash, author, date, and message for each commit in the current branch's history.

-   **Branch Merging (`lit merge`):** A crucial next step is to implement branch merging. This will involve developing the logic to combine the histories of two branches, starting with a "fast-forward" merge and then tackling more complex three-way merges.

-   **The "Safety Net" Undo (`lit undo`):** To make `Lit` more forgiving, we will add a simple, human-friendly command for undoing common actions without needing to understand complex commands like `reset`.
    -   `lit undo --commit`: This would undo the most recent commit but keep the changes from that commit in the staging area (index). This is perfect for when you commit but immediately realize you forgot to add a file.
    -   `lit undo --add`: This would unstage all files from the staging area, effectively undoing all `lit add` commands since the last commit.

#### Phase 2: Developer Experience & Robustness

To ensure the project is stable and easy to contribute to, we will focus on internal improvements and developer-facing features.

-   **Comprehensive Help (`--help`):** We will implement a global `--help` flag and command-specific help (e.g., `lit commit --help`) to provide users with clear, accessible documentation directly from the command line.

-   **Unit Testing (JUnit):** We will create a comprehensive suite of unit tests using a framework like JUnit. This is critical to ensure that existing features remain stable as we add new functionality and will help us catch bugs early.

-   **Build Management (Gradle):** To streamline the development process, we will integrate a build manager like Gradle or Maven. This will automate the process of compiling the code, managing dependencies, running tests, and packaging the application.
