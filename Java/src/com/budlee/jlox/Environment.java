package com.budlee.jlox;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Environment {
	final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	public Environment() {
		this.enclosing = null;
	}

	public Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	Object get(Token name) {
		if (values.containsKey(name.lexme)) {
			final Object value = values.get(name.lexme);
			if (Objects.isNull(value)) {
				throw new RuntimeError(name, String.format("Variable '%s' has not been assigned value before use.", name.lexme));
			}
			return value;
		}
		if (enclosing != null) {
			return enclosing.get(name);
		}
		throw new RuntimeError(name, String.format("Undefined variable '%s'.", name.lexme));
	}

	void define(String name, Object value) {
		values.put(name, value);
	}

	private Environment ancestor(int distance) {
		Environment environment = this;
		for (int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}
		return environment;
	}

	Object getAt(Integer distance, String name) {
		return ancestor(distance).values.get(name);
	}

	void assign(Token name, Object value) {
		if (values.containsKey(name.lexme)) {
			values.put(name.lexme, value);
			return;
		}
		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}
		throw new RuntimeError(name, String.format("Undefined variable '%s'.", name.lexme));
	}

	void assignAt(Integer distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexme, value);
	}
}
