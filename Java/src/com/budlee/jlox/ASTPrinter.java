package com.budlee.jlox;

import java.util.Objects;

public class ASTPrinter implements Expr.Visitor<String> {
	String print(Expr expr) {

		return expr.accept(this);
	}

	@Override
	public String visitAssignExpr(Expr.Assign expr) {
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
	public String visitSuperExpr(Expr.Super expr) {
		return null;
	}

	@Override
	public String visitThisExpr(Expr.This expr) {
		return null;
	}

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return parenthensize(expr.operator.lexme, expr.left, expr.right);
	}


	@Override
	public String visitGroupingExpr(Expr.Grouping expr) {
		return parenthensize("group", expr.expression);
	}

	@Override
	public String visitLiteralExpr(Expr.Literal expr) {
		return Objects.isNull(expr.value) ?
				"nil" :
				expr.value.toString();
	}

	@Override
	public String visitVariableExpr(Expr.Variable expr) {
		return parenthensize("variable", expr);
	}

	@Override
	public String visitLogicalExpr(Expr.Logical expr) {
		return parenthensize(expr.operator.lexme, expr.left, expr.right);
	}

	@Override
	public String visitCallExpr(Expr.Call expr) {
		return parenthensize(expr.paren.lexme, expr);
	}

	@Override
	public String visitUnaryExpr(Expr.Unary expr) {
		return parenthensize(expr.operator.lexme, expr.right);
	}

	private String parenthensize(String name, Expr... exprs) {
		final StringBuilder builder = new StringBuilder();

		builder.append("(").append(name);

		for (Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this));
		}
		builder.append(")");

		return builder.toString();
	}

	public static void main(String[] args) {
		final Expr expression = new Expr.Binary(
				new Expr.Unary(
						new Token(TokenType.MINUS, "-", null, 1),
						new Expr.Literal(123)),
				new Token(TokenType.STAR, "*", null, 1),
				new Expr.Grouping(
						new Expr.Literal(45.67)));

		System.out.println(new ASTPrinter().print(expression));
	}
}
