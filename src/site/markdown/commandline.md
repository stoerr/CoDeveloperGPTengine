# Command line and configuration

I assume you have [installed](install.md) the program and created a script `developersToolbenchPlugin` in your path
that starts it. With argument -h or --help you get a short help message with the options available.

```
version: : DevToolBench version: 2.0
usage: options are
    -g,--globalconfigdir {arg}   Directory for global configuration (default:
    ~/.cgptdevbenchglobal/
    -h,--help                    Display this help message
    -l,--local                   Only use local configuration via options -
    ignore any global configuration
    -p,--port {arg}              Port number
    -w,--write                   Permit file writes and action executions
```

- `-p`: To specify the port number, include -p followed by a valid port number. If no -p option is given, the default port number 3002 is used.
- `-w`: If you include -w, writing and executing actions will be enabled. If there's no -w option mentioned in the command, writing and executing actions are disabled by default.
- `-h`: This option is to display the help message. It'll also be shown when there's an exception in parsing options.
- `-g`: This option is used to specify the directory path for global configuration. If it's not mentioned, the default directory "~/.cgptdevbenchglobal/" is used.
- `-l`: This option if present, is used to ensure that only command line configuration is used and global configuration is ignored.

## Global configuration file

The global configuration file defines several properties that are important for the communication with ChatGPT.
Normally it's put into the directory `~/.cgptdevbenchglobal/` and is named `config.properties`, for example:

```
gptsecret=examplesecret
openaitoken=exampletoken
```

where:

* `gptsecret` A secret the GPT has to share to access the devtoolbench as an action.
* `openaitoken` The token OpenAI provides after entering the gptSecret when developing a plugin.

If you want to run it without a tunnel but to provide a HTTPS interface by itself (compare [Access via HTTPS](https.md))
there can also be the following properties:

* `httpsPort` The port the program should open for HTTPS (has to be reachable from the internet as port 443)
* `keystorePath` The path to the keystore file.
* `keystorePassword` The password for the keystore.
* `domain` The external domain through which the application is accessible.
* `externport` The external port through which the application is reachable (optional with default 443, something else than 443 probably won't work with ChatGPT)

## Local configuration directory

If you have a directory named .cgptdevbench/ in the directory where you start the program, a log file `.requestlog.txt`
will be put there with the requests and responses to ChatGPT. This is useful for debugging or just to see what's going
on.

Second, you can put there a number of shell scripts *.sh that can be executed from the program. It's a good idea to
also put a file listActions.sh there that lists the available actions:

```bash
# Plugin Action: list all available actions
cd .cgptdevbench || exit 1
for fil in *.sh; do
echo "${fil%.sh} :  " $(egrep -o "Plugin Action: .*" "$fil" | sed -e 's/Plugin Action: //')
done
```

It requires a comment `# Plugin Action: ` to be present in each *.sh file that explains the function of the action.

More examples can be found in the
[.cgptdevbench directory of the project](https://github.com/stoerr/DevelopersChatGPTToolBench/tree/develop/.cgptdevbench) .
