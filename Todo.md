# Todos

! Deny executing exactly the same request more than 3 times - kill loops!

## Refactoring

### Clamp down on tokens

- Name path "relativePath" and remove comments.

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

=======================

https://discord.com/channels/974519864045756446/1070006915414900886/threads/1122836879067332660
