# Related Tools

A list of related tools, to compare.

https://github.com/PromtEngineer/localGPT

# Open source tools from [Awesome AI Agents](https://github.com/e2b-dev/awesome-ai-agents)

## [Smol developer](https://github.com/smol-ai/developer)
:hatching_chick: Your own junior developer. [Deployed in few seconds via e2b](https://app.e2b.dev/agent/smol-developer/?utm_source=awesome-ai-agents)
<details>

### Description
- Human-centric, coherent whole program synthesis
- Your own junior developer
- Allows to develop, debug, and decompile
- 200 LOC, half english
- 100k context can summarize both content and codebases
- Markdown is the best prompting DSL
- Copy and paste your errors as prompts
- Copy and paste curl output as prompts
- Write CSS animation by describing what u want
- GPT4 >>> GPT3.5/Anthropic Claude for codegen

### Links
- Author: [Swyx](https://twitter.com/swyx)
- [Demo](https://www.youtube.com/watch?v=UCo7YeTy-aE)
- [Twitter](https://twitter.com/SmolModels)
- [Meme](https://smol.ai/)


</details>

## [GPT Engineer](https://github.com/AntonOsika/gpt-engineer)
An AI agent that generates an entire codebase based on a prompt

<details>

### Description
- Model: GPT 4
- Specify your project, and the AI agent asks for clarification, and then constructs the entire code base
- Features
	- Made to be easy to adapt, extend, and make your agent learn how you want your code to look. It generates an entire codebase based on a prompt
	- You can specify the "identity" of the AI agent by editing the files in the identity folder
	- Editing the identity and evolving the main prompt is currently how you make the agent remember things between projects
	- Each step in steps.py will have its communication history with GPT4 stored in the logs folder, and can be rerun with scripts/rerun_edited_message_logs.py

<!--

### Features
- Made to be easy to adapt, extend, and make your agent learn how you want your code to look. It generates an entire codebase based on a prompt
- You can specify the "identity" of the AI agent by editing the files in the identity folder
- Editing the identity, and evolving the main prompt, is currently how you make the agent remember things between projects
- Each step in steps.py will have its communication history with GPT4 stored in the logs folder, and can be rerun with scripts/rerun_edited_message_logs.py

-->

### Links
- [Discord](https://discord.com/invite/8tcDQ89Ej2)
- Author: [Anton Osika](https://twitter.com/antonosika)
- [Twitter review by @Attack](https://twitter.com/Attack/status/1671165869064609792)

</details>

## [Cody](https://docs.sourcegraph.com/cody)

An AI code assistant from Sourcegraph that writes code and answers questions for you by reading your entire codebase and the code graph.

<details>

### Links
- [GitHub](https://github.com/sourcegraph/sourcegraph/tree/main/client/cody)
- Author: [@sourcegraph](https://twitter.com/sourcegraph) (Twitter)

</details>


## [Bloop](https://bloop.ai/)
A GPT-4 powered semantic code search engine that uses an AI agent

<details>

### Description
- Powered by GPT-4 and semantic code search, precise code navigation
- Built on stack graphs and scope queries
- Fast code search and regex matching engine written in Rust
- Allows to find Code on Rust and Typescript
- Allows to stage changes
- The agent searches both your local and remote repositories with natural language, regex and filtered queries
- Bloop can be run via app (easy to download via GitHub)

### Links
- [GitHub](https://github.com/BloopAI/bloop)
- ["Getting started" guide](https://bloop.ai/docs/getting-started)
- [Bloop apps](https://github.com/BloopAI/bloop/releases)

</details>

## [WorkGPT](https://github.com/team-openpm/workgpt)
A GPT agent framework for invoking APIs
<details>

### Description
- WorkGPT is an agent framework in a similar fashion to AutoGPT or LangChain. You give it a directive and an array of APIs and it will converse back and forth with the AI until its directive is complete.
- For example, a directive could be to research the web for something, to crawl a website, or to order you an Uber. We support any and all APIs that can be represented with an OpenAPI file.
- WorkGPT now has OpenAI's new function invocation feature baked into it
	- While chaining together APIs was possible before (see AutoGPT), it was slow, expensive, and error prone
	- [The tweet announcing this feature](https://twitter.com/maccaw/status/1669367224694607875)

### Links
- Author: [Alex MacCaw](https://twitter.com/maccaw)

</details>


## [AutoPR](https://github.com/irgolic/AutoPR)
AI-generated pull requests to fix issues, powered by ChatGPT
<details>

### Description
- Triggered by adding a label containing AutoPR to an issue, AutoPR will:
	- Plan a fix
	- Write the code
	- Push a branch
	- Open a pull request

### Links
- [Discord](https://discord.com/invite/ykk7Znt3K6)

</details>
