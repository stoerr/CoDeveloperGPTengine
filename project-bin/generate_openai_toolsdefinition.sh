#!/usr/bin/env bash
# generates a json schema for using the engine within other tools
scriptdir=$(dirname $(realpath $0))
cd $(dirname $0)/../
aigenpipeline -wvf -o src/main/resources/static/codeveloperengine-toolsdefinition.json \
    -p $scriptdir/generate_openai_toolsdefinition.prompt src/test/resources/test-expected/codeveloperengine.yaml

aigenpipeline -wvf -o chatgptscriptrun/codeveloperengine-chatgptscript-toolsdefinition.json \
    -p $scriptdir/generate_chatgpt_script_toolsdefinition.prompt src/main/resources/static/codeveloperengine-toolsdefinition.json
