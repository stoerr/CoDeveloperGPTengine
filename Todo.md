# Todos

These are possible later plans, not urgent.

## Improvements

- Meta-GPT that answers questions about it?
- Is it possible to return images?

- ignore stuff in .gitignore?
- ??? How to be a regular plugin?
- Compare with Kaguaya
- License?
- announce somewhere. tinyurl.com/29e7g8co , https://github.com/taishi-i/awesome-ChatGPT-repositories
- run in docker
- Extend searh and replace message: if search string can be found with other indentation, report about that inc. the
  indentation.

## Refactoring

## Added functionality that makes sense

- grep : fileregex and grepregex and context lines -> output file and line numbers
- changeFile :

Create an action GrepAction that has parameters like fileRegex and grepRegex (like the ListFilesAction) but returns
the lines with matches that were found, instead of listing the files. It should also have a parameter contextLines,
that isn't yet used.

- Make an actual release.

## Perhaps

- instead of search and replace: sed script on file? conflicted diff format? actual java code to make the
  replacement? Annoying: huge amount of backslashes due to double and triple quoting.

- Compare with https://github.com/e2b-dev/awesome-ai-agents references. Put there?

## Next feature, not quite clear how to do.

operation changeFile changes one part of a file (must be called multiple times for multiple changes)
parameters:

- content : the replacement for the part of the file demarked by the other parameters. Optional - if empty the part of
  the file is deleted.
- startLineRegex (optional)
- startLineOffset (optional, default 0, can be negative)
- endLineRegex (optional)
- endLineOffset (optional, default 0, can be negative)

This will change the content of a file between two lines, excluding those lines, with the given content.
The lines are identified by a optional regex and an offset.
If the startLineRegex or endLineRegex is not given, we take the start of the file. as the position.
If the startLineRegex or endLineRegex matches several lines in the file, that is an error.

Important cases:
start of file until given line
given line until end of file
some line until other line
line number x until line number y
change whole file
insertion at specific line or start of the file
append to file
replace last n lines

Please make a list with what parameters each of these cases can be covered, or mark the cases that are not covered.

---
More operations:
search and replace in file or file tree
rename file, delete file

--- more changes:
Add excludeRegex to file list
Add recursive (default = false) to file list. Ouch: then that should list directories, too!
Refactor into an actual project as this becomes unmanageable. Perhaps integration of scripts like
https://www.chatpc.ai/docs/macos/getting-started/

Perhaps: use ctags somehow: https://aider.chat/docs/ctags.html

## Little improvements

Files not found etc. show up in GPT as an error, and that looks like a bug. Define extra return code / declare that
in the OpenAPI spec?

Feedback action.

## Related

https://github.com/smol-ai/developer - works on github creating PRs

## Naming

Aspekte: GPT, Co-Developer, Tools, major point: Local files
portmanteau suggestions related to ChatGPT + Co-Developer

Coding Companion? Co-Developer? Software Sidekick? Coding Sidekick? GPTCoDev? GPTCoDeveloper?

GPTCoDev , GPTCoCoder, GPT CodeSmith

AI LocalDev Tool
Co-Developer Actions
GPT Local DevAssistant
CoDeveloperGPTengine
GPT DevAction Lab
GPT CoDeveloper tools
CoDeveloper GPT tools

https://medium.com/@guandika8/unleashing-chatgpt-plugin-kaguya-the-quantum-leap-in-the-coding-assistant-and-game-changer-weve-2e4fcd08de4d

## Entries wanted for:

https://github.com/taishi-i/awesome-ChatGPT-repositories
https://github.com/e2b-dev/awesome-ai-agents
https://github.com/formulahendry/awesome-gpt
https://github.com/taranjeet/awesome-gpts
https://github.com/targed/awesome-plugins

## Struktur Eintrag:

Short: Let a GPT collaborate with you as a co-developer on your local files and execute actions on your computer.

Name: CoDeveloperGPTengine
Make a GPT work together with you on your local files and run actions on your computer. Let ChatGPT explore your code,
discuss it with you, and assign it flexible tasks: coding, documenting, testing, building and fixing your software,
or even pair-programming with you!

Details:

- A command line tool that equips your personal OpenAI GPT with actions that operate on local files in the directory
  where the tool is launched.
- Capabilities include listing, searching, reading, and modifying textfiles within that directory.
- Executes configured actions like builds and tests on your computer.
- Provides examples of how to configure a GPT to use these actions, or allows you to add your own
  instructions / knowledge.
- Language-independent - works with any programming language ChatGPT knows.
- Interactive development assistance: discussion, coding, documentation, testing, and building support, all powered by
  AI.

Links:
Weblink: https://CoDeveloperGPTengine.stoerr.net/
Githublink: https://github.com/stoerr/CoDeveloperGPTengine
, ? Twitter, Linkedin, other

Interactive Development Assistance: Get coding, documentation, testing, and building support from an AI-powered assist
