# Developers ChatGPT ToolBench

## Introduction

Would you like to have ChatGPT list, search, read and modify your local files and have it execute (e.g. build and test)
actions locally to support you in your development processes? Then this might be for you. The Developers ChatGPT
ToolBench can either work as a [ChatGPT plugin](https://openai.com/blog/chatgpt-plugins) or provide the actions for a
OpenAI
[GPT](https://openai.com/blog/introducing-gpts) for read or even write access to the files in the local directory it is
started in.

In contrast to other approaches like [AutoGPT](https://github.com/Significant-Gravitas/AutoGPT) this is not meant to
autonomously execute extensive changes (which would likely require a lot of prompt engineering), but to enable the
developer to flexibly use the AI within a ChatGPT chat session for various tasks both to analyze code and to make
some changes.

The project is quite stable and useable. In fact I use it regularily in my own development - both on itself and on
my other projects. Using it does, however, require that you have a paid ChatGPT account that can use plugins / GPTs.
There are three ways to use it within ChatGPT: run it as a localhost plugin if you are registered
as a [plugin developer](https://openai.com/waitlist/plugins), register it as an unverified plugin or put it into a GPT.
See the respective documentation for details.

## Functionality

The `Developers ChatGPT ToolBench` provides ChatGPT with read and optionally write access to the directory in which it
is started. It provides ChatGPT with several operations that it can use on the files in that directory, including:

- Listing the files in a directory
- Reading the contents of a file
- Writing content to a file / changing file content by search and replace
- searching for regular expressions in files
- Execute actions you can define yourself, possibly with additional input from ChatGPT
- fetch the text content of an URL in the internet

More details about the functionality are [here](functionality.md).

## Preconditions

To use the `Developers ChatGPT ToolBench`, you need a [ChatGPT Plus](https://openai.com/blog/chatgpt-plus) account,
since only then OpenAI will let you use plugins / GPTs.
The program is an executable Java JAR, so you need a Java runtime environment (JRE) of at least version 16 to run it.
Also, you will have to make it reachable from OpenAI's servers via https -
there are [several ways to do this](https.md).

## Getting started

- First, you have to [install the program](install.md) locally
- Second, you have to make it [reachable from OpenAI's servers](https.md).
- Third, [run it](commandline.md) in a directory you want to access.
- Fourth, you have to [create a GPT with it as action](gpt.md), or [register it as a ChatGPT plugin](plugin.md). 

Then you can use it from ChatGPT.