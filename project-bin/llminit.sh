#!/usr/bin/env bash
# setup llm usable for search https://github.com/simonw/llm
# call once for installation on MacOS
progfile=$0
if test -L "$progfile"; then
  progfile=$(readlink "$progfile")
fi
progdir=$(dirname "$progfile")/..
cd $progdir

brew install llm
brew upgrade llm
llm install -U llm
llm install -U llm-sentence-transformers
llm sentence-transformers register --lazy -a minilm all-MiniLM-L12-v2
llm sentence-transformers register --lazy -a mpnet all-mpnet-base-v2
