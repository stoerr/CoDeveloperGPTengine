# CoDeveloperGPTengine

<img src="src/site/resources/images/dalle/joinedkeyboard1.png" alt="Joined Keyboard Image" style="width: 15em;
height: auto;" align="right" />

Would you like to have ChatGPT list, search, read your local files, discuss them with you, modify them
and have it execute (e.g. build and test)
actions locally to support you in your development processes? Then this might be for you. The CoDeveloperGPTengine
provide the actions for a OpenAI [GPT](https://openai.com/blog/introducing-gpts)
for read or even write access to the files in the local directory it is started in.
It can also work as a [ChatGPT plugin](https://openai.com/blog/chatgpt-plugins).

In contrast to other approaches like [AutoGPT](https://github.com/Significant-Gravitas/AutoGPT) this is not meant to
autonomously execute extensive changes (which would likely require a lot of prompt engineering), but to enable the
developer to employ the AI flexibly within a ChatGPT chat session for various tasks both to analyze code and to make
changes or execute configured actions like builds and tests. A ChatGPT chat does, however, permit to trigger several
actions in one message, so it's also possible to e.g. tell it to run the build, fix errors and repeat until it succeeds.

The project is stable and useable. In fact I use it regularily in my own development - both on itself and on
my other projects. Using it does, however, require that you have a paid
[ChatGPT](https://chat.openai.com/) account to use plugins / GPTs.

For more information see the [documentation site](https://CoDeveloperGPTengine.stoerr.net/).

<a href="https://www.youtube.com/watch?v=ubBhv2PUSEs">
    <img src="https://img.youtube.com/vi/ubBhv2PUSEs/hqdefault.jpg" alt="Quick Demo on Youtube">
</a>
