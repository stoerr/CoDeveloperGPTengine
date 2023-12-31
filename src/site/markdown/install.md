# Local installation of the Co-Developer GPT engine

You can either grab a release from Github or build the tool locally. In addition to that you will have to register it
with ChatGPT - either [as a ChatGPT plugin](plugin.md) or [as a GPT](gpt.md).
As a precondition it has to be reachable with https from the internet (or, more specifically, ChatGPT's servers) - 
see [the corresponding documentation](https.md).

## Download a release

You will find the latest release on it's
[release page on Github](https://github.com/stoerr/CoDeveloperGPTengine/releases) -
the release description contains a link to the executable jar file.
For starting it could the script
[bin/developersToolbenchPlugin](https://github.com/stoerr/CoDeveloperGPTengine/blob/develop/bin/developersToolbenchPlugin)
as an example how to run that downloaded jar.
It needs to be run in the directory you want to access, so you might want to put that into your `PATH`.

## Run it from the source

As an alternative to downloading it, you can check out
[the source](https://github.com/stoerr/CoDeveloperGPTengine)
and build the program with `bin/developersToolbenchPlugin-buildStable` in that directory
and run it with `bin/developersToolbenchPlugin` from the directory you want to access with ChatGPT.
