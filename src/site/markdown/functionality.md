# Functionality: Operations of the GPT actions / plugin

The program allows ChatGPT to access the files in the directory it is started in -
it cannot be used to go outside of the directory ("../somefile" won't work) and also
files starting with a dot or containing /target/ are invisible and not writeable. That prevents directories like .git
to be touched, and maven target folders are ignored since they tend to contain very much stuff.
(Compare regex IGNORE_FILES_PATTERN constant in
[CoDeveloperEngine.java](https://github.com/stoerr/CoDeveloperGPTengine/blob/develop/src/main/java/net/stoerr/chatgpt/codevengine/CoDeveloperEngine.java)
). We also ignore files that in .gitignore files, though only simple rules are supported.

You can start it [with or without writing ability](commandline.md) - use the writing features at your own risk.
There will likely be problems with large files. An approach that is nicely working for me is to have ChatGPT write
things, but frequently make a git commit to easily inspect changes and be able to revert.

If there is a file named .cgptcodeveloper/.requestlog.txt the requests are logged into that, to see what ChatGPT did.

## Operations of the plugin / GPT

Here are some examples of how to use the `Co-Developer GPT Engine`:

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
  are in [.cgptcodeveloper/](https://github.com/stoerr/CoDeveloperGPTengine/tree/develop/.cgptcodeveloper) and
  [src/test/resources/testdir/.cgptcodeveloper](https://github.com/stoerr/CoDeveloperGPTengine/tree/develop/src/test/resources/testdir/.cgptcodeveloper) .

- **Fetch the text content of an URL**: gives ChatGPT simple web access: this can perform a GET request and returns
  the text content of the URL (not the HTML) to ChatGPT.

Remember, the `Co-Developer GPT Engine` operates on the directory where it was started,
so be careful to start it in a directory that contains the files you want to access.
