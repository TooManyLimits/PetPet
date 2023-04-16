package petpet.lang.parse;

import petpet.lang.run.PetPetFunction;

import static petpet.lang.lex.Lexer.*;
import static petpet.lang.lex.Lexer.TokenType.*;


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

    //checks further ahead
    private boolean checkAhead(int offset, TokenType... types) {
        if (pos + offset >= len) return false;
        TokenType checked = toks[pos + offset].type();
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
        if (global) {
            if (!checkAhead(1, FUNCTION)) //"global fn ..." should not be parsed here, but instead later on!
                consume();
            else //This is a "global fn" situation, just return and let the later function handle it
                return parseOr();
        }
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
            case GLOBAL, FUNCTION -> parseFunction(); //Global as well, since "global fn ..."
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
            case LIST_START -> parseListConstructor();
            case TABLE_START -> parseTableConstructor();
            case NULL_LITERAL -> new Expression.Null(consume().line());
            case RETURN -> parseReturn();
            default -> throw new ParserException(peek());
        };
    }

    private Expression parseFunction() throws ParserException {
        boolean global = check(GLOBAL);
        if (global)
            consume();
        if (!check(FUNCTION))
            throw new ParserException("Expected function? Bug with the parser, contact devs");
        int funLine = consume().line();

        if (check(LEFT_PAREN)) {
            //anonymous function, a "fn() ..." situation. ensure not global
            if (global) throw new ParserException("Cannot have global anonymous function! line = " + funLine);
            List<String> params = parseParams(funLine);
            Expression body = parseExpression();
            return new Expression.Function(funLine, null, params, body);
        } else {
            //This function has a name! It's a "global? fn <name>()" situation here.
            //Some strangeness is done here to allow interesting function defining abilities like Lua has.
            if (!check(NAME))
                throw new ParserException("Expected either a name (named) or parentheses (anonymous) for function declaration on line " + funLine);
            int nameLine = peek().line();
            Expression funcNameExpr = new Expression.Name(nameLine, consume().getString());
            while (check(DOT, COLON)) {
                if (global)
                    throw new ParserException("Cannot create \"global fn\" with indexing in name. line = " + funLine);
                int dotLine = peek().line();
                char c = consume().type() == DOT ? '.' : ':';
                if (!check(NAME))
                    throw new ParserException("Expected name after " + c + " for function declaration on line " + funLine);
                funcNameExpr = new Expression.Get(dotLine, funcNameExpr, new Expression.Literal(peek().line(), consume().getString()));
            }

            List<String> params = parseParams(funLine);
            Expression body = parseExpression();
            //If the funcNameExpr is just a name, use that name, otherwise, use the last name in the chain
            if (funcNameExpr instanceof Expression.Name nameExpr) {
                Expression func = new Expression.Function(funLine, nameExpr.name, params, body);
                return new Expression.Assign(funLine, global, nameExpr.name, func);
            } else {
                Expression.Get getExpr = (Expression.Get) funcNameExpr;
                Expression.Literal nameExpr = (Expression.Literal) getExpr.indexer;
                Expression func = new Expression.Function(funLine, (String) nameExpr.value, params, body);
                return new Expression.Set(funLine, getExpr.left, getExpr.indexer, func);
            }
        }
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
        int whileLine = consume().line();
        Expression condition = parseExpression();
        Expression body = parseExpression();
        return new Expression.While(whileLine, condition, body);
    }

    private Expression parseListConstructor() throws ParserException {
        int startLine = consume().line();
        if (check(RIGHT_SQUARE)) { //If we find the right square immediately, just consume it and return empty list
            consume();
            return new Expression.ListConstructor(startLine, List.of());
        }

        List<Expression> exprs = new ArrayList<>();
        exprs.add(parseExpression());
        while (check(COMMA)) {
            consume(); //consume comma
            exprs.add(parseExpression());
        }
        //Now that comma statements are all gone, expect right square
        if (!check(RIGHT_SQUARE))
            throw new ParserException("Expected ] to end list constructor on line " + startLine);
        consume(); //consume right square
        return new Expression.ListConstructor(startLine, exprs);
    }

    private Expression parseTableConstructor() throws ParserException {
        int startLine = consume().line();
        if (check(RIGHT_SQUARE)) { //If we find the right side immediately, just consume it and return empty
            consume();
            return new Expression.TableConstructor(startLine, List.of());
        }
        List<Expression> keysValues = new ArrayList<>();

        if (check(NAME)) {
            keysValues.add(new Expression.Literal(peek().line(), consume().getString()));
        } else if (check(LEFT_SQUARE)) {
            int leftLine = consume().line();
            keysValues.add(parseExpression());
            if (!check(RIGHT_SQUARE))
                throw new ParserException("Expected ] for key (line="+leftLine+") inside table constructor (line="+startLine+")");
            consume();
        } else {
            throw new ParserException("Keys in table constructor (line="+startLine+") must either be names or expressions inside []");
        }
        if (!check(ASSIGN))
            throw new ParserException("Expected = for key-value pair (line="+last().line()+") in table constructor (line=" + startLine + ")");
        consume();
        keysValues.add(parseExpression());
        while (check(COMMA)) {
            consume(); //consume comma
            if (check(NAME)) {
                keysValues.add(new Expression.Literal(peek().line(), consume().getString()));
            } else if (check(LEFT_SQUARE)) {
                int leftLine = consume().line();
                keysValues.add(parseExpression());
                if (!check(RIGHT_SQUARE))
                    throw new ParserException("Expected ] for key (line="+leftLine+") inside table constructor (line="+startLine+")");
                consume();
            } else {
                throw new ParserException("Keys in table constructor (line="+startLine+") must either be names or expressions inside []");
            }
            if (!check(ASSIGN))
                throw new ParserException("Expected = for key-value pair (line="+last().line()+") in table constructor (line=" + startLine + ")");
            consume();
            keysValues.add(parseExpression());
        }
        //Now that comma statements are all gone, expect right square
        if (!check(RIGHT_SQUARE))
            throw new ParserException("Expected ] to end table constructor on line " + startLine);
        consume(); //consume right square
        return new Expression.TableConstructor(startLine, keysValues);
    }

    private Expression parseReturn() throws ParserException {
        int startLine = consume().line();
        Expression retVal = parseExpression();
        return new Expression.Return(startLine, retVal);
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