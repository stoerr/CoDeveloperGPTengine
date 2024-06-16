#!/usr/bin/env bash
# Plugin Action: change a file by giving a description what to change. The first line of the input has to be a filename that has to exist, and the rest is the description of what to change.
# read first line from stdin, check whether this is an existing file and complain if it isn't.

read -t 20 -r filename
if [[ ! -f "$filename" ]]; then
  echo "File $filename does not exist"
  exit 1
fi

promptfil=$(mktemp)
trap 'rm -f $promptfil' EXIT
while IFS= read -r line; do
  echo $line >> $promptfil
done

set -v
aigenpipeline -upd -v -o $filename -p $promptfil -f -wo -m gpt-3.5-turbo
