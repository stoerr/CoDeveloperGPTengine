# Integrating external actions

The CoDeveloper Engine has a nice feature that lets you easily integrate additional actions that can be triggered by
ChatGPT by putting shell scripts into the directory `.cgptcodeveloper/` in your project.
If you, e.g., ask ChatGPT to *Please execute listActions* then it'll trigger a request that has the engine look for a
script called listActions.sh there, execute it and deliver the output to ChatGPT. I follow the convention
to always put the script
[`listActions.sh`](https://github.com/stoerr/CoDeveloperGPTengine/tree/develop/.cgptcodeveloper/listActions.sh)
there, which looks for other `*.sh` in this directory and prints their names and any comments starting with
`Plugin Action:`. This way ChatGPT knows what actions it can execute, and what functions they have. When calling
them, it can transmit arguments and / or standard input to the script. Remember to thoroughly inform
the GPT about what's expected as arguments or input in the `Plugin Action:` comment!

If you're working with Apache Maven as a build system, like I usually do, then this would be a nice script `build.sh`,
which executes the build and writes the log to a file - and tells the GPT whether the build failed or was successful.

```
# Plugin Action: maven build incl. running unit- and integrationtests
echo executing mvn clean install
mvn -B clean install > build.log 2>&1
if [ $? -ne 0 ]; then
  echo "mvn clean install failed, see build.log. You can use the grep action with some context lines to look for errors and exceptions in file build.log"
  exit 1
else
  echo "successful; build log is in build.log"
  exit 0
fi
```

With this you can give it instructions to run the build and analyze or even fix any problems for you. For more examples
please have a look at
[examples/actions](https://github.com/stoerr/CoDeveloperGPTengine/tree/develop/examples/actions)
or the `.cgptcodeveloper/` directory in the engine sources.
