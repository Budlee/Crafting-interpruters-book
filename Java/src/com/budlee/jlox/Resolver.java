package com.budlee.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	private final Interpreter interpreter;
	private Stack<Map<String, Boolean>> scopes = new Stack<>();
	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;

	private enum FunctionType {
		NONE,
		METHOD,
		FUNCTION,
		INITIALIZER
	}

	private enum ClassType {
		NONE,
		CLASS,
		SUBCLASS
	}

	public Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	@Override
	public Void visitAssignExpr(Expr.Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		declare(stmt.name);
		define(stmt.name);
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitVariableExpr(Expr.Variable expr) {
		if (!scopes.isEmpty() &&
				scopes.peek().get(expr.name.lexme) == Boolean.FALSE) {
			Lox.error(expr.name,
					"Can't read local variab;e in its own initializer");
		}
		resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		declare(stmt.name);
		if (Objects.nonNull(stmt.initializer)) {
			resolve(stmt.initializer);
		}
		define(stmt.name);
		return null;
	}

	private void declare(Token name) {
		if (scopes.isEmpty()) {
			return;
		}
		final Map<String, Boolean> scope = scopes.peek();
		if (scope.containsKey(name.lexme)) {
			Lox.error(name, "Already a variable with this name in this scope.");
		}
		scope.put(name.lexme, false);
	}

	private void define(Token name) {
		if (scopes.isEmpty()) {
			return;
		}
		scopes.peek().put(name.lexme, true);
	}

	void resolve(List<Stmt> statements) {
		statements.forEach(this::resolve);
	}

	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}

	private void resolve(Expr expr) {
		expr.accept(this);
	}

	private void resolveLocal(Expr expr, Token name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}
	}

	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;
		beginScope();
		function.params.forEach(param -> {
			declare(param);
			define(param);
		});
		resolve(function.body);
		endScope();
		currentFunction = enclosingFunction;
	}

	private void beginScope() {
		scopes.push(new HashMap<>());
	}

	private void endScope() {
		scopes.pop();
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		final ClassType enclosingClass = this.currentClass;
		currentClass = ClassType.CLASS;
		declare(stmt.name);
		define(stmt.name);
		if (Objects.nonNull(stmt.superclass) &&
				stmt.name.lexme.equals(stmt.superclass.name.lexme)) {
			Lox.error(stmt.superclass.name,
					"A class can't inherit from itself.");
		}
		if (Objects.nonNull(stmt.superclass)) {
			currentClass = ClassType.SUBCLASS;
			resolve(stmt.superclass);
		}
		if (Objects.nonNull(stmt.superclass)) {
			beginScope();
			scopes.peek().put("super", true);
		}
		beginScope();
		scopes.peek().put("this", true);
		stmt.methods.forEach(method -> {
					FunctionType decleration = FunctionType.METHOD;
					if (method.name.lexme.equals("init")) {
						decleration = FunctionType.INITIALIZER;
					}
					resolveFunction(method, decleration);
				}
		);
		endScope();
		if (Objects.nonNull(stmt.superclass)) {
			endScope();
		}
		this.currentClass = enclosingClass;
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (Objects.nonNull(stmt.elseBranch)) {
			resolve(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Lox.error(stmt.keyword, "Can't return from top-level code.");
		}
		if (Objects.nonNull(stmt.value)) {
			if (currentFunction == FunctionType.INITIALIZER) {
				Lox.error(stmt.keyword, "Can't return a value from an initializer.");
			}
			resolve(stmt.value);
		}
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Expr.Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Expr.Call expr) {
		resolve(expr.callee);
		expr.arguments.forEach(this::resolve);
		return null;
	}

	@Override
	public Void visitGetExpr(Expr.Get expr) {
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitGroupingExpr(Expr.Grouping expr) {
		resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Expr.Literal expr) {
		return null;
	}

	@Override
	public Void visitLogicalExpr(Expr.Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	@Override
	public Void visitSetExpr(Expr.Set expr) {
		resolve(expr.value);
		resolve(expr.object);
		return null;
	}

	@Override
	public Void visitSuperExpr(Expr.Super expr) {
		if (currentClass == ClassType.NONE){
			Lox.error(expr.keyword, "Can't use 'super' outside of a class");
		}else if(currentClass != ClassType.SUBCLASS){
			Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitThisExpr(Expr.This expr) {
		if (currentClass == ClassType.NONE) {
			Lox.error(expr.keyword,
					"Can't use 'this' outside of class.");
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Expr.Unary expr) {
		resolve(expr.right);
		return null;
	}
}