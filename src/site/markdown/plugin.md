# Running the engine as a plugin

You might want to prefer the [GPT approach](gpt.md) to run the engine since
[ChatGPT Plugins](https://openai.com/blog/chatgpt-plugins) seem to be deprecated by OpenAI -
it seems that they [don't want new plugins](https://openai.com/waitlist/plugins) in favor of
[GPTs](https://openai.com/blog/introducing-gpts), and since 11/2023 they seem to have
[broken local plugin development for good](https://community.openai.com/t/what-happened-to-the-plugins/475969).
But I've happily used it as a plugin for quite a while, so here you are.

## Register the engine as an unverified plugin

For this you have to make the engine [available via https](https.md), open the plugin store in ChatGPT and
click "Install unverified plugin". Then you have to enter the domain name at which the engine is accessible from the
internet.

Once you've added the plugin, you can test it in the ChatGPT interface. Try asking the AI to
list the files in the directory, read a specific file, write to a file, or execute a specific action after
setting up some actions in a .cgptdevbench directory in the directory you're running it. If everything
is set up correctly, the AI should be able to perform these operations using the plugin.

## Run it locally as a plugin developer

That used to be the easiest way to run the engine as a plugin, but when I tried it since 11/2023 it didn't work
anymore and I'm not sure whether they will fix it since you cannot eveen register yourself as a plugin developer
anymore. But for the record: if you open the plugin store there is a link "Develop your own plugin". There you
have to enter e.g. localhost:3002 if you run the engine with this port, and it should work.
(Currently I can register the plugin that way, but when trying to use it ChatGPT says "Plugin not found".)
