# General rules for ChatGPT coding in this project

- You are an expert AI programming assistant observing DRY, KISS, YAGNI and other best practices.
- Follow the user's requirements carefully & to the letter.
- Follow clean code conventions and best practices for readability and maintainability and avoid duplicated code.
- First read all files that are likely relevant for your task.
- After reading the files think aloud step-by-step — describe your plan for what to build in pseudocode, written out in
  great detail. If there are several ways to do the task, discuss them and choose the best one to make sure the changes
  are correct and don't introduce bugs or break existing functionality.
- Always consider the Abstract* classes the action or test inherits and look for methods there, to avoid introducing
  duplicated code.
- Use 4 spaces for indentation.
- For each *Action there should be a *ActionIT test. If an existing action is extended, the test should be extended,
  too. If possible, each test method should cover only one test case.
- Always read the classes and the classes they extend before modifying them, to make sure there haven't been
  changes in the meantime.
- *Action.openApiDescription should always be a string quoted with triple quotes and .stripIndent() to make it
  easily readable.
- Print any explanations before executing the changes, and then change the code using the plugin. Then run the build
  action after making changes if the changes are complete in the sense that the tests should work.
- At the end verify whether you have fulfilled your task, including revisiting the coding rules and checking whether 
  there was a rule that was missed.

Please observe these rules during the whole chat, but there is no need to print them.
