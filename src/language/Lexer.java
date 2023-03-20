package language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Lexer {

    public static final Pattern REGEX = Pattern.compile(
            "//.*|==|!=|>=|<=|&&|\\|\\||[\\[\\]{}();!=><+\\-*/%.,]|\\d+(?:\\.\\d*)?|[a-zA-Z_]\\w*|\"[^\"]*\"|\n"
    );
    public static final Pattern WORD_REGEX = Pattern.compile(
            "[a-zA-Z_]\\w*"
    );

    public static final Map<String, Function<Integer, Token>> TOKMAP = new HashMap<>() {{

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
                    if (str.isBlank()) { //newline
                        curLine++;
                        continue;
                    }

                    if (str.startsWith("\"")) //String literal
                        toks.add(new Token(TokenType.STRING_LITERAL, str.substring(1, str.length() - 1), curLine));

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
        public double getNumber() {
            return (Double) value;
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
        COMMA(","),

        IF("if"),
        ELSE("else"),
        WHILE("while"),
        THIS("this"),
        GLOBAL("global"),

        FUN("fun"),

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
            super("Failed to parse given number on line " + line + ": perhaps it is too large?", nfe);
        }

        public LexingException(String invalidString, int line) {
            super("Encountered unrecognized character sequence on line " + line + ": \"" + invalidString + "\".");
        }
    }


}
