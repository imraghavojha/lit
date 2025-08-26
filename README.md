# Lit: A Lightweight Local Version Control System

Lit is a minimalist version control system implemented in Java, inspired by Git. Designed for local use, Lit enables developers to track code changes, manage versions, and merge branches without requiring a remote server. It serves as an educational tool to explore the core mechanics of version control systems.

## Features

- **Local Version Control**: Track changes, create branches, and manage commits locally.
- **Git-Inspired Commands**: Familiar commands like `init`, `add`, `commit`, `branch`, `merge`, and more.
- **Simple Setup**: Built with Java and Gradle for easy compilation and execution.
- **Educational Focus**: Demonstrates fundamental version control concepts in a lightweight package.

## Prerequisites

To use Lit, ensure you have the following installed:
- **Java Development Kit (JDK)**: Version 11 or higher.
- **Gradle**: Used for building the project (included as a Gradle wrapper).

## Getting Started

### Building the Project

1. Clone or download the Lit repository to your local machine.
2. Navigate to the project’s root directory in your terminal.
3. Build the project using the Gradle wrapper:
   ```bash
   ./gradlew build
   ```
   This compiles the source code and generates a `.jar` file in the `build/libs` directory.

### Running the Command-Line Interface (CLI)

Lit’s CLI is currently executed from the `sample` directory using Gradle’s `run` task with the `--args` flag.

Example commands:
```bash
# Initialize a new Lit repository
./gradlew run --args="init"

# Commit changes with a message
./gradlew run --args="commit -m 'Initial commit'"
```

For a streamlined experience:
1. Run `./gradlew distZip` to generate a distribution in the `build/distributions` directory.
2. Extract the distribution and add the `bin` directory to your system’s `PATH`.
3. Run Lit commands directly, e.g., `lit init`.

## Core Commands

| Command                     | Description                                                                 |
|-----------------------------|-----------------------------------------------------------------------------|
| `lit init`                  | Initializes a new Lit repository in the current directory.                  |
| `lit add <file>`            | Stages a file’s contents to the index.                                      |
| `lit commit -m "<message>"` | Commits staged changes with a message.                                      |
| `lit status`                | Displays the status of the working directory, index, and untracked files.   |
| `lit log`                   | Shows the commit history.                                                  |
| `lit branch <branch-name>`  | Creates a new branch.                                                      |
| `lit switch <branch-name>`  | Switches to the specified branch, updating the working directory.           |
| `lit merge <branch-name> -m "<message>"` | Merges changes from the specified branch into the current branch. |
| `lit rm <file>`             | Removes a file from the working tree and index.                             |
| `lit diff [commit1] [commit2]` | Shows differences between commits, working directory, or index.          |

## Project Goals

Lit is a functional clone of Git’s core features, built to illustrate the principles of version control systems. It’s ideal for learning how version control system like Git works under the hood or for lightweight local version control in small projects. 

You can check [Documentation](documentation.md) for further details on different Lit commands.

## Contributing

Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a feature branch (`lit branch feature-name`).
3. Submit a pull request with your changes.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.