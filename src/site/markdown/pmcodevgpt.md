# Chat on the command line using the codeveloper engine with the chatgpt command line tool

As an alternative to running the CoDeveloper GPT Engine from ChatGPT you can run it with the chatgpt
script from my [ChatGPT Toolsuite](https://github.com/stoerr/chatGPTtools). This not a full fledged chat interface,
(hence the script name `pmcodevgpt`= "Poor Mans CoDEVeloper GPT"),
but starts up within a second and can be used for quick tasks. You'll need an
[OpenAI API key](https://platform.openai.com/api-keys) to use it, though -
either in an environment variable `OPENAI_API_KEY` or in a file `~/.openai-api-key.txt` .
The `pmcodevgpt` script starts the CoDeveloper GPT Engine in the background and the
[chatgpt](https://github.com/stoerr/chatGPTtools/blob/develop/bin/chatgpt) script with a tools definition
that is generated from the OpenAPI description of the CoDeveloper GPT Engine.

If you call pmcodevgpt, you can type your prompt and end it with `/end` on a line of its own, or press Ctrl-D to end
each message. After processing and printing the response this starts again - abort the program with Ctrl-C.

If you like you could also use the audio chat feature of the chatgpt script to talk to the CoDeveloper GPT Engine.
You can call `pmcodevgpt -ca` to start the audio chat - follow the instructions it prints to the console. You can
dictate your prompts, but the output will be written to the console.

BTW: if you know any open source models / interfaces that support a function calling / tools interface like
OpenAI does, please let me know! I'd like to try that / integrate that, too.
