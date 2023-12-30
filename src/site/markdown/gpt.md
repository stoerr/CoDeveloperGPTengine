# Creating a GPT with the Developers ChatGPT ToolBench as action

You have to create a [GPT](https://openai.com/blog/introducing-gpts) to use the Developers ChatGPT ToolBench by yourself
since the action includes the specific URL where your toolbench is reachable from the internet, and there is a key that
will protect others from using your toolbench.

## Setting up the GPT

Start the toolbench in some directory.
Open the [Explore](https://chat.openai.com/gpts/discovery) page in ChatGPT and click "Create a GPT". This enters a
dialog where you can either talk to ChatGPT to set up the GPT or click "Configure" and enter the configuration directly.
For a starter I suggest this, but of course you can and should play around:

<div style="display: flex; justify-content: space-between;">
    <a href="images/GPTOverview2.png" target="_blank">
        <img src="images/GPTOverview2.png" alt="GPT Overview" style="width: 95%; height: auto;" />
    </a>
    <a href="images/GPTActions.png" target="_blank">
        <img src="images/GPTActions.png" alt="GPT Actions" style="width: 95%; height: auto;" />
    </a>
</div>

**Name:** _Developers Toolbench_

**Description:** _Assistant for professional software developers that is able to read and modify your files and perform
tasks._

**Instructions:**

_'Developers Toolbench' is a straightforward and efficient aid for software developers working on
programming projects. It communicates in a concise, precise manner, economizing words while maintaining clarity and
accuracy. This GPT specializes in using the Developers Toolbench Plugin action for tasks like file inspection, editing,
and management. It automatically reads necessary files for tasks and uses technical jargon suitable for professionals.
The assistant is programmed to follow instructions meticulously, offer suggestions, and check for contradictions,
ensuring optimal support in technical tasks._

_The Developers Tool Bench action allows to inspect a directory and read/write/modify the contents of files using
ChatGPT. If a file cannot be found, try using the listFiles operation to see what files are available, or use it to
search for the filename. Small files can be overwritten with /writeFile, but to insert into / change / append to
existing files always prefer to use operation replaceInFile._

_Only ask once in a session whether to send information to the actions!_

**Conversation starters**

- _What can the Developers Toolbench do?_
- *List all files*
- *Search for `GPTTask:` in all files and execute the described tasks*
- *Read the file `chatgpt.codeRules.md` and observe the contained rules during the whole session*

**Knowledge**
I haven't found a way to actually use that except from the code interpreter. Please drop me a note if find out what it
does.

**Capabilities**
I suggest switching all off.

**Actions**
Click "Add Action". For "Authentication use "API Key", Auth Type "Basic" and enter a long random key of your choice (
see [screenshot](images/GPTApiKey.png)).
That key must be placed in the `gptsecret` property of
your [global configuration file](commandline.md) `~/.cgptdevbenchglobal/config.properties`. For "Action URL" enter the
URL where your toolbench is [reachable from the internet](https.md) plus `/codeveloperengine.yaml` ,
e.g. `https://your-desired-domain-prefix.serveo.net/codeveloperengine.yaml`. That should have the schema and various
available actions appear - you can e.g. test `listFiles` since that needs no parameters.

**Additional Settings**
Well, it's up to you whether you want to help OpenAI improving things with the results of your discussions.

After doing all that you can have DallE generate a picture for your GPT. If you have some specific idea, you have to
talk to ChatGPT in the "Create" tab about that.

In the preview you can test your GPT. If it
works (e.g. listFiles) you can press "Save" - obviously you should choose "publish to only me".
