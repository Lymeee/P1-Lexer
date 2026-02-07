package plc.project;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            if (peek("[ \\n\\r\\t\\u0008]")) {
                match("[ \\n\\r\\t\\u0008]");
                continue;
            }
            tokens.add(lexToken());
        }
        return tokens;
    }

    public Token lexToken() {
        chars.skip();

        if (peek("[A-Za-z_]")) {
            return lexIdentifier();
        } else if (peek("[+-]", "[0-9]") || peek("[0-9]")) {
            return lexNumber();
        } else if (peek("'")) {
            return lexCharacter();
        } else if (peek("\"")) {
            return lexString();
        } else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        if (!match("[A-Za-z_]")) {
            throw new ParseException("Invalid identifier start.", chars.index);
        }
        while (peek("[A-Za-z0-9_-]")) {
            match("[A-Za-z0-9_-]");
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        match("[+-]");

        if (!chars.has(0) || !String.valueOf(chars.get(0)).matches("[0-9]")) {
            throw new ParseException("Invalid number literal.", chars.index);
        }

        char firstDigit = chars.get(0);
        match("[0-9]");
        int digitCount = 1;

        while (peek("[0-9]")) {
            match("[0-9]");
            digitCount++;
        }

        if (peek("\\.", "[0-9]")) {
            match("\\.", "[0-9]");
            while (peek("[0-9]")) {
                match("[0-9]");
            }
            return chars.emit(Token.Type.DECIMAL);
        } else {
            if (digitCount > 1 && firstDigit == '0') {
                throw new ParseException("Leading zeros are not allowed.", chars.index);
            }
            return chars.emit(Token.Type.INTEGER);
        }
    }

    public Token lexCharacter() {
        if (!match("'")) {
            throw new ParseException("Invalid character literal.", chars.index);
        }

        if (!chars.has(0)) {
            throw new ParseException("Unterminated character literal.", chars.index);
        }

        if (peek("\\\\")) {
            lexEscape();
        } else {
            char c = chars.get(0);
            if (c == '\'' || c == '\n' || c == '\r' || c == '\\') {
                throw new ParseException("Invalid character literal.", chars.index);
            }
            chars.advance();
        }

        if (!match("'")) {
            throw new ParseException("Unterminated character literal.", chars.index);
        }

        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        if (!match("\"")) {
            throw new ParseException("Invalid string literal.", chars.index);
        }

        while (true) {
            if (!chars.has(0)) {
                throw new ParseException("Unterminated string literal.", chars.index);
            }

            if (peek("\"")) {
                match("\"");
                break;
            }

            if (peek("\\\\")) {
                lexEscape();
            } else {
                char c = chars.get(0);
                if (c == '\n' || c == '\r') {
                    throw new ParseException("Unterminated string literal.", chars.index);
                }
                if (c == '\\') {
                    throw new ParseException("Invalid escape sequence.", chars.index);
                }
                chars.advance();
            }
        }

        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() {
        if (!match("\\\\")) {
            throw new ParseException("Invalid escape.", chars.index);
        }

        if (!chars.has(0)) {
            throw new ParseException("Invalid escape.", chars.index);
        }

        char c = chars.get(0);
        switch (c) {
            case 'b':
            case 'n':
            case 'r':
            case 't':
            case '\'':
            case '"':
            case '\\':
                chars.advance();
                break;
            default:
                throw new ParseException("Invalid escape.", chars.index);
        }
    }

    public Token lexOperator() {
        if (peek("[<>!=]")) {
            match("[<>!=]");
            match("=");
        } else {
            if (!chars.has(0)) {
                throw new ParseException("Invalid operator.", chars.index);
            }
            chars.advance();
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i)) {
                return false;
            }
            if (!String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean match(String... patterns) {
        if (!peek(patterns)) {
            return false;
        }
        for (int i = 0; i < patterns.length; i++) {
            chars.advance();
        }
        return true;
    }

    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}

