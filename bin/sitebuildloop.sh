#!/usr/bin/env bash
# Periodically Checks whether there is any file in src/site and it's subdirectories that is newer than target/site/index.html
# or whether there is no target/site/index.html at all.
# If so, it runs mvn -N clean site

while true;
do
    if [ ! -f target/site/index.html ]; then
        mvn -N clean site
    fi

    if [ `find src/site -newer target/site/index.html | wc -l` -gt 0 ]; then
        mvn -N clean site
    fi

    sleep 10
done
