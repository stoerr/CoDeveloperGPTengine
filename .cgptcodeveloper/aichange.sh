#!/usr/bin/env bash
# Plugin Action: change a file by giving a description what to change. The file name is the first argument, the description is read from stdin.

filename=$1
if [[ -z "$filename" ]]; then
  echo "Usage: $0 <filename>"
  echo "The description of the change is read from stdin."
  exit 1
fi
if [[ ! -f "$filename" ]]; then
  echo "File $filename does not exist"
  exit 1
fi

promptfil=$(mktemp)
trap 'rm -f $promptfil' EXIT
while IFS= read -r line; do
  echo $line >> $promptfil
done

aigenpipeline -upd -o $filename -p $promptfil -f -wo -m gpt-4o-mini
