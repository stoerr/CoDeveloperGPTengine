# Developers ChatGPT ToolBench

## Introduction

Would you like to have ChatGPT list, search, read and modify your local files and have it execute (e.g. build and test) 
actions locally to support you in your development processes? Then this might be for you. The Developers ChatGPT 
ToolBench can work as a [ChatGPT plugin](https://openai.com/blog/chatgpt-plugins) or provide the actions for a OpenAI 
[GPT](https://openai.com/blog/introducing-gpts) to access the files in the local directory it is started in.

In contrast to other approaches like [AutoGPT](https://github.com/Significant-Gravitas/AutoGPT) this is not meant to
autonomously execute extensive changes (which would require a lot of prompt engineering), but to enable the
developer to flexibly use the AI within a ChatGPT chat session for various tasks both to analyze code and to make
some changes.

The plugin is quite stable and useable. In fact I use it regularily in my own development - both on itself and on
other projects. Using it does, however, require that you have a paid ChatGPT account that can use plugins / GPTs.
There are three ways to use it within ChatGPT: run it as a localhost plugin if you are registered 
as a [plugin developer](https://openai.com/waitlist/plugins), register it as an unverified plugin or put it into a GPT.

## Purpose

The `Developers ChatGPT ToolBench` is a Java application designed to provide a plugin for ChatGPT that allows the AI to
access, read, and write files in the directory where the plugin is started. The plugin is implemented as an
executable jar. If you check out and compile this Git repository, you can also use the script
[bin/developersToolBenchPlugin](bin/developersToolBenchPlugin) after building it with
[bin/developersToolBenchPlugin-buildStable](bin/developersToolBenchPlugin-buildStable).

The plugin provides several operations, including:

- Listing the files in a directory
- Reading the contents of a file
- Writing content to a file / changing file content by search and replace
- searching for regular expressions in files
- Execute actions you can define yourself, possibly with additional input from ChatGPT
- fetch the text content of an URL

## Usage

To use the `Developers ChatGPT ToolBench`, you need to have registered as a plugin developer with ChatGPT.
Once you've done that, you can add the `Developers ChatGPT ToolBench` using the
"Develop your own plugin" option in the ChatGPT web interface with URL "localhost:3002". (You could also specify
another port on the command line when starting it, if you like.)

To start the plugin, navigate to the directory you want to access and run the `bin/developersToolBenchPlugin` script.
The plugin will start a server on port 3002 (by default) and will be ready to accept requests from ChatGPT. If you
want to give a port use option -p (portnumber) ; if you want to write files add option -w .

The plugin is written so that it cannot be used to go outside of the directory ("../somefile" won't work) and also
files starting with a dot or containg /target/ are invisible and not writeable. That prevents directories like .git
to be touched and maven target folders tend to contain very much stuff.
(Compare regex IGNORE_FILE in the script).

Use the writing features at your own risk. There will likely be problems with large files. A possible approach is
have ChatGPT write things, but frequently make a git commit to easily inspect changes and be able to revert.

If there is a file named .cgptdevbench/.requestlog.txt the requests are logged into that, to see what ChatGPT did.

## Download

You can run it from the source (build the program with bin/developersToolbenchPlugin-buildStable) and run it with
bin/developersToolbenchPlugin, or [download a release](https://github.com/stoerr/DevelopersChatGPTToolBench/releases)
and run the executable jar in whatever directory you want to access. You could use the script
[bin/developersToolbenchPlugin](bin/developersToolbenchPlugin) as an example how to run that downloaded jar.

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
