# Lit: A Git Clone in Java

`Lit` is a learning project that aims to recreate the core functionality of the popular version control system, Git, from the ground up using Java. This project explores the fundamental data structures and concepts that power Git.

## Current Prototype Features

The current prototype successfully implements the in-memory representation of Git's primary data objects. It can take a sample directory structure and model it according to Git's internal logic.

### What Works:

- **Blob Objects**: The system can take any file and generate a `BlobObject`. This object represents the file's content and calculates its unique SHA-1 hash.
- **Tree Objects**: The system can recursively traverse a directory and create a `TreeObject`. This object represents the directory's state, containing a sorted list of entries for blobs (files) and other trees (subdirectories), and calculates a unique SHA-1 hash for that directory snapshot.
- **Commit Objects**: The system can create a `CommitObject` that ties a root `TreeObject` to metadata, including an author, a commit message, and a parent commit. This effectively creates a snapshot of the project's state at a specific point in time.

The `Main.java` entry point currently serves as a test runner that demonstrates the creation of these objects based on a sample project directory.

## What We're Working On

Our vision is to evolve this prototype into a command-line tool that can manage a real repository on the file system.

Based on our project plan, we are now working towards implementing the following key features in phases:

### Phase 1: Establish the Repository

We are working on creating a persistent repository on the file system. This involves:

- **Creating the `.lit` directory structure:** Implementing a `lit init` command that creates the necessary directories (`.lit/objects`, `.lit/refs/heads`) and the `HEAD` file to store all repository data.
- **Object Serialization:** Modifying our object classes to save themselves to the `.lit/objects` directory, using their SHA-1 hash as the filename.

### Phase 2: Implement the Core Workflow

We are building the command-line interface (CLI) and the core user workflow. This includes:

- **The Staging Area (Index):** Designing and implementing a `.lit/index` file to serve as the staging area, allowing users to craft their commits.
- **`lit add <file>` command:** Creating the logic to hash a file into a blob, save it, and add its information to the index.
- **`lit commit` command:** Orchestrating the entire commit process: creating a tree from the index, finding the parent commit, creating a new commit object, and updating the current branch to point to the new commit.

### Phase 3: Enable Branching

We are focused on implementing Git's powerful branching capabilities. This involves:

- **`lit branch <branch-name>` command:** Adding the ability to create new branches, which are essentially pointers to specific commits.
- **`lit switch <branch-name>` command:** Allowing users to switch between branches by updating the `HEAD` file and, eventually, updating the working directory to reflect the state of the switched branch.

test push from codespaces