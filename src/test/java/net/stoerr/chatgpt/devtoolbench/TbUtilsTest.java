package net.stoerr.chatgpt.devtoolbench;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class TbUtilsTest {


    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void testCompileReplacementWithGroupReferences() {
        String input = "Hello, my name is John Doe.";
        String pattern = "(Hello, my name is) (John Doe)";
        String replacement = "$1 Jane Doe";

        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(input);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(sb, TbUtils.compileReplacement(null, replacement));
        }
        matcher.appendTail(sb);

        collector.checkThat(sb.toString(), CoreMatchers.is("Hello, my name is Jane Doe."));
    }

    @Test
    public void testCompileReplacementWithBackslashes() {
        String input = "Hello\\World";
        String pattern = "(Hello\\\\World)";
        String replacement = "$1\\\nUni\tverse$$";

        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(input);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(sb, TbUtils.compileReplacement(null, replacement));
        }
        matcher.appendTail(sb);

        collector.checkThat(sb.toString(), CoreMatchers.is("Hello\\World\\\nUni\tverse$"));
    }
}
