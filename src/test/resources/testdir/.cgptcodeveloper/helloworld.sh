#!/bin/bash
echo "Hello World! Your input was: $(cat)"
# if there are arguments, print them
if [ $# -gt 0 ]; then
  echo "Arguments: $*"
fi
