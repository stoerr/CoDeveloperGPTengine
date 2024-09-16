#!/usr/bin/env bash
echo search using large language model for "$*"
# update database using bin/llmupdatedb.sh

progfile=$0
if test -L "$progfile"; then
  progfile=$(readlink "$progfile")
fi
progdir=$(dirname "$progfile")/..
cd $progdir

# if .cgptcodeveloper/llmsearch.db does not exist or is older than a week
# then update it using bin/llmupdatedb.sh
if [[ ! -f .cgptcodeveloper/llmsearch.db ]] || [[ $(find .cgptcodeveloper/llmsearch.db -mtime +7) ]]; then
  echo starting database update
  bin/llmupdatedb.sh
  echo finished database update
fi

llm similar til -d .cgptcodeveloper/llmsearch.db -n 5 -c "$*"
