# Archive for some information relevant to development.

## Composition of ChatGPT prompt

Actual prompt for ChatGPT looks like this (not necessarily current). This is composed of the following parts:

- description for model. (The description in the OpenAPI yaml is NOT passed on!)
- For an action the summary.
- for a query parameter and body parameters, the description

// Devtoolbench allows to inspect a directory and read/write/modify the contents of files using ChatGPT. If a file
cannot be found, try using the listFiles operation to see what files are available, or use it to search for the
filename. Small files can be overwritten with /writeFile, but to insert into / change existing files please prefer to
use regex replacement with replaceInFile. (By the way, if you receive
an `ApiSyntaxError: Could not parse API call kwargs as JSON: exception=Unterminated string ...` then that usually means
that the request was too long.)
namespace Devtoolbench {

// Execute an action with given content as standard input. Only on explicit user request.
type executeExternalAction = (_: {
actionName: string,
actionInput?: string,
}) => any;

// Search for lines in text files matching the given regex.
type grepAction = (_: {
// relative path to the directory to search in or the file to search. root directory = '.'
path: string,
// optional regex to filter file names
fileRegex?: string,
// regex to filter lines in the files
grepRegex: string,
// number of context lines to include with each match (not yet used)
contextLines?: number,
}) => any;

// Recursively lists files in a directory. Optionally filters by filename and content.
type listFiles = (_: {
// relative path to directory to list. root directory = '.'
path: string,
// regex to filter file names
filenameRegex?: string,
// an optional regex that lists only files with matching content
grepRegex?: string,
}) => any;

// Read a files content.
type readFile = (_: {
// relative path to file
path: string,
}) => any;

// Replaces occurrences of a regular expression in a file. You are a Java regular expression expert and can use all
advanced regex features - the whole file is matched, not line by line.
type replaceInFile = (_: {
// relative path to file
path: string,
// if true, replace all occurrences, otherwise exactly one occurrence - it would be an error if there is no occurrence
or several occurrences
multiple?: boolean,
// java Pattern to be replaced
pattern: string,
// will replace the regex; can contain group references as in Java Matcher.appendReplacement
replacement: string,
}) => any;

// Overwrite a small file with the content given in full.
type writeFile = (_: {
// relative path to file
path: string,
content?: string,
}) => any;

} // namespace Devtoolbench
