package com.budlee.jlox;

import java.util.Objects;

public class ASTPrinterRPN implements Expr.Visitor<String> {
	String print(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitAssignExpr(Expr.Assign expr) {
		return null;
	}

	@Override
	public String visitThisExpr(Expr.This expr) {
		return null;
	}

	@Override
	public String visitSuperExpr(Expr.Super expr) {
		return null;
	}

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return parenthensize(expr.operator.lexme, expr.left, expr.right);
	}


	@Override
	public String visitGroupingExpr(Expr.Grouping expr) {
		return parenthensize("", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Expr.Literal expr) {
		return Objects.isNull(expr.value) ?
				"nil" :
				expr.value.toString();
	}

	@Override
	public String visitVariableExpr(Expr.Variable expr) {
		return null;
	}

	@Override
	public String visitLogicalExpr(Expr.Logical expr) {
		return null;
	}

	@Override
	public String visitCallExpr(Expr.Call expr) {
		return null;
	}

	@Override
	public String visitGetExpr(Expr.Get expr) {
		return null;
	}

	@Override
	public String visitSetExpr(Expr.Set expr) {
		return null;
	}

	@Override
	public String visitUnaryExpr(Expr.Unary expr) {
		return parenthensize(expr.operator.lexme, expr.right);
	}

	private String parenthensize(String name, Expr... exprs) {
		final StringBuilder builder = new StringBuilder();

		for (Expr expr : exprs) {
			builder.append(expr.accept(this));
		}
		builder.append(" ")
				.append(name);

		return builder.toString();
	}

	public static void main(String[] args) {
		final Expr.Binary plus12 = new Expr.Binary(
				new Expr.Literal(1),
				new Token(TokenType.PLUS, "+", null, 1),
				new Expr.Literal(2)
		);
		final Expr.Binary minus43 = new Expr.Binary(
				new Expr.Literal(4),
				new Token(TokenType.MINUS, "-", null, 1),
				new Expr.Literal(3)
		);
		final Expr expression = new Expr.Binary(
				new Expr.Grouping(plus12),
				new Token(TokenType.STAR, "*", null, 1),
				new Expr.Grouping(minus43)
		);

		System.out.println(new ASTPrinterRPN().print(expression));
	}
}
