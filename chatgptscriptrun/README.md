# Running the codeveloper engine with the chatgpt command line tool

As an alternative to running the CoDeveloper GPT Engine from ChatGPT you can run it with the chatgpt
script from my [ChatGPT Toolsuite](https://github.com/stoerr/chatGPTtools).
This directory contains a tools definition file for `chatgpt` -
[codeveloperengine-chatgptscript-toolsdefinition.json](codeveloperengine-chatgptscript-toolsdefinition.json) .

## Examples

chatgpt -tf codeveloperengine-chatgptscript-toolsdefinition.json "run the tool list files for the directory '.'"

chatgpt -tf codeveloperengine-chatgptscript-toolsdefinition.json "write 'hallo' to file foo.bar"
