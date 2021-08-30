package com.budlee.jlox;

import java.util.List;

public class LoxFunction implements LoxCallable {

	private final Stmt.Function decleration;
	private final Environment closure;
	private boolean isInitializer;

	public LoxFunction(Stmt.Function decleration, Environment closure, boolean isInitializer) {
		this.decleration = decleration;
		this.closure = closure;
		this.isInitializer = isInitializer;
	}

	@Override
	public int arity() {
		return decleration.params.size();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		final Environment environment = new Environment(closure);
		for (int i = 0; i < decleration.params.size(); i++) {
			environment.define(decleration.params.get(i).lexme, arguments.get(i));
		}
		try {
			interpreter.executeBlock(decleration.body, environment);
		}
		catch (Return returnValue) {
			if(isInitializer){
				return closure.getAt(0, "this");
			}
			return returnValue.value;
		}
		if (isInitializer){
			return closure.getAt(0, "this");
		}
		return null;
	}

	@Override
	public String toString() {
		return String.format("<fn %s>", decleration.name.lexme);
	}

	LoxFunction bind(LoxInstance instance) {
		final Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new LoxFunction(decleration, environment, isInitializer);
	}
}
