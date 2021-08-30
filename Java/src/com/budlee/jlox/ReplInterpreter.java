package com.budlee.jlox;

import java.util.List;
import java.util.Objects;

public class ReplInterpreter extends Interpreter {

	void interpret(List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		}
		catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	private void execute(Stmt stmt){
		stmt.accept(this);
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		System.out.println(stmt.expression.accept(this));
		return null;
	}


}
