package com.budlee.jlox;

import java.util.Objects;

public class Token {

	final TokenType tokenType;
	final String lexme;
	final Object literal;
	final int line;


	Token(TokenType tokenType, String lexme, Object literal, int line) {
		this.tokenType = tokenType;
		this.lexme = lexme;
		this.literal = literal;
		this.line = line;
	}

	@Override
	public String toString() {
		return String.format("%s %s %s",tokenType, lexme, literal );
	}
}
