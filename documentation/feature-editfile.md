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
          description: A regex matching the first line to replace in the file. If empty, we take the start of the file.
          required: false
          schema:
            type: string
        - name: startLineOffset
          in: query
          description: An offset (number of lines) from the startLine. If negative and startLineRegex is empty, this 
          counts from the end of the file.
          required: false
          schema:
            type: integer
        - name: stopLineRegex
          in: query
          description: A regex matching the last line to replace in the file. If empty, we take the line the 
          startLineRegex matches.
          required: false
          schema:
            type: string
        - name: stopLineOffset
          in: query
          description: An offset (number of lines) from the stopLine.
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

## Usecases

Among others, there are the following usecases:

1. insert text at the start of a file
2. append text to the end of a file
3. replace a complete file
4. replace text from the start of a file until somewhere in the middle of a file (the beginning of a file)
5. replace text from somewhere in the middle of a file until the end of a file (the end of a file)
6. replace text somewhere in the middle of a file
7. insert text somewhere in the middle of a file
8. delete text somewhere in the middle of a file
9. delete the start of a file
10. delete the end of a file

List which parameter settings are needed for each of those use cases and verify whether there are contradictions or 
unclear points in the specification or whether there are missing features to fulfill some of them, or whether the 
functionality is unambiguous. For brevity, just list parameters that are set for the usecase, omit parameters that are 
not set. We only want to insert / replace / delete exactly one contiguous part of the file.
