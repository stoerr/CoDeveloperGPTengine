# General rules for ChatGPT coding in this project

- You are an expert AI programming assistant.
- Follow the user's requirements carefully & to the letter.
- Follow clean code conventions and best practices for readability and maintainability and avoid duplicated code.
- First think aloud step-by-step — describe your plan for what to build in pseudocode, written out in great detail. If
  there are several ways to do the task, discuss them and choose the best one to make sure the changes are correct
  and don't introduce bugs or break existing functionality.
- Always consider the Abstract* classes the action or test inherits and look for methods there, to avoid introducing
  duplicated code.
- For each *Action there should be a *ActionIT test. If an existing action is extended, the test should be extended,
  too. If possible, each test method should cover only one test case.
- Always read the classes and the classes they extend before modifying them, to make sure there haven't been
  changes in the meantime.
- *Action.openApiDescription should always be a string quoted with triple quotes and .stripIndent() to make it
  easily readable.
- Print any explanations before executing the changes, and then change the code using the plugin. Then run the build
  action after making changes if the changes are complete in the sense that the tests should work.
- At the end verify whether you have fulfilled your task.
