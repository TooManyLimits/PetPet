package petpet.lang.lex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Lexer {

    private static final Pattern REGEX = Pattern.compile(
            "//.*|==|!=|>=|<=|&&|\\|\\||!\\[|\\$\\[|[\\[\\]{}():;!=><+\\-*/%.,]|\\d+(?:\\.\\d+)?|[a-zA-Z_]\\w*|\"(?:\\\\.|[^\\\\\"])*\"|\n|."
    );
    private static final Pattern WORD_REGEX = Pattern.compile(
            "[a-zA-Z_]\\w*"
    );

    private static final Map<String, Function<Integer, Token>> TOKMAP = new HashMap<>() {{

        for (TokenType type: TokenType.values())
            for (String s : type.s)
                put(s, line -> new Token(type, null, line));

        put("true", line -> new Token(TokenType.BOOLEAN_LITERAL, true, line));
        put("false", line -> new Token(TokenType.BOOLEAN_LITERAL, false, line));
    }};

    public static Token[] lex(String source) throws LexingException {
        String[] strings = REGEX.matcher(source).results().map(MatchResult::group).toArray(String[]::new);
        ArrayList<Token> toks = new ArrayList<>();
        int curLine = 1;
        try {
            for (String str : strings) {
                //Get the token type directly from the map if possible
                if (TOKMAP.containsKey(str))
                    toks.add(TOKMAP.get(str).apply(curLine));
                else {
                    //Otherwise, find it ourselves

                    if (str.startsWith("//")) continue; //comment
                    if (str.isBlank()) { //whitespace
                        if (str.contains("\n")) curLine++;
                        continue;
                    }

                    if (str.startsWith("\"")) { //String literal
                        if (str.length() == 1)
                            throw new LexingException("Encountered unmatched quote on line " + curLine);
                        StringBuilder builder = new StringBuilder();
                        for (int i = 1; i < str.length()-1; i++) {
                            char c = str.charAt(i);
                            if (c == '\\') {
                                i++;
                                char next = str.charAt(i);
                                builder.append(switch (next) {
                                    case '\\' -> '\\';
                                    case 'n' -> '\n';
                                    case 't' -> '\t';
                                    case 'r' -> '\r';
                                    case '"' -> '"';
                                    default -> throw new LexingException("Illegal escape character \"\\" + next + "\" on line " + curLine);
                                });
                            } else {
                                builder.append(c);
                            }
                        }
                        toks.add(new Token(TokenType.STRING_LITERAL, builder.toString(), curLine));
                    }

                    else if (Character.isDigit(str.charAt(0))) //Number literal
                        toks.add(new Token(TokenType.NUMBER_LITERAL, Double.parseDouble(str), curLine));

                    else if (WORD_REGEX.matcher(str).matches()) //Name
                        toks.add(new Token(TokenType.NAME, str, curLine));
                    else
                        throw new LexingException(str, curLine);
                }
            }
        } catch (NumberFormatException nfe) {
            throw new LexingException(nfe, curLine);
        }

        return toks.toArray(new Token[0]);
    }

    public record Token(TokenType type, Object value, int line) {
        public String getString() {
            return (String) value;
        }

        @Override
        public String toString() {
            return type.toString() + (value != null ? "(" + value + ")" : "");
        }
    }

    public enum TokenType {
        PLUS("+"),
        MINUS("-"),
        TIMES("*"),
        DIVIDE("/"),
        MODULO("%"),
        DOT("."),
        ASSIGN("="),

        NOT("!"),
        EQUALS("=="),
        NOT_EQUALS("!="),
        GREATER_EQUAL(">="),
        LESS_EQUAL("<="),
        GREATER(">"),
        LESS("<"),

        AND("&&", "and"),
        OR("||", "or"),

        LEFT_CURLY("{"),
        RIGHT_CURLY("}"),
        LEFT_PAREN("("),
        RIGHT_PAREN(")"),
        LEFT_SQUARE("["),
        RIGHT_SQUARE("]"),
        SEMICOLON(";"),
        TABLE_START("$["),
        LIST_START("!["),
        COLON(":"),
        COMMA(","),

        IF("if"),
        ELSE("else"),
        WHILE("while"),
        THIS("this"),
        GLOBAL("global"),

        FUNCTION("fn"),

        NUMBER_LITERAL(), //double
        BOOLEAN_LITERAL(),
        NAME(),
        CALL(),
        STRING_LITERAL(),

        ;public final String[] s;
        TokenType(String... s) {
            this.s = s;
        }

    }

    public static class LexingException extends Exception {
        public LexingException(NumberFormatException nfe, int line) {
            super("Failed to parse given number on line " + line, nfe);
        }

        public LexingException(String invalidString, int line) {
            super("Encountered unrecognized character sequence on line " + line + ": \"" + invalidString + "\".");
        }

        public LexingException(String message) {
            super(message);
        }
    }


}
