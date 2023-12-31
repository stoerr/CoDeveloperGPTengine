# Creating a GPT with the Co-Developer GPT Engine as action

You have to create a [GPT](https://openai.com/blog/introducing-gpts) to use the Co-Developer GPT Engine by yourself
since the action includes the specific URL where your engine is reachable from the internet, and there is a key that
will protect others from using your engine.

## Setting up the GPT

Start the engine in some directory.
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

**Name:** _Co-Developer_

**Description:** _Assistant for professional software developers that is able to read and modify your files and perform
tasks._

**Instructions:**

_'Co-Developer' is a straightforward and efficient aid for software developers working on
programming projects. It communicates in a concise, precise manner, economizing words while maintaining clarity and
accuracy. This GPT specializes in using the Co-Developer Engine action for tasks like file inspection, editing,
and management. It automatically reads necessary files for tasks and uses technical jargon suitable for professionals.
The assistant is programmed to follow instructions meticulously, offer suggestions, and check for contradictions,
ensuring optimal support in technical tasks._

_The assistant observes good programming practices like clean code, KISS, DRY, YAGNI, SOLID, OOP, TDD, POLA, SoC, use functional programming, keep it stateless if sensible, favors idempotent operations, avoid code smells._

_The Co-Developer GPT Engine actions allow to inspect a directory and read/write/modify the contents of files using
ChatGPT. If a file cannot be found, try using the listFiles operation to see what files are available, or use it to
search for the filename. Small files can be overwritten with /writeFile, but to insert into / change / append to
existing files always prefer to use operation replaceInFile._

_Only ask once in a session whether to send information to the engine!_

**Conversation starters**

These are a few suggestions - pick 4 you like or make up your own. I tend to include a chatgpt.codeRules.md file in the project root directory (for instance [in this project](https://github.com/stoerr/CoDeveloperGPTengine/blob/develop/chatgpt.codeRules.md)) that lays out basic things about the project and the desired programming style - that's what these corresponding starters are about.

- _What can the Co-Developer do?_
- *List all files*
- *Search for `GPTTask:` in all files and execute the described tasks*
- *Read the file `chatgpt.codeRules.md` and observe the contained rules during the whole session*
- Read the chatgpt.codeRules.md and observe the rules that are laid out there during this whole chat, but you don't need to repeat them.
- Read the chatgpt.codeRules.md and observe the rules that are laid out there during this whole chat, but you don't need to repeat them.  Use the grep operation to search for comments starting with `ChatGPTTask:` and execute them. The comment should be replaced by the new code or deleted when it's completely done. Read these classes entirely and also classes they refer to.

**Knowledge**
I haven't found a way to actually use that except from the code interpreter. Please drop me a note if find out what it
does.

**Capabilities**
I suggest switching all off.

**Actions**
Click "Add Action". For "Authentication use "API Key", Auth Type "Basic" and enter a long random key of your choice (
see [screenshot](images/GPTApiKey.png)).
That key must be placed in the `gptsecret` property of
your [global configuration file](commandline.md) `~/.cgptcodeveloperglobal/config.properties`. For "Action URL" enter the
URL where your engine is [reachable from the internet](https.md) plus `/codeveloperengine.yaml` ,
e.g. `https://your-desired-domain-prefix.serveo.net/codeveloperengine.yaml`. That should have the schema and various
available actions appear - you can e.g. test `listFiles` since that needs no parameters.

**Additional Settings**
Well, it's up to you whether you want to help OpenAI improving things with the results of your discussions.

After doing all that you can have DallE generate a picture for your GPT. If you have some specific idea, you have to
talk to ChatGPT in the "Create" tab about that. Or you upload [my proposal](/images/dalle/joinedkeyboard1.png).

In the preview you can test your GPT. If it
works (e.g. listFiles) you can press "Save" - obviously you should choose "publish to only me".
