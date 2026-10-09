package me.rerere.matrix.commands.condition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;

/** Parses and evaluates the boolean expressions accepted by Matrix commands. */
public class Expression {
    private final BooleanExpression rootExpression;

    public Expression(String expressionText) {
        BooleanLexer lexer = new BooleanLexer(expressionText);
        BooleanParser parser = new BooleanParser(lexer);
        this.rootExpression = parser.build();
    }

    @Override
    public String toString() {
        return rootExpression.toString();
    }

    public boolean evaluate() {
        return ((Boolean) rootExpression.interpret()).booleanValue();
    }

    public interface BooleanExpression {
        Object interpret();
    }

    public abstract static class NonTerminal implements BooleanExpression {
        public BooleanExpression left;
        public BooleanExpression right;

        public void setLeft(BooleanExpression expression) {
            this.left = expression;
        }

        public void setRight(BooleanExpression expression) {
            this.right = expression;
        }
    }

    public abstract static class Terminal implements BooleanExpression {
        public final Object value;

        public Terminal(Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("%s", value);
        }

        @Override
        public Object interpret() {
            return value;
        }
    }

    public static final class Number extends Terminal {
        public Number(double value) {
            super(Double.valueOf(value));
        }
    }

    public static final class And extends NonTerminal {
        @Override
        public Object interpret() {
            boolean leftValue = ((Boolean) left.interpret()).booleanValue();
            boolean rightValue = ((Boolean) right.interpret()).booleanValue();
            return Boolean.valueOf(leftValue && rightValue);
        }

        @Override
        public String toString() {
            return String.format("(%s & %s)", left, right);
        }
    }

    public static final class Or extends NonTerminal {
        @Override
        public Object interpret() {
            boolean leftValue = ((Boolean) left.interpret()).booleanValue();
            boolean rightValue = ((Boolean) right.interpret()).booleanValue();
            return Boolean.valueOf(leftValue || rightValue);
        }

        @Override
        public String toString() {
            return String.format("(%s | %s)", left, right);
        }
    }

    public static final class Not extends NonTerminal {
        public void setChild(BooleanExpression expression) {
            setLeft(expression);
        }

        @Override
        public void setRight(BooleanExpression ignored) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object interpret() {
            boolean value = ((Boolean) left.interpret()).booleanValue();
            return Boolean.valueOf(!value);
        }

        @Override
        public String toString() {
            return String.format("!%s", left);
        }
    }

    public static final class CmpEquals extends NonTerminal {
        @Override
        public Object interpret() {
            double leftValue = ((Double) left.interpret()).doubleValue();
            double rightValue = ((Double) right.interpret()).doubleValue();
            return Boolean.valueOf(leftValue == rightValue);
        }

        @Override
        public String toString() {
            return String.format("(%s = %s)", left, right);
        }
    }

    public static final class CmpGreaterThan extends NonTerminal {
        @Override
        public Object interpret() {
            double leftValue = ((Double) left.interpret()).doubleValue();
            double rightValue = ((Double) right.interpret()).doubleValue();
            return Boolean.valueOf(leftValue > rightValue);
        }

        @Override
        public String toString() {
            return String.format("(%s > %s)", left, right);
        }
    }

    public static final class CmpLessThan extends NonTerminal {
        @Override
        public Object interpret() {
            double leftValue = ((Double) left.interpret()).doubleValue();
            double rightValue = ((Double) right.interpret()).doubleValue();
            return Boolean.valueOf(leftValue < rightValue);
        }

