package me.rerere.matrix.commands.condition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.rerere.matrix.thirdparty.kotlin.text.Regex;
import me.rerere.matrix.config.MetadataKind;

/** Lexer and parser for command expressions. */
public final class CommandExpressionParser {
    private CommandExpressionParser() { }

    private static CharSequence tokenValue(ExpressionToken token) {
        return token.getValue();
    }

    private static CharSequence tokenTextForJoin(ExpressionToken token) {
        return token.getValue();
    }

    private static String removeComments(String input) {
        return new Regex("(//|#).*").replace(input, "");
    }

    private static boolean isKind(ExpressionToken token,
                                  MetadataKind kind) {
        return token.getType() == kind;
    }

    private static String joinTokens(List<ExpressionToken> tokens, int start, int end) {
        StringBuilder joinedTokens = new StringBuilder();
        for (int i = start; i < end; i++) {
            joinedTokens.append(tokenTextForJoin(tokens.get(i)));
        }
        return trimKotlin(joinedTokens.toString());
    }

    private static String trimKotlin(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isKotlinWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isKotlinWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isKotlinWhitespace(char ch) {
        return Character.isWhitespace(ch) || Character.isSpaceChar(ch);
    }

    private static List<String> splitArguments(List<ExpressionToken> tokens) {
        if (tokens.isEmpty()) throw new IllegalStateException("Empty expression");
        if (!isKind(tokens.get(0), MetadataKind.WORDS)) {
            throw new IllegalStateException("Expression must start with a function name");
        }

        int leftParenthesis = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (isKind(tokens.get(i), MetadataKind.LEFT_PAREN)) {
                leftParenthesis = i;
                break;
            }
        }
        if (leftParenthesis == -1) throw new IllegalStateException("Expression must contain a left parenthesis");

        int rightParenthesis = -1;
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (isKind(tokens.get(i), MetadataKind.RIGHT_PAREN)) {
                rightParenthesis = i;
                break;
            }
        }
        if (rightParenthesis == -1) throw new IllegalStateException("Expression must contain a right parenthesis");
        if (leftParenthesis > rightParenthesis) throw new IllegalStateException("Left parenthesis must be before right parenthesis");

        List<String> arguments = new ArrayList<String>();
        int nestingDepth = 1;
        int argumentStart = leftParenthesis + 1;
        for (int i = leftParenthesis + 1; i < rightParenthesis; i++) {
            ExpressionToken token = tokens.get(i);
            if (isKind(token, MetadataKind.LEFT_PAREN)) {
                nestingDepth++;
            } else if (isKind(token, MetadataKind.RIGHT_PAREN)) {
                nestingDepth--;
            } else if (isKind(token, MetadataKind.COMMA) && nestingDepth == 1) {
                String argumentText = joinTokens(tokens, argumentStart, i);
                arguments.add(argumentText);
                argumentStart = i + 1;
            }
        }
        String finalArgument = joinTokens(tokens, argumentStart, rightParenthesis);
        if (!finalArgument.isEmpty()) arguments.add(finalArgument);

        List<String> normalized = new ArrayList<String>(arguments.size());
        for (String argument : arguments) {
            int length = argument.length();
            if (length >= 2 && argument.charAt(0) == '"' && argument.charAt(length - 1) == '"') {
                normalized.add(argument.substring(1, length - 1));
            } else if (length >= 2 && argument.charAt(0) == '\''
                    && argument.charAt(length - 1) == '\'') {
                normalized.add(argument.substring(1, length - 1));
            } else {
                normalized.add(argument);
            }
        }
        return normalized;
    }

    public static me.rerere.matrix.util.FunctionInfo parseCommand(String input) {
        String expression = trimKotlin(removeComments(input));
        List<ExpressionToken> tokens = lexTokens(expression);
        String functionName = tokenValue(tokens.get(0)).toString();
        List<String> arguments = splitArguments(tokens);
        List<String> positional = new ArrayList<String>();
        Map<String, String> named = new LinkedHashMap<String, String>();
        for (String argument : arguments) {
            if (new Regex("\\w+\\s*=\\s*.*").matches(argument)) {
                int equals = argument.indexOf('=');
                String key = trimKotlin(argument.substring(0, equals));
                String value = trimKotlin(argument.substring(equals + 1));
                named.put(key, value);
            } else {
                positional.add(argument);
            }
        }
        return new me.rerere.matrix.util.FunctionInfo(functionName, arguments, positional, named);
    }

    public static void printParserSample() {
        me.rerere.matrix.util.FunctionInfo command =
                parseCommand("cmd(\"matrix status %player_name%\")");
        System.out.println(command);
        System.out.println(command.indexedFunction(0));
    }

    public static void main(String[] args) {
        printParserSample();
    }

    private static List<ExpressionToken> lexTokens(String input) {
        List<ExpressionToken> result = new ArrayList<ExpressionToken>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            switch (c) {
                case '(':
                    result.add(new ExpressionToken("(", MetadataKind.LEFT_PAREN));
                    i++;
                    continue;
                case ')':
                    result.add(new ExpressionToken(")", MetadataKind.RIGHT_PAREN));
                    i++;
                    continue;
                case ',':
                    result.add(new ExpressionToken(",", MetadataKind.COMMA));
                    i++;
                    continue;
                case '\t':
                case '\n':
                    i++;
                    continue;
                case ' ':
                    result.add(new ExpressionToken(" ", MetadataKind.WHITE_SPACE));
                    i++;
                    continue;
                case '"':
                case '\'': {
                    int start = i++;
                    while (i < input.length() && input.charAt(i) != c) i++;
                    result.add(new ExpressionToken(input.substring(start, i + 1),
                            MetadataKind.WORDS));
                    i++;
                    continue;
                }
                default: {
                    int start = i++;
                    while (i < input.length()) {
                        char next = input.charAt(i);
                        if (next == ' ' || next == '\t' || next == '\n'
                                || next == '(' || next == ')' || next == ',') break;
                        i++;
                    }
                    result.add(new ExpressionToken(input.substring(start, i),
                            MetadataKind.WORDS));
                }
            }
        }
        return result;
    }
}
