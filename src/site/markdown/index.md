---
description: Let a ChatGPT collaborate with you as a co-developer to read and write your local files and execute actions on your computer.
keywords:
  - AI
  - ChatGPT
  - Software Development
  - Productivity
---

<!-- https://github.blog/2008-12-19-github-ribbons/ -->
<a href="https://github.com/stoerr/CoDeveloperGPTengine" style=" position: absolute; top: 0px; right: 0px; ">
  <img decoding="async" width="149" height="149" src="https://github.blog/wp-content/uploads/2008/12/forkme_right_gray_6d6d6d.png?resize=149%2C149" class="attachment-full size-full" alt="Fork me on GitHub" loading="lazy" data-recalc-dims="1"></img>
</a>

# Co-Developer GPT Engine

<div style="text-align: center; margin-bottom: 2em; margin-top: 2em;">
    <img src="banner.png" itemprop="image" alt="Co-Developer GPT engine banner" style="max-width: 100%;height: auto;max-height: 
25em;"/>
</div>

## Introduction

Would you like to have ChatGPT list, search, read your local files, discuss them with you, and modify them
and have it execute (e.g. build and test)
actions locally to support you in your development processes? Then this might be for you. The Co-Developer GPT
Engine can either work as a [ChatGPT plugin](https://openai.com/blog/chatgpt-plugins) or provide the actions for a
OpenAI
[GPT](https://openai.com/blog/introducing-gpts) for read or even write access to the files in the local directory it is
started in.

In contrast to other approaches like [AutoGPT](https://github.com/Significant-Gravitas/AutoGPT) this is not meant to
autonomously execute extensive changes (which would likely require a lot of prompt engineering), but to enable the
developer to flexibly use the AI within a ChatGPT chat session for various tasks both to analyze code or documentation
and to make some changes. A ChatGPT chat does, however, permit to trigger several actions in one message, so it's
also possible to e.g. tell it to run the build, fix errors and repeat until it succeeds.

The project is quite stable and useable. In fact I use it regularily in my own development - both on itself and on
my other projects. Using it does, however, require that you have a paid ChatGPT account that can use plugins / GPTs.
There are three ways to use it within ChatGPT: put it as actions into a GPT, register it as an unverified plugin or run
it as a localhost plugin if you are registered as a plugin developer.
See the respective documentation for details. It is open source under the Apache 2.0 license - it's source is
available [on GitHub](https://github.com/stoerr/CoDeveloperGPTengine). See
also [my blog](https://www.stoerr.net/blog/codeveloper) for some background.

## Functionality

<div style="float: right; margin-left: 2em; margin-bottom: 2em;">
<iframe src="https://www.youtube.com/embed/ubBhv2PUSEs?si=Vk-QKH6rIq8oktPP" title="YouTube video player" 
frameborder="0" width="560" height="315"
allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
allowfullscreen="allowfullscreen"></iframe>
</div>

The `Co-Developer GPT Engine` provides ChatGPT with read and optionally write access to the directory in which it
is started. It provides ChatGPT with several operations that it can use on the files in that directory, including:

- Listing the files in a directory
- Reading the contents of a file
- Writing content to a file / changing file content by search and replace
- searching for regular expressions in files
- Execute actions you can define yourself, possibly with additional input from ChatGPT
- fetch the text content of an URL in the internet

More details about the functionality are [here](functionality.md).
The [video on the right](https://youtu.be/ubBhv2PUSEs)
contains a quick demo what it can look like to work with the GPT.

## Preconditions

To use the `Co-Developer GPT Engine`, you need a [ChatGPT Plus](https://openai.com/blog/chatgpt-plus) account,
since only then OpenAI will let you use plugins / GPTs.
The program is an executable Java JAR, so you need a Java runtime environment (JRE) of at least version 11 to run it.
Also, you will have to make it reachable from OpenAI's servers via https -
there are [several ways to do this](https.md).

## Getting started

To start up quickly you can follow the steps in the [quickstart instructions](quickstart.md), or you can go into
depth with the corresponding documentations:

- First, you have to [install the program](install.md) locally
- Second, you have to make it [reachable from OpenAI's servers](https.md).
- Third, [run it](commandline.md) in a directory you want to access.
- Fourth, you have to [create a GPT with it as action](gpt.md), or [register it as a ChatGPT plugin](plugin.md).

Then you can use it from ChatGPT and enjoy working with your newly found AI assistant.
