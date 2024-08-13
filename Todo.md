# Todos

These are possible later plans, not urgent.

!! Site config

## Improvements

- Use from LobeChat https://lobehub.com/docs/usage/start with local models

- Meta-GPT that answers questions about it?

- ignore stuff in .gitignore?
- ??? How to be a regular plugin?
- Compare with Kaguaya
- Extend search and replace message: if search string can be found with other indentation, report about that inc. the
  indentation.

- use chatgpt-4o-mini for modifications
- recheck other modification variants with gpt-4o
- check how good gpt-4o-mini is for the tasks

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

https://medium.com/@guandika8/unleashing-chatgpt-plugin-kaguya-the-quantum-leap-in-the-coding-assistant-and-game-changer-weve-2e4fcd08de4d

## Entries wanted for:

https://github.com/taishi-i/awesome-ChatGPT-repositories
https://github.com/e2b-dev/awesome-ai-agent
(https://github.com/formulahendry/awesome-gpt - no submission)
(https://github.com/taranjeet/awesome-gpts - only actual GPTs)

## Struktur Eintrag:

Short: Let a GPT collaborate with you as a co-developer on your local files and execute actions on your computer.

Alternative: Let a ChatGPT collaborate with you as a co-developer to read and write your local files and execute
actions on your computer.

Very short:
12345678901234567890123456789012345678901234567890
Build OpenAI GPTs that work with your local files

Name: Co-Developer GPT engine

Details:

Make a GPT work together with you on your local files and run actions on your computer. Tell ChatGPT to explore your code, discuss it with you, and assign it flexible tasks: coding, documenting, testing, building and fixing your software, even pair-programming with you or tell it to run the build and fix things until the tests work!

- Equips an OpenAI GPT with the capability to read and write files in a local directory and execute user-defined actions (like builds or test executions) there
- A command line tool provides actions to the GPT including capabilities include listing, searching, reading and modifying textfiles within the directory where the tool is launched, execute user-defined actions, retrieve URLs as markdown
- Language-independent - works with any programming language ChatGPT knows, or text files in general
- Specialize it by creating GPTs with your own instructions and knowledge

Links:
Weblink: https://CoDeveloperGPTengine.stoerr.net/
Githublink: https://github.com/stoerr/CoDeveloperGPTengine
Linkedin: https://www.linkedin.com/in/hans-peter-st%C3%B6rr-5944594/
Twitter: https://twitter.com/HansPeterStoerr
Other: https://www.stoerr.net/blog.html
Other: https://codevelopergptengine.stoerr.net/banner.png

## Postings about it

- https://www.stoerr.net/blog/2024-01-07-co-developer-gpt-engine.html
- https://medium.com/@yu4cheem/the-co-developer-gpt-engine-creating-gpts-that-can-work-on-your-local-files-29ef9f5ac80f
- https://www.youtube.com/watch?v=ubBhv2PUSEs
- https://twitter.com/HansPeterStoerr/status/1744091597149475005

Possible channels: Twitter, LinkedIn, Xing, Blog, ? OpenAI

Canonical link: https://CoDeveloperGPTengine.stoerr.net/

### TODO

If you like to employ the arguably currently most advanced public #AI, #openai #chatgpt , for your
#softwaredevelopment - how about trying my Co-Developer #gpt engine? It allows ChatGPT to look directly at the files in
a local directory, modify them or write new ones, execute user-configured actions there. So it's much easier discussing
your code with ChatGPT. And you can give it small tasks there: answer questions, make documentation, modify code, create
tests, even run the build and fix errors. Just like your personal Co-Developer:

If you like to employ the arguably currently most advanced public AI, ChatGPT, for your software development - how about
trying my Co-Developer GPT engine? It allows ChatGPT to look directly at the files in a local directory, modify them or
write new ones, execute user-configured actions there. So it's much easier discussing your code with ChatGPT. And you
can give it small tasks there: answer questions, make documentation, modify code, create tests, even run the build and
fix errors. Just like your personal Co-Developer: https://CoDeveloperGPTengine.stoerr.net/

## Painpoints of others this could solve

Informationssouche. RAG
