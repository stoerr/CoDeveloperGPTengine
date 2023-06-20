# Operation edit file

The operation /editFile (EditFileAction) can be used to edit parts of a file, unlike writeFile that overwrites
the whole file. Since ChatGPT is not able to reliably generate line numbers or patch files we use distinctive lines
as anchor, instead. It has the following query parameters:

- path: the path of the file
- startLineRegex: optional, a regex matching the first line to replace in the file. It is an error if there are
  several lines matching that regex in the file.
- startLineOffset: optional, an offset from the startLine
- stopLineRegex: optional, a regex matchig the last line to replace in the file. We just take the first line
  matching that regex after the line matching the startLineRegex.
- stopLineOffset: optional, an offset from the stopLine
- content: in the request body (just like in the writeFileAction)

## OpenAI description of path /editFile  

Description of /editFile in a code block:

```
  /editFile:
    post:
      operationId: editFile
      summary: Edit parts of a file.
      parameters:
        - name: path
          in: query
          description: The path of the file to edit.
          required: true
          schema:
            type: string
        - name: startLineRegex
          in: query
          description: A regex matching the first line to replace in the file.
          required: false
          schema:
            type: string
        - name: startLineOffset
          in: query
          description: An offset from the startLine.
          required: false
          schema:
            type: integer
        - name: stopLineRegex
          in: query
          description: A regex matching the last line to replace in the file.
          required: false
          schema:
            type: string
        - name: stopLineOffset
          in: query
          description: An offset from the stopLine.
          required: false
          schema:
            type: integer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                content:
                  type: string
      responses:
        '204':
          description: File edited
        '400':
          description: Invalid parameter
```
