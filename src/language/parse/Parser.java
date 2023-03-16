package language.parse;

import static language.Lexer.*;
import static language.Lexer.TokenType.*;


import java.util.ArrayList;
import java.util.List;

/**
 * __Grammar__
 *
 * Script: TopLevelStatement*
 * g name name = x;
 * g name = x;
 * name = x;
 * name;
 * g;
 * g = x;
 * Idea: Parse an expression. If the result is just a name, then parse a declaration.
 *
 * TopLevelStatement:
 * | (g | global)? Name(type name)? Name = Expression ;
 * | Expression ;
 * | ControlFlow
 * | TypeDefinition
 *
 * Expression:
 * | ( Expression )
 * | Expression InfixOperator Expression
 * | PrefixOperator Expression
 * | // Weird mixfix stuff like ternary operator
 * | Expression . Name ( Arguments )
 * | Name ( Arguments )
 * | Literal value
 *
 */
public class Parser {

    private final Token[] toks;
    private int pos;

    private final int len;

    public Parser(Token[] toks) {
        this.toks = toks;
        pos = 0;
        len = toks.length;
    }

    private Token peek() {return toks[pos];}

    private Token consume() {return toks[pos++];}

    private Token last() {return toks[pos-1];}

    private boolean check(TokenType... types) {
        if (pos >= len) return false;
        TokenType checked = peek().type();
        for (TokenType t : types)
            if (t == checked)
                return true;
        return false;
    }

    public List<Expression> parseChunk() throws ParserException {
        List<Expression> exprs = new ArrayList<>();
        while (pos != len)
            exprs.add(parseExpression());
        return exprs;
    }

    private Expression parseExpression() throws ParserException {
        //highest priority is assignment
        return parseAssignment();
    }

    private Expression parseAssignment() throws ParserException {
        Expression lhs = parseComparison();
        if (check(ASSIGN)) {
            int tokline = consume().line(); //consume the '='
            if (lhs instanceof Expression.Name name)
                return new Expression.Assign(name.name, parseAssignment());
            if (lhs instanceof Expression.Get get) {
                return new Expression.Set(get.left, get.index, parseAssignment());
            }
            throw new ParserException("Invalid assign target for '=' on line " + tokline);
        }
        return lhs;
    }

    private Expression parseComparison() throws ParserException {
        Expression lhs = parseSum();
        while (check(
                EQUALS, NOT_EQUALS,
                LESS, GREATER,
                LESS_EQUAL, GREATER_EQUAL
        )) {
            Expression.Binary.Op op = Expression.Binary.Op.get(consume().type());
            lhs = new Expression.Binary(lhs, op, parseSum());
        }
        return lhs;
    }

    private Expression parseSum() throws ParserException {
        Expression lhs = parseProduct();
        while (check(PLUS, MINUS)) {
            Expression.Binary.Op op = Expression.Binary.Op.get(consume().type());
            lhs = new Expression.Binary(lhs, op, parseProduct());
        }
        return lhs;
    }

    private Expression parseProduct() throws ParserException {
        Expression lhs = parseUnary();
        while (check(TIMES, DIVIDE, MODULO)) {
            Expression.Binary.Op op = Expression.Binary.Op.get(consume().type());
            lhs = new Expression.Binary(lhs, op, parseUnary());
        }
        return lhs;
    }

    private Expression parseUnary() throws ParserException {
        if (check(NOT, MINUS)) {
            Expression.Unary.Op op = Expression.Unary.Op.get(consume().type());
            return new Expression.Unary(op, parseUnary());
        }
        return parseCallOrGet();
    }

    private Expression parseCallOrGet() throws ParserException {
        Expression lhs = parseUnit();
        while (check(LEFT_PAREN, DOT)) {
            if (check(LEFT_PAREN))
                lhs = new Expression.Call(lhs, parseArguments());
            else {
                int dotLine = consume().line();
                Expression rhs = parseUnit();
                if (rhs instanceof Expression.Name)
                    lhs = new Expression.Get(lhs, rhs);
                else
                    throw new ParserException("Expected name after '.' on line " + dotLine);
            }
        }
        return lhs;
    }

