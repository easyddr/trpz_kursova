package editor;

import com.example.JavaSyntaxParser;
import com.example.SyntaxHighlightService;
import com.example.TokenStyleFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSyntaxParserTest {

    private static final String ESC = "\u001B\\[[0-9;]*m";

    private final TokenStyleFactory factory = new TokenStyleFactory();
    private final JavaSyntaxParser parser = new JavaSyntaxParser(factory);

    private static String plain(String highlighted) {
        return highlighted.replaceAll(ESC, "");
    }

    @Test
    void highlightingNeverChangesTheVisibleText() {
        String code = "int x = 42; // note\nString s = \"hi\"; /* c */ return null;";

        assertEquals(code, plain(parser.parse(code)));
    }

    @Test
    void keywordsNumbersStringsAndCommentsAreStyled() {
        String result = parser.parse("int x = 42; // note\nString s = \"hi\";");

        assertTrue(result.contains(factory.getStyle(TokenStyleFactory.TokenType.KEYWORD).wrap("int")));
        assertTrue(result.contains(factory.getStyle(TokenStyleFactory.TokenType.NUMBER).wrap("42")));
        assertTrue(result.contains(factory.getStyle(TokenStyleFactory.TokenType.COMMENT).wrap("// note")));
        assertTrue(result.contains(factory.getStyle(TokenStyleFactory.TokenType.STRING).wrap("\"hi\"")));
    }

    @Test
    void identifiersThatMerelyContainKeywordsAreNotStyled() {
        String code = "interface1 printer classy";

        assertEquals(code, parser.parse(code));
    }

    @Test
    void keywordInsideStringOrCommentIsNotStyledSeparately() {
        String result = parser.parse("\"class if\" /* while */");

        assertEquals(factory.getStyle(TokenStyleFactory.TokenType.STRING).wrap("\"class if\"")
                + " " + factory.getStyle(TokenStyleFactory.TokenType.COMMENT).wrap("/* while */"), result);
    }

    @Test
    void escapedQuoteDoesNotEndTheString() {
        String result = parser.parse("\"a\\\"b\" int");

        assertTrue(result.contains(factory.getStyle(TokenStyleFactory.TokenType.STRING).wrap("\"a\\\"b\"")));
        assertTrue(result.contains(factory.getStyle(TokenStyleFactory.TokenType.KEYWORD).wrap("int")));
    }

    @Test
    void unterminatedBlockCommentRunsToTheEnd() {
        String result = parser.parse("int a; /* open");

        assertTrue(result.endsWith(factory.getStyle(TokenStyleFactory.TokenType.COMMENT).wrap("/* open")));
    }

    @Test
    void flyweightFactorySharesStyleInstances() {
        parser.parse("int a; int b; int c;");

        assertEquals(1, factory.size());
        assertSame(factory.getStyle(TokenStyleFactory.TokenType.KEYWORD),
                factory.getStyle("#CC7832", true, false));
    }

    @Test
    void serviceDelegatesToTheParser() {
        SyntaxHighlightService service = new SyntaxHighlightService(parser);

        assertEquals(parser.parse("int a;"), service.highlight("int a;"));
    }
}
