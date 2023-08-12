# Developers ChatGPT ToolBench

This is a [ChatGPT](https://chat.openai.com) plugin for developers that allows reading / searching / writing files
in the local directory it is started in, and to execute configured actions, e.g. a build and test run.

This is currently in development, but is already useable. (In fact I use it on itself while continuing to develop it.)

## Purpose

The `Developers ChatGPT ToolBench` is a Java application designed to provide a plugin for ChatGPT that allows the AI to
access, read, and write files in the directory where the plugin is started. The plugin is implemented as an
executable jar. If you check out and compile this Git repository, you can also use the script
[bin/developersToolBenchPlugin](bin/developersToolBenchPlugin) after building it with
[bin/developersToolBenchPlugin-buildStable](bin/developersToolBenchPlugin-buildStable).

The plugin provides several operations, including:

- Listing the files in a directory
- Reading the contents of a file
- Writing content to a file / changing file content
- searching for regular expressions in files
- Executing a shell script with given content as standard input
- fetch the text content of an URL

Caution: you currently need to be a paying user of ChatGPT (to have access to ChatGPT-4) and to be registered for the
ChatGPT Plugin beta, probably as a plugin developer.

## Status

I'm regularily use it for my own development, and it seems quite stable. I'm using it quite often when I'm asking
ChatGPT questions or small extensions.

## Usage

To use the `Developers ChatGPT ToolBench`, you need to have registered as a plugin developer with ChatGPT.
Once you've done that, you can add the `Developers ChatGPT ToolBench` using the
"Develop your own plugin" option in the ChatGPT web interface with URL "localhost:3002". (You could also specify
another port on the command line when starting it, if you like.)

To start the plugin, navigate to the directory you want to access and run the `developersToolBenchPlugin` class.
The plugin will start a server on port 3002 (by default) and will be ready to accept requests from ChatGPT. If you
want to give a port use option -p <portnumber> ; if you want to write files add option -w .

The plugin is written so that it cannot be used to go outside of the directory ("../somefile" won't work) and also
files starting with a dot or containg /target/ are invisible and not writeable. That prevents directories like .git
to be touched and maven target folders tend to contain very much stuff.
(Compare regex IGNORE_FILE in the script).

Use the writing features at your own risk. There will likely be problems with large files. A possible approach is
have ChatGPT write things, but frequently make a git commit to easily inspect changes and be able to revert.

If there is a file named .cgptdevbench/.requestlog.txt the requests are logged into that, to see what ChatGPT did.

## Examples

Here are some examples of how to use the `Developers ChatGPT ToolBench`:

- **List Files**: To list the files in the current directory, you can use the `listFiles` operation. In ChatGPT, you
  would ask the AI to list the files in the directory, and it would send a request to the plugin to perform this
  operation. It currently lists all files recursively, so don't use a too large directory.

- **Read File**: To read the contents of a file, you can use the `readFile` operation. In ChatGPT, you would ask the AI
  to read a specific file, and it would send a request to the plugin to perform this operation.

- **Write File**: To write content to a file, you can use the `writeFile` operation. In ChatGPT, you would ask the AI to
  write a specific content to a file, and it would send a request to the plugin to perform this operation.

- **Search Files**: to search for Strings in files, you can use the 'grepFiles' operation. In ChatGPT, you could ask
  to search for files with a file name pattern and containing a string or pattern.

- **Execute Action**: To execute a shell script with given content as standard input, you can use the `executeAction`
  operation. In ChatGPT, you would ask the AI to execute a specific action, and it would send a request to the plugin to
  perform this operation. The shell script should be located at `.cgptfmgr/{actionName}.sh`, where `{actionName}` is a
  parameter provided in the query string. The content is passed as standard input to the shell script. Some examples
  are in [.cgptdevbench/](.cgptdevbench/) and
  [src/test/resources/testdir/.cgptdevbench](src/test/resources/testdir/.cgptdevbench) .

- **Fetch the text content of an URL**: gives ChatGPT simple web access: this can perform a GET request and returns 
  the text content of the URL (not the HTML) to ChatGPT.

Remember, the `Developers ChatGPT ToolBench` operates on the directory where it was started,
so be careful to start it in a directory that contains the files you want to access.

## Configuring FileManagerPlugin for use in ChatGPT

To use the `Developers ChatGPT ToolBench` with ChatGPT, you need to register it as a plugin in the ChatGPT interface.
Here's a step-by-step guide on how to do this:

1. **Register as a Plugin Developer**: If you haven't already, register as a plugin developer with ChatGPT. This will
   give you access to the plugin developer interface where you can add your own plugins.

2. **Start the Plugin**: Navigate to the directory you want to access and run the `Developers ChatGPT ToolBench`
   program. This will start a server on port 3002 (by default).

3. **Add the Plugin**: In the ChatGPT interface, navigate to the plugin developer section and select "Develop your own
   plugin", and enter the url `localhost:3002`

4. **Test the Plugin**: Once you've added the plugin, you can test it in the ChatGPT interface. Try asking the AI to
   list the files in the directory, read a specific file, write to a file, or execute a specific action after
   setting up some actions in a .cgptdevbench directory in the directory you're running it. If everything
   is set up correctly, the AI should be able to perform these operations using the plugin.
