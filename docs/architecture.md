# Co-Developer GPT Engine Architecture

## Overview

The Co-Developer GPT Engine is a Java-based web service that enables ChatGPT and OpenAI GPTs to perform local file
system operations. It runs as an embedded Jetty server and exposes REST endpoints that allow AI assistants to read,
write, search, and modify files in the directory where the engine is started.

## Core Architecture Principles

- **Action-based Design**: All operations inherit from `AbstractPluginAction` and are implemented as HTTP servlets
- **Security-First**: Authentication via bearer tokens, configurable access controls, and gitignore-based file filtering
- **RESTful API**: OpenAPI-compliant endpoints for seamless integration with ChatGPT plugins and GPTs
- **Local File Focus**: Operates exclusively within the started directory and respects `.gitignore` patterns
- **External Action Support**: Configurable shell scripts for build, test, and other development workflows

## Project Structure

### Source Code Organization (`src/main/java/net/stoerr/chatgpt/codevengine/`)

#### Core Classes

- **`CoDeveloperEngine.java`** - Main application entry point and Jetty server setup
- **`AbstractPluginAction.java`** - Base class for all file operations, provides common utilities like file filtering,
  gitignore handling, and error management
- **`TbUtils.java`** - Utility functions for logging, file operations, and common tasks

#### Action Implementations

All actions extend `AbstractPluginAction` and implement specific file operations:

- **`ListFilesAction.java`** - Lists files with optional filtering by path/content patterns
- **`ReadFileAction.java`** - Reads file contents with optional line range limiting
- **`WriteFileAction.java`** - Creates new files or overwrites existing ones
- **`ReplaceAction.java`** - Performs targeted text replacement in files
- **`ReplaceRegexAction.java`** - Regex-based text replacement operations
- **`GrepAction.java`** - Searches file contents with pattern matching
- **`ExecuteExternalAction.java`** - Executes predefined shell scripts (build, test, etc.)
- **`UrlAction.java`** - Provides engine status and configuration information

#### Configuration and Security

- **`UserGlobalConfig.java`** - Manages user configuration and authentication
- **`RepeatedRequestChecker.java`** - Prevents duplicate operations
- **`ExecutionAbortedException.java`** - Custom exception for operation failures

### Resources (`src/main/resources/`)

- **`ai-plugin.json`** - ChatGPT plugin manifest
- **`static/`** - Web assets including OpenAPI specifications and debugging tools
- **`logback.xml`** - Logging configuration

### Tests (`src/test/java/net/stoerr/chatgpt/codevengine/`)

#### Test Types

- **Unit Tests** - End with `Test.java` (e.g., `TbUtilsTest.java`, `GitIgnoreRulesTest.java`)
- **Integration Tests** - End with `IT.java`, test actual HTTP endpoints and file operations
    - `AbstractActionIT.java` - Base test class providing common test utilities
    - Individual action tests (e.g., `ListFilesActionIT.java`, `ReplaceActionIT.java`)

### Configuration and Examples

#### Configuration (`examples/config/`)

- Shell scripts for tunneling and external service setup

#### External Actions (`examples/actions/`)

- **`build.sh`** - Maven build execution
- **`execsql.sh`** - Database operation example
- **`aichange.sh`** - AI-assisted change workflow
- **`listActions.sh`** - Available actions discovery

### Build and Deployment

#### Maven Configuration (`pom.xml`)

- Java 8 compatible
- Jetty embedded server
- Shade plugin for fat JAR creation
- GitHub Packages distribution

#### Binary Distribution (`bin/`)

- **`co-developer-gpt-engine.jar`** - Executable JAR
- **`codeveloperengine`** - Shell wrapper script
- **`pmcodevgpt`** - Process management utilities

### Documentation (`src/site/`)

#### User Documentation (`src/site/markdown/`)

- Installation, configuration, and usage guides
- Feature descriptions and examples
- Security and HTTPS setup instructions

#### Project Utilities (`project-bin/`)

- Build automation scripts
- OpenAPI specification generation
- Site building and link checking tools

## Key Design Patterns

### Action Pattern

All file operations follow a consistent pattern:

1. Extend `AbstractPluginAction`
2. Implement `getUrl()` for endpoint mapping
3. Implement `openApiDescription()` for API documentation
4. Override `doGet()`/`doPost()` for operation logic
5. Use inherited utilities for file filtering and error handling

### Security Model

- Bearer token authentication for all protected endpoints
- Gitignore-based file filtering prevents access to sensitive files
- Configurable access controls via local configuration
- HTTPS support for production deployments

### Error Handling

- Consistent error responses via `sendError()` method
- Structured logging with request/response details
- Graceful degradation for missing files or permissions

## Development Guidelines

### For Human Developers

- Actions should extend `AbstractPluginAction` and follow established patterns
- Integration tests should extend `AbstractActionIT` for consistent setup
- Use `TbUtils` for common operations like logging and file handling
- Respect gitignore patterns and security boundaries

### For AI Assistants

- All file operations must go through the defined REST endpoints
- Use `listFiles` to discover available files before attempting operations
- Prefer `replaceInFile` over `writeFile` for modifying existing content
- Check action availability via `/listActions` endpoint
- Respect file size limits and use appropriate endpoints for large files

## Testing Strategy

### Unit Tests

- Test utility functions and core logic
- Mock external dependencies
- Focus on edge cases and error conditions

### Integration Tests

- Test complete HTTP request/response cycles
- Use temporary directories for file operations
- Verify security and filtering behavior
- Test external action execution

### Development Workflow

- Local testing via embedded Jetty server
- Integration with ChatGPT for end-to-end validation
- Continuous integration with Maven build process
