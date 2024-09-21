#!/usr/bin/env bash
# Plugin Action: execute a SQL query given as stdin on the database. If a numeric argument is given, that's the maximum number of lines - if not given we use 50 as default.

tmpfile=$(mktemp)
trap "rm -f $tmpfile" EXIT
USER=CHANGEME
PASSWORD=CHANGEME
DATABASE=CHANGEME

# filter out lines matching "Using a password on the command line" from mysql stderr with grep while writing stdout to $tmpfile
mysql -h gf-db -P 3306 -u $USER -p$PASSWORD -D $DATABASE 2>&1 | fgrep -v "Using a password on the command line" > $tmpfile
if [ $? -ne 0 ]; then
    echo "Error connecting to the database"
    exit 1
fi

maxlinecount=50
if [ $# -eq 1 ]; then
    maxlinecount=$1
fi

# if $tmpfile has more than $maxlinecount lines, we'll truncate it
if [ $(wc -l < $tmpfile) -gt $maxlinecount ]; then
    echo "CAUTION: Output too long, truncating to $maxlinecount lines"
    echo
    head -n $maxlinecount $tmpfile
    echo
    echo "... (truncated) ..."
else
    cat $tmpfile
fi
