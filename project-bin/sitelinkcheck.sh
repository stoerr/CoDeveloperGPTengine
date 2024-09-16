#!/usr/bin/env bash
echo Perform a link check for the site

mvn -N clean site

#brew upgrade linklint # MacOS!
#mkdir -p target/linkcheck/.
#mkdir -p .linklint
#linklint -cache .linklint -root target/site/. -host codevelopergptengine.stoerr.net -net -doc target/linkcheck/. -out target/linkcheck/linklint.log /#/#

# needs to be installed with pip install linkchecker
# linkchecker linklint -http -host https://codevelopergptengine.stoerr.net/ /#/# -doc target/linkcheck/.

# for MacOS
brew upgrade lychee
lychee --cache -f detailed target/site/*.html target/site/*/*.html
