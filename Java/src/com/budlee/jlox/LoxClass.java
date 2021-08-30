package com.budlee.jlox;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LoxClass implements LoxCallable{
	final String name;
	private final LoxClass superclass;
	private final Map<String, LoxFunction> methods;

	public LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
		this.name = name;
		this.superclass = superclass;
		this.methods = methods;
	}

	@Override
	public int arity() {
		final LoxFunction initializer = findMethod("init");
		if(Objects.isNull(initializer)){
			return 0;
		}
		return initializer.arity();
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		final LoxInstance instance = new LoxInstance(this);
		final LoxFunction initializer = findMethod("init");
		if(Objects.nonNull(initializer)){
			initializer.bind(instance).call(interpreter,arguments);
		}
		return instance;
	}

	@Override
	public String toString() {
		return name;
	}

	LoxFunction findMethod(String name) {
		if(methods.containsKey(name)){
			return methods.get(name);
		}
		if(Objects.nonNull(superclass)){
			return superclass.findMethod(name);
		}
		return null;
	}
}
