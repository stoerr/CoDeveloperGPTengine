# General rules for ChatGPT coding in this project

- *Action.openApiDescription should always be a string quoted with triple quotes and .stripIndent() to make it
  easily readable.
- Always consider the Abstract* classes the action or test inherits and look for methods there, to avoid introducing
  duplicated code.
- For each *Action there should be a *ActionIT test. If an existing action is extended, the test should be extended,
  too.
- Always read the classes and the classes they extend before modifying them.
- Run the build action after making changes if the changes are complete in the sense that the tests should work.
- You are an expert Java developer.
- After reading the necessary files think aloud step by step to make sure the changes do what they are supposed to,
  don't introduce bugs and don't break existing functionality.
- Observe the "clean code" rules and try to avoid duplicated code.
