import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class GenerateAST {
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
		if (args.length != 1) {
			System.err.println("Usage: generate_ast <output directory>");
			System.exit(64);
		}
		String outputDir = args[0];
		defineAst(outputDir, "Expr", Arrays.asList(
						"Assign : 	Token name, Expr value",
						"Binary : 	Expr left, Token operator, Expr right",
						"Call : 	Expr callee, Token paren, List<Expr> arguments",
						"Get : 		Expr object, Token name",
						"Grouping : Expr expression",
						"Literal : 	Object value",
						"Logical : 	Expr left, Token operator, Expr right",
						"Set : 		Expr object, Token name, Expr value",
						"Super : 	Token keyword, Token method",
						"This : 	Token keyword",
						"Variable : Token name",
						"Unary : 	Token operator, Expr right"
				)
		);
		defineAst(outputDir, "Stmt", Arrays.asList(
						"Block : 		List<Stmt> statements",
						"Class : 		Token name, Expr.Variable superclass, List<Stmt.Function> methods",
						"Expression : 	Expr expression",
						"Function : 	Token name, List<Token> params, List<Stmt> body",
						"If : 			Expr condition, Stmt thenBranch, Stmt elseBranch",
						"Print : 		Expr expression",
						"Return : 		Token keyword, Expr value",
						"Var : 			Token name, Expr initializer",
						"While : 		Expr condition, Stmt body"
				)
		);
	}

	private static void defineAst(String outputDir, String baseName, List<String> types) throws FileNotFoundException, UnsupportedEncodingException {
		var path = String.format("%s/%s.java", outputDir, baseName);
		final PrintWriter writer = new PrintWriter(path, "UTF-8");
		writer.println("package com.budlee.jlox;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println();
		writer.println(String.format("abstract class %s {", baseName));
		defineVisitor(writer, baseName, types);

		for (String type : types) {
			final String className = type.split(":")[0].trim();
			final String fields = type.split(":")[1].trim();
			defineType(writer, baseName, className, fields);
		}

		writer.println();
		writer.println("    abstract <R> R accept(Visitor<R> visitor);");

		writer.println("}");
		writer.close();
	}

	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("    interface Visitor<R> {");

		for (String type : types

		) {
			final String typename = type.split(":")[0].trim();
			writer.println(String.format("    R visit%s%s(%s %s);", typename, baseName, typename, baseName.toLowerCase()));
		}
		writer.println("    }");
	}

	private static void defineType(PrintWriter writer, String baseName, String className, String fieldsList) {
		writer.println(String.format("    static class %s extends %s {",
				className, baseName
		));

		writer.println(String.format("    %s (%s) {", className, fieldsList));
		String[] fields = fieldsList.split(", ");
		for (String field : fields) {
			final String name = field.split(" ")[1];
			writer.println(String.format("        this.%s = %s;", name, name));
		}
		writer.println("    }");


		//Visitor pattern
		writer.println();
		writer.println("    @Override");
		writer.println("    <R> R accept(Visitor<R> visitor) {");
		writer.println(String.format("        return visitor.visit%s%s(this);", className, baseName));
		writer.println("    }");
		writer.println("");


		//Fields
		writer.println();

		for (String field : fields) {
			writer.println(String.format("    final %s;", field));
		}
		writer.println("    }");

	}
}