    //Parse expressions and commas until we hit a closing parenthesis
    //This consumes the closing parenthesis
    private List<Expression> parseArguments() throws ParserException {
        if (!check(LEFT_PAREN))
            throw new ParserException("Expected function args, did not find opening parenthesis. This indicates a bug in the parser, contact devs.");
        int openParenLine = consume().line();
        List<Expression> exprs = new ArrayList<>();
        if (check(RIGHT_PAREN)) { //If we find the right paren immediately, just consume it and return
            consume();
            return exprs;
        }
        exprs.add(parseExpression());
        while (check(COMMA)) {
            consume(); //consume comma
            exprs.add(parseExpression()); //parse expr
        }
        //Now that comma statements are all gone, expect right paren
        if (!check(RIGHT_PAREN))
            throw new ParserException("Expected right parenthesis to end arguments list that started on line " + openParenLine);
        consume(); //consume closing parenthesis
        return exprs;
    }

    private Expression parseUnit() throws ParserException {
        return switch (peek().type()) {
            case NAME -> new Expression.Name(consume().getString());
            case INT_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL, LONG_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL -> new Expression.Literal(consume().value()); //Literals
            case FUN -> parseFunction();
            case LEFT_PAREN -> { //Parenthesis for grouping
                int leftLine = consume().line(); //Consume left paren
                Expression inner = parseExpression(); //Read expression
                if (!check(RIGHT_PAREN))
                    throw new ParserException("Expected closing right parenthesis to match left parenthesis on line " + leftLine);
                consume(); //consume right paren
                yield inner;
            }
            case LEFT_CURLY -> parseBlockExpression();
            case IF -> parseIfExpression();
            case WHILE -> null;
            default -> throw new ParserException(peek());
        };
    }

    private Expression parseFunction() throws ParserException {
        if (!check(FUN))
            throw new ParserException("Expected function? Bug with the parser, contact devs");
        int funLine = consume().line();
        List<String> params = parseParams(funLine);
        Expression body = parseExpression();
        return new Expression.Function(params, body);
    }

    private List<String> parseParams(int funLine) throws ParserException {
        if (!check(LEFT_PAREN))
            throw new ParserException("Expected function params, did not find opening parenthesis. This indicates a bug in the parser, contact devs.");
        int openParenLine = consume().line();
        List<String> paramNames = new ArrayList<>();
        if (check(RIGHT_PAREN)) { //If we find the right paren immediately, just consume it and return
            consume();
            return paramNames;
        }
        if (!check(NAME))
            throw new ParserException("Expected name for parameter to function defined on line " + funLine);
        paramNames.add(consume().getString());

        while (check(COMMA)) {
            consume(); //consume comma
            if (!check(NAME))
                throw new ParserException("Expected name for parameter to function defined on line " + funLine);
            paramNames.add(consume().getString());
        }
        //Now that comma statements are all gone, expect right paren
        if (!check(RIGHT_PAREN))
            throw new ParserException("Expected right parenthesis to end parameters list for function " + funLine + " that started on line " + openParenLine);
        consume(); //consume closing parenthesis
        return paramNames;
    }

    private Expression parseBlockExpression() throws ParserException {
        List<Expression> exprs = new ArrayList<>();
        int startLine = consume().line();

        while (pos != len && !check(RIGHT_CURLY)) {
            //Parse expression
            exprs.add(parseExpression());
        }
        if (!check(RIGHT_CURLY))
            throw new ParserException("Expected '}' to close block expression that started on line " + startLine);
        consume(); //consume right curly
        return new Expression.BlockExpression(exprs);
    }

    private Expression parseIfExpression() throws ParserException {
        if (!check(IF))
            throw new ParserException("Expected if statement? Bug with the parser, contact devs");
        consume(); //consume "if"
        Expression condition = parseExpression();
        Expression ifTrue = parseExpression();
        if (check(ELSE)) {
            consume(); //consume "else"
            return new Expression.IfExpression(condition, ifTrue, parseExpression());
        } else {
            return new Expression.IfExpression(condition, ifTrue, null);
        }
    }

    public static class ParserException extends Exception {

        public ParserException(String s) {
            super(s);
        }

        public ParserException(Token tok) {
            super("Unexpected token " + tok + " on line " + tok.line());
        }

    }

}