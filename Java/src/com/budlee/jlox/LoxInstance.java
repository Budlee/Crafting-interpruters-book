package com.budlee.jlox;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoxInstance {
	private LoxClass klass;
	private final Map<String, Object> fields = new HashMap<>();

	public LoxInstance(LoxClass klass) {
		this.klass = klass;
	}

	Object get(Token name){
		if (fields.containsKey(name.lexme)){
			return fields.get(name.lexme);
		}
		LoxFunction method = klass.findMethod(name.lexme);
		if(Objects.nonNull(method)){
			return method.bind(this);
		}
		throw new RuntimeError(name,
				String.format("Undefined property '%s'.", name.lexme));
	}

	@Override
	public String toString() {
		return klass.name + " instance";
	}

	void set(Token name, Object value) {
		fields.put(name.lexme, value);
	}
}
