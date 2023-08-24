package net.stoerr.chatgpt.devtoolbench;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReadFileActionIT extends AbstractActionIT {

    @Test
    public void testReadWithoutParameters() throws Exception {
        String response = checkResponse("/readFile?path=firstfile.txt", "GET", null, 200, null);
        assertEquals("Hello!\nJust a test.\n", response);
    }

    @Test
    public void testReadWithMaxLines() throws Exception {
        String response = checkResponse("/readFile?path=secondfile.md&maxLines=1", "GET", null, 200, null);
        String[] lines = response.split("\n");
        assertEquals(3, lines.length);
    }

    @Test
    public void testReadWithStartLine() throws Exception {
        String response = checkResponse("/readFile?path=secondfile.md&startLine=14", "GET", null, 200, null);
        assertEquals("File secondfile.md lines 14 to line 17\n" +
                "\n" +
                "appearing context line too\n" +
                "\n" +
                "Oh well, let's have a duck again!\n" +
                "end\n", response);
    }

    @Test
    public void testReadWithMaxLinesAndStartLine() throws Exception {
        String response = checkResponse("/readFile?path=firstfile.txt&maxLines=1&startLine=2", "GET", null, 200, null);
        String[] lines = response.split("\n");
        assertEquals(3, lines.length);
        assertEquals("Just a test.", lines[2]);
    }
}
