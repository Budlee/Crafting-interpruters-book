package com.budlee.jlox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LoxScanner {

	private static final Map<String, TokenType> KEYWORDS;

	static {
		Map<String, TokenType> keywordsTempMap = new HashMap<>();
		keywordsTempMap.put("and", TokenType.AND);
		keywordsTempMap.put("class", TokenType.CLASS);
		keywordsTempMap.put("else", TokenType.ELSE);
		keywordsTempMap.put("false", TokenType.FALSE);
		keywordsTempMap.put("for", TokenType.FOR);
		keywordsTempMap.put("fun", TokenType.FUN);
		keywordsTempMap.put("if", TokenType.IF);
		keywordsTempMap.put("nil", TokenType.NIL);
		keywordsTempMap.put("or", TokenType.OR);
		keywordsTempMap.put("print", TokenType.PRINT);
		keywordsTempMap.put("return", TokenType.RETURN);
		keywordsTempMap.put("super", TokenType.SUPER);
		keywordsTempMap.put("this", TokenType.THIS);
		keywordsTempMap.put("true", TokenType.TRUE);
		keywordsTempMap.put("var", TokenType.VAR);
		keywordsTempMap.put("while", TokenType.WHILE);

		KEYWORDS = Collections.unmodifiableMap(keywordsTempMap);
	}

	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;


	public LoxScanner(String source) {
		this.source = source;
	}

	List<Token> scanTokens() {
		while (!isAtEnd()) {
			start = current;
			scanToken();
		}
		tokens.add(new Token(TokenType.EOF, "", null, line));
		return tokens;
	}

	private void scanToken() {
		char c = advance();
		switch (c) {
		case '(':
			addToken(TokenType.LEFT_PAREN);
			break;
		case ')':
			addToken(TokenType.RIGHT_PAREN);
			break;
		case '{':
			addToken(TokenType.LEFT_BRACE);
			break;
		case '}':
			addToken(TokenType.RIGHT_BRACE);
			break;
		case ',':
			addToken(TokenType.COMMA);
			break;
		case '.':
			addToken(TokenType.DOT);
			break;
		case '-':
			addToken(TokenType.MINUS);
			break;
		case '+':
			addToken(TokenType.PLUS);
			break;
		case ';':
			addToken(TokenType.SEMICOLON);
			break;
		case '*':
			addToken(TokenType.STAR);
			break;
		case '!':
			addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
			break;
		case '=':
			addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
			break;
		case '>':
			addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
			break;
		case '<':
			addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
			break;
		case '/':
			if (match('/')) {
				while (peek() != '\n' && !isAtEnd()) {
					advance();
				}
			}
			else if (match('*')) {
				commentBlock();
			}
			else {
				addToken(TokenType.SLASH);
			}
			break;
//		Ignore White space
		case ' ':
		case '\r':
		case '\t':
			break;
		case '\n':
			line++;
			break;
		case '"':
			string();
			break;
		default:
			if (isDigit(c)) {
				number();
			}
			else if (isAlpha(c)) {
				identifier();
			}
			else {
				Lox.error(line, "Unexpected character.");
			}
			break;
		}
	}

	private void commentBlock() {
		while (!(peek()  == '*' && peekNext() == '/') && !isAtEnd()) {
			if (peek() == '\n') {
				line++;
			}
			advance();
		}
		if (isAtEnd()) {
			Lox.error(line, "Unterminated Comment Block.");
			return;
		}
		advance();
		advance();
		String value = source.substring(start + 2, current - 2);
		System.out.println(String.format("Comment block was [%s]", value));
	}

	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z') ||
				c == '_';
	}

	private void identifier() {
		while (isAlphaNumeric(peek())) {
			advance();
		}
		var text = source.substring(start, current);
		TokenType tokenType = KEYWORDS.get(text);
		if (Objects.isNull(tokenType)) {
			tokenType = TokenType.IDENTIFIER;
		}
		addToken(tokenType);
	}

	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	private void number() {
		while (isDigit(peek()) && !isAtEnd()) {
			advance();
		}
		// look for the fractal part.
		if (peek() == '.' && isDigit(peekNext())) {
			advance();
		}
		while (isDigit(peek())) {
			advance();
		}
		addToken(TokenType.NUMBER,
				Double.parseDouble(source.substring(start, current)));
	}

	private char peekNext() {
		if (current + 1 >= source.length()) {
			return '\0';
		}
		return source.charAt(current + 1);
	}

	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * Supports multi line strings
	 */
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') {
				line++;
			}
			advance();
		}
		if (isAtEnd()) {
			Lox.error(line, "Unterminated  string.");
			return;
		}
		advance();
		String value = source.substring(start + 1, current - 1);
		addToken(TokenType.STRING, value);
	}

	private char peek() {
		return isAtEnd() ? '\0'
				: source.charAt(current);
	}

	private boolean match(char expected) {
		if (isAtEnd()) {
			return false;
		}
		if (source.charAt(current) != expected) {
			return false;
		}
		current++;
		return true;

	}

	private void addToken(TokenType tokenType) {
		addToken(tokenType, null);
	}

	private void addToken(TokenType tokenType, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(tokenType, text, literal, line));
	}

	/**
	 * Move current position of character forward one
	 * @return The current character
	 */
	private char advance() {
		return source.charAt(current++);
	}

	private boolean isAtEnd() {
		return current >= source.length();
	}
}
