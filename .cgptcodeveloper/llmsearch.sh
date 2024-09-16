# Plugin Action: execute a LLM based search for the search string given as input
echo executing a LLM based search
searchstring=$(cat)
echo "search string is $searchstring"
project-bin/llmsearch.sh "$searchstring"
