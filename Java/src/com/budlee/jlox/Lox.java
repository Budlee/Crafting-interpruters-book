package com.budlee.jlox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class Lox {
	private static final Interpreter interpreter = new Interpreter();
	private static final ReplInterpreter replInterpreter = new ReplInterpreter();
	private static boolean hadError;
	private static boolean hadRuntimeError = false;

	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("usage: jlox [scrpts]");
			System.exit(64);
		}
		else if (args.length == 1) {
			runFile(args[0]);
		}
		else {
			runPrompt();
		}
	}

	private static void runPrompt() throws IOException {
		final InputStreamReader inputStreamReader = new InputStreamReader(System.in);
		final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		while (true) {
			System.out.print("> ");
			final String line = bufferedReader.readLine();
			if (Objects.isNull(line)) {
				break;
			}
			run(line, replInterpreter);
			hadError = false;
		}
	}

	private static void runFile(String path) throws IOException {
		final byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()), interpreter);
		if (hadError) {
			System.exit(65);
		}
		if (hadRuntimeError) {
			System.exit(70);
		}
	}

	private static void run(String source, Interpreter interpreter) {
		final LoxScanner scanner = new LoxScanner(source);
		final List<Token> tokens = scanner.scanTokens();
//		tokens.stream()
//				.forEach(System.out::println);
		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

		if (hadError) {
			return;
		}

		Resolver resolver = new Resolver(interpreter);
		resolver.resolve(statements);

		if (hadError) {
			return;
		}

//		System.out.println(new ASTPrinter().print(expression));
		interpreter.interpret(statements);
//		Lox.interpreter.interpret(statements);

	}

	static void error(int line, String message) {
		report(line, "", message);
	}


	static void error(Token token, String message) {
		if (token.tokenType == TokenType.EOF) {
			report(token.line, " at end", message);
		}
		else {
			report(token.line, String.format(" at '%s'", token.lexme), message);
		}
	}

	private static void report(int line, String where, String message) {
		System.err.println(String.format("[line %s] Error %s: %s", line, where, message));
		hadError = true;
	}

	static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() + String.format("\n[line %s]", error.token.line));
		hadRuntimeError = true;
	}
}
