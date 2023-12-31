# Quickstart: the minimal and easiest setup

Here is the fastest way to get you started - if you want to understand things, have more control / other options please
see the detailed installation / configuration documentation. We use Serveo for HTTPS tunneling here and run it in a GPT
and assume you are running on a Unix-like system like MacOS, Linux or possibly Windows with Cygwin (not tested).
You need a paid ChatGPT account.

1. Create a directory where the executable should be put and download the jar from the
   [latest release](https://github.com/stoerr/CoDeveloperGPTengine/releases). Rename or symlink the jar
   to `co-developer-gpt-engine.jar`.
2. Create a script `codeveloperengine` in the same directory with the content from
   [here](https://github.com/stoerr/CoDeveloperGPTengine/blob/develop/bin/codeveloperengine) and make it
   executable. Symlink it into a directory in your `PATH`. Alternatively, you can put both files into a directory in
   your `PATH`.
3. Create a directory `$HOME/.cgptcodeveloperglobal/` and a file config.properties in there with a content

```
gptsecret=somewildpassword
```

where you replace somewildpassword with a long password of your choice. It'll be used to authenticate the GPT requests.

4. Create an executable file `$HOME/.cgptcodeveloperglobal/tunnel.sh` with the content

```bash
#!/bin/bash
exec ssh -T -R yourenginedomain.serveo.net:80:localhost:3002 serveo.net
```

where you replace yourenginedomain with a prefix of your choice. It'll be used to access the engine from ChatGPT.

5. Start `codeveloperengine` in a directory you want to access with ChatGPT. It will start a webserver on
   port 3002 and a tunnel to it via serveo.net. You can check whether it works by calling https://yourenginedomain.serveo.net/
   in a browser.
6. Create a GPT according to the [documentation](gpt.md).

Now you can use the engine from ChatGPT and enjoy exploring the capabilities of your assistant. :-)
