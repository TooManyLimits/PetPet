package main.java.petpet.lang.parse;

import static main.java.petpet.lang.lex.Lexer.*;
import static main.java.petpet.lang.lex.Lexer.TokenType.*;


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
        boolean global = check(GLOBAL);
        if (global) consume();
        Expression lhs = parseOr();
        if (check(ASSIGN)) {
            int tokline = consume().line(); //consume the '='
            if (lhs instanceof Expression.Name name)
                return new Expression.Assign(lhs.startLine, global, name.name, parseAssignment());
            if (lhs instanceof Expression.Get get) {
                if (global) throw new ParserException("Cannot use global on indexing operation (line " + tokline + ")");
                return new Expression.Set(lhs.startLine, get.left, get.indexer, parseAssignment());
            }
            throw new ParserException("Invalid assign target for '=' on line " + tokline);
        }
        return lhs;
    }

    private Expression parseOr() throws ParserException {
        Expression lhs = parseAnd();
        while (check(OR)) {
            lhs = new Expression.Logical(consume().line(), false, lhs, parseAnd());
        }
        return lhs;
    }

    private Expression parseAnd() throws ParserException {
        Expression lhs = parseComparison();
        while (check(AND)) {
            lhs = new Expression.Logical(consume().line(), true, lhs, parseComparison());
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
            Token opToken = consume();
            Expression.Binary.Op op = Expression.Binary.Op.get(opToken.type());
            lhs = new Expression.Binary(opToken.line(), lhs, op, parseSum());
        }
        return lhs;
    }

    private Expression parseSum() throws ParserException {
        Expression lhs = parseProduct();
        while (check(PLUS, MINUS)) {
            Token opToken = consume();
            Expression.Binary.Op op = Expression.Binary.Op.get(opToken.type());
            lhs = new Expression.Binary(opToken.line(), lhs, op, parseProduct());
        }
        return lhs;
    }

    private Expression parseProduct() throws ParserException {
        Expression lhs = parseUnary();
        while (check(TIMES, DIVIDE, MODULO)) {
            Token opToken = consume();
            Expression.Binary.Op op = Expression.Binary.Op.get(opToken.type());
            lhs = new Expression.Binary(opToken.line(), lhs, op, parseUnary());
        }
        return lhs;
    }

    private Expression parseUnary() throws ParserException {
        if (check(NOT, MINUS)) {
            Token opToken = consume();
            Expression.Unary.Op op = Expression.Unary.Op.get(opToken.type());
            return new Expression.Unary(opToken.line(), op, parseUnary());
        }
        return parseCallOrGet();
    }

    private Expression parseCallOrGet() throws ParserException {
        Expression lhs = parseUnit();
        while (check(LEFT_PAREN, DOT, COLON, LEFT_SQUARE) && !check(SEMICOLON)) {
            if (check(LEFT_PAREN)) {
                int openParenLine = consume().line();
                if (lhs instanceof Expression.Get get && (!(lhs instanceof Expression.Get.Strong)))
                    lhs = new Expression.Invoke(openParenLine, get.left, get.indexer, parseArguments(openParenLine));
                else
                    lhs = new Expression.Call(openParenLine, lhs, parseArguments(openParenLine));
            } else if (check(DOT, COLON)) {
                boolean strong = peek().type() == COLON;
                int indexerLine = consume().line();
                if (check(NAME)) {
                    Token name = consume();
                    int line = name.line();
                    String val = name.getString();
                    if (strong)
                        lhs = new Expression.Get.Strong(indexerLine, lhs, new Expression.Literal(line, val));
                    else
                        lhs = new Expression.Get(indexerLine, lhs, new Expression.Literal(line, val));
                } else {
                    String indexerSymbol = strong ? ":" : ".";
                    throw new ParserException("Expected name after '" + indexerSymbol + "' on line " + indexerLine);
                }
            } else {
                int openSquareLine = consume().line();
                Expression indexer = parseExpression();
                if (!check(RIGHT_SQUARE))
                    throw new ParserException("Expected ] to end indexing operation on line " + openSquareLine);
                consume(); //consume right square bracket
                lhs = new Expression.Get(openSquareLine, lhs, indexer);
            }
        }
        if (check(SEMICOLON)) consume(); //consume the semi
        return lhs;
    }

    //Parse expressions and commas until we hit a closing parenthesis
    //This consumes the closing parenthesis
    private List<Expression> parseArguments(int openParenLine) throws ParserException {
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
            case NAME -> new Expression.Name(peek().line(), consume().getString());
            case THIS -> new Expression.This(consume().line());
            case NUMBER_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL -> new Expression.Literal(peek().line(), consume().value()); //Literals
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
            case WHILE -> parseWhileExpression();
            default -> throw new ParserException(peek());
        };
    }

    private Expression parseFunction() throws ParserException {
        if (!check(FUN))
            throw new ParserException("Expected function? Bug with the parser, contact devs");
        int funLine = consume().line();
        List<String> params = parseParams(funLine);
        Expression body = parseExpression();
        return new Expression.Function(funLine, params, body);
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
        return new Expression.BlockExpression(startLine, exprs);
    }

    private Expression parseIfExpression() throws ParserException {
        if (!check(IF))
            throw new ParserException("Expected if expression? Bug with the parser, contact devs");
        int ifLine = consume().line(); //consume "if"
        Expression condition = parseExpression();
        Expression ifTrue = parseExpression();
        if (check(ELSE)) {
            consume(); //consume "else"
            return new Expression.IfExpression(ifLine, condition, ifTrue, parseExpression());
        } else {
            return new Expression.IfExpression(ifLine, condition, ifTrue, null);
        }
    }

    private Expression parseWhileExpression() throws ParserException {
        if (!check(WHILE))
            throw new ParserException("Expected while expression? Bug with parser, contact devs");
        int whileLine = consume().line();
        Expression condition = parseExpression();
        Expression body = parseExpression();
        return new Expression.While(whileLine, condition, body);
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