        @Override
        public String toString() {
            return String.format("(%s < %s)", left, right);
        }
    }

    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    public static class BooleanLexer {
        private final StreamTokenizer tokenizer;

        public enum Symbol {
            EOF,
            EOL,
            OR,
            AND,
            NOT,
            LEFT,
            RIGHT,
            CMP_EQUALS,
            CMP_LESS_THAN,
            CMP_GREATER_THAN,
            NUMBER
        }

        public static class SymbolObject {
            private final Symbol tokenKind;
            private final Double numericValue;

            public SymbolObject(Symbol symbol, Double data) {
                this.tokenKind = symbol;
                this.numericValue = Double.valueOf(data == null ? 0.0D : data.doubleValue());
            }

            public double getData() {
                return numericValue == null ? 0.0D : numericValue.doubleValue();
            }

            public Symbol getSymbol() {
                return tokenKind;
            }
        }

        public BooleanLexer(String expressionText) {
            this.tokenizer = new StreamTokenizer(new BufferedReader(new StringReader(expressionText)));
            tokenizer.resetSyntax();
            tokenizer.wordChars('a', 'z');
            tokenizer.wordChars('A', 'Z');
            tokenizer.whitespaceChars(0, 32);
            tokenizer.whitespaceChars(10, 9);
            tokenizer.ordinaryChar('(');
            tokenizer.ordinaryChar(')');
            tokenizer.ordinaryChar('&');
            tokenizer.ordinaryChar('|');
            tokenizer.ordinaryChar('!');
            tokenizer.ordinaryChar('>');
            tokenizer.ordinaryChar('=');
            tokenizer.ordinaryChar('<');
            tokenizer.parseNumbers();
        }

        public SymbolObject nextToken() {
            Symbol symbol;
            Double data = null;
            int token;
            try {
                token = tokenizer.nextToken();
                switch (token) {
                    case 10:
                        symbol = Symbol.EOL;
                        break;
                    case StreamTokenizer.TT_EOF:
                        symbol = Symbol.EOF;
                        break;
                    case StreamTokenizer.TT_NUMBER:
                        symbol = Symbol.NUMBER;
                        data = Double.valueOf(tokenizer.nval);
                        break;
                    case '!':
                        symbol = Symbol.NOT;
                        break;
                    case '&':
                        symbol = Symbol.AND;
                        break;
                    case '(':
                        symbol = Symbol.LEFT;
                        break;
                    case ')':
                        symbol = Symbol.RIGHT;
                        break;
                    case '<':
                        symbol = Symbol.CMP_LESS_THAN;
                        break;
                    case '=':
                        symbol = Symbol.CMP_EQUALS;
                        break;
                    case '>':
                        symbol = Symbol.CMP_GREATER_THAN;
                        break;
                    case '|':
                        symbol = Symbol.OR;
                        break;
                    default:
                        throw new RuntimeException("Lexer error: Got an invalid token");
                }
            } catch (IOException ignored) {
                symbol = Symbol.EOF;
            }
            return new SymbolObject(symbol, data);
        }
    }

    public static class BooleanParser {
        private final BooleanLexer lexer;
        private BooleanExpression parsedExpression;
        private BooleanLexer.SymbolObject lookaheadToken;

        public BooleanParser(BooleanLexer lexer) {
            this.lexer = lexer;
        }

        public BooleanExpression build() {
            try {
                parseExpression();
                parsedExpression.interpret();
            } catch (NullPointerException ignored) {
                throw new ParseException("Failed to parse: root expression is null.");
            } catch (Exception exception) {
                throw new ParseException(concatExceptionMessage(
                        exception.getClass().getSimpleName(), exception.getMessage()));
            }
            return parsedExpression;
        }

        // OR is the outermost level, so AND binds more tightly.
        private void parseExpression() {
            parseTerm();
            while (lookaheadToken.getSymbol() == BooleanLexer.Symbol.OR) {
                Or expression = new Or();
                expression.setLeft(parsedExpression);
                parseTerm();
                expression.setRight(parsedExpression);
                parsedExpression = expression;
            }
        }

        private void parseTerm() {
            parseFactor();
            while (lookaheadToken.getSymbol() == BooleanLexer.Symbol.AND) {
                And term = new And();
                term.setLeft(parsedExpression);
                parseFactor();
                term.setRight(parsedExpression);
                parsedExpression = term;
            }
        }

        private void parseFactor() {
            parseComparison();
            if (lookaheadToken.getSymbol() == BooleanLexer.Symbol.NOT) {
                Not expression = new Not();
                parseFactor();
                expression.setChild(parsedExpression);
                parsedExpression = expression;
            } else if (lookaheadToken.getSymbol() == BooleanLexer.Symbol.LEFT) {
                parseExpression();
                lookaheadToken = lexer.nextToken();
            }
        }

        private void parseComparison() {
            parseConstant();
            if (lookaheadToken.getSymbol() == BooleanLexer.Symbol.CMP_EQUALS) {
                CmpEquals comparison = new CmpEquals();
                comparison.setLeft(parsedExpression);
                parseConstant();
                comparison.setRight(parsedExpression);
                parsedExpression = comparison;
            } else if (lookaheadToken.getSymbol() == BooleanLexer.Symbol.CMP_GREATER_THAN) {
                CmpGreaterThan comparison = new CmpGreaterThan();
                comparison.setLeft(parsedExpression);
                parseConstant();
                comparison.setRight(parsedExpression);
                parsedExpression = comparison;
            } else if (lookaheadToken.getSymbol() == BooleanLexer.Symbol.CMP_LESS_THAN) {
                CmpLessThan comparison = new CmpLessThan();
                comparison.setLeft(parsedExpression);
                parseConstant();
                comparison.setRight(parsedExpression);
                parsedExpression = comparison;
            }
        }

        private void parseConstant() {
            lookaheadToken = lexer.nextToken();
            if (lookaheadToken.getSymbol() == BooleanLexer.Symbol.NUMBER) {
                parsedExpression = new Number(lookaheadToken.getData());
                lookaheadToken = lexer.nextToken();
            }
        }

        private static String concatExceptionMessage(String className, String message) {
            return new StringBuilder(className).append(": ").append(message).toString();
        }
    }
}
