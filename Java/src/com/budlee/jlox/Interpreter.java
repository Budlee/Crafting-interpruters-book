package com.budlee.jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
	private Environment environment = globals;
	private final Map<Expr, Integer> locals = new HashMap<>();

	Interpreter() {
		globals.define("clock", new LoxCallable() {
			@Override
			public int arity() {
				return 0;
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				return (double) System.currentTimeMillis() / 1000.0;
			}

			@Override
			public String toString() {
				return "<native fn>";
			}
		});
	}

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

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		final Object left = evaluate(expr.left);
		final Object right = evaluate(expr.right);
		switch (expr.operator.tokenType) {
		case GREATER:
			checkNumberOperands(expr.operator, left, right);
			return (double) left > (double) right;
		case GREATER_EQUAL:
			checkNumberOperands(expr.operator, left, right);
			return (double) left >= (double) right;
		case LESS:
			checkNumberOperands(expr.operator, left, right);
			return (double) left < (double) right;
		case LESS_EQUAL:
			checkNumberOperands(expr.operator, left, right);
			return (double) left <= (double) right;
		case BANG_EQUAL:
			return !isEqual(left, right);
		case EQUAL_EQUAL:
			return isEqual(left, right);
		case MINUS:
			checkNumberOperands(expr.operator, left, right);
			return (double) left - (double) right;
		case SLASH:
			checkNumberOperands(expr.operator, left, right);
			if ((double) right == 0.0) {
				throw new RuntimeError(expr.operator, "Divisor can not be zero.");
			}
			return (double) left / (double) right;
		case STAR:
			checkNumberOperands(expr.operator, left, right);
			return (double) left * (double) right;
		case PLUS:
			if (left instanceof Double &&
					right instanceof Double) {
				return (double) left + (double) right;
			}
			if (left instanceof String &&
					right instanceof String) {
				return (String) left + (String) right;
			}
			throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");
		}
		return null;
	}

	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		final Object right = evaluate(expr.right);

		switch (expr.operator.tokenType) {
		case MINUS:
			checkNumberOperand(expr.operator, right);
			return -(double) right;
		case BANG:
			return !isTruthy(right);
		}
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		final Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		environment.define(stmt.name.lexme, value);
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.body);
		}
		return null;
	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return lookUpVariable(expr.name, expr);
	}

	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (Objects.nonNull(distance)) {
			return environment.getAt(distance, name.lexme);
		}
		return globals.get(name);
	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);

		Integer distance = locals.get(expr);
		if (Objects.nonNull(distance)) {
			environment.assignAt(distance, expr.name, value);
		}
		else {
			globals.assign(expr.name, value);
		}
		return value;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		Object superclass = null;
		if(Objects.nonNull(stmt.superclass)){
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof LoxClass)){
				throw new RuntimeError(stmt.superclass.name,
						"Superclass must be of type class.");
			}
		}
		environment.define(stmt.name.lexme, null);
		if(Objects.nonNull(stmt.superclass)){
			environment = new Environment(environment);
			environment.define("super", superclass);
		}
		final Map<String, LoxFunction> methods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			final LoxFunction function = new LoxFunction(method, environment, method.name.lexme.equals("init"));
			methods.put(method.name.lexme, function);
		}

		LoxClass klass = new LoxClass(stmt.name.lexme,(LoxClass) superclass, methods);
		if(Objects.nonNull(superclass)){
			environment = environment.enclosing;
		}
		environment.assign(stmt.name, klass);
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		}
		else if (Objects.nonNull(stmt.elseBranch)) {
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);
		if (expr.operator.tokenType == TokenType.OR) {
			if (isTruthy(left)) {
				return left;
			}
		}
		else {
			if (!isTruthy(left)) {
				return left;
			}
		}
		return evaluate(expr.right);
	}

	@Override
	public Object visitSetExpr(Expr.Set expr) {
		final Object object = evaluate(expr.object);
		if(!(object instanceof LoxInstance)){
			throw new RuntimeError(expr.name,
					"Only instances have fields.");
		}
		final Object value = evaluate(expr.value);
		((LoxInstance) object).set(expr.name, value);
		return value;
	}

	@Override
	public Object visitSuperExpr(Expr.Super expr) {
		final Integer distance = locals.get(expr);
		LoxClass superclass = (LoxClass) environment.getAt(distance, "super");
		LoxInstance object = (LoxInstance) environment.getAt(distance -1, "this");
		final LoxFunction method = superclass.findMethod(expr.method.lexme);

		if(Objects.isNull(method)){
			throw new RuntimeError(expr.method,
					String.format("Undefined property '%s'.", expr.method.lexme));
		}

		return method.bind(object);
	}

	@Override
	public Object visitThisExpr(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		final Object callee = evaluate(expr.callee);
		final List<Object> arguments = expr.arguments.stream()
				.map(this::evaluate)
				.collect(Collectors.toList());
		if (!(callee instanceof LoxCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");
		}

		LoxCallable function = (LoxCallable) callee;
		if (arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren,
					String.format("Expected %s arguments but got %s.", function.arity(), arguments.size()));
		}

		return function.call(this, arguments);
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		final Object object = evaluate(expr.object);
		if(object instanceof LoxInstance){
			return ((LoxInstance) object).get(expr.name);
		}
		throw new RuntimeError(expr.name,
				"Only instances have properties.");
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		final LoxFunction loxFunction = new LoxFunction(stmt, environment, false);
		environment.define(stmt.name.lexme, loxFunction);
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) {
			value = evaluate(stmt.value);
		}
		throw new Return(value);
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double &&
				right instanceof Double) {
			return;
		}
		throw new RuntimeError(operator, "Operands must be a number.");
	}

	private boolean isTruthy(Object object) {
		if (Objects.isNull(object)) {
			return false;
		}
		if (object instanceof Boolean) {
			return (boolean) object;
		}
		return true;
	}

	private boolean isEqual(Object a, Object b) {
		if (Objects.isNull(a) && Objects.isNull(b)) {
			return true;
		}
		if (Objects.isNull(a)) {
			return false;
		}
		return a.equals(b);
	}

	private Object evaluate(Expr expression) {
		return expression.accept(this);
	}

	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}

	void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;
			for (Stmt statement : statements) {
				execute(statement);
			}
		}
		finally {
			this.environment = previous;
		}
	}

	private String stringify(Object object) {
		if (Objects.isNull(object)) {
			return "nil";
		}
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}
		return object.toString();
	}
}
