package decker.model;
import java.io.PrintStream;
import decker.util.ArrayModifier;



final class ArrayDefinition extends Expression
{
	private Expression[] array_definition = new Expression[0];


	ArrayDefinition (final String script_name, final int line, final int column, final Expression[] expression_stack, final int[] expression_stack_top)  {
		super(script_name, line, column);
		setOperator(ARRAY_DEFINITION);
		// the stack is used to parse complex Expressions in scripts. add this ArrayDefinition to its parent Expression if there is one on the stack and put it on the stack
		if (expression_stack != null) {
			if (expression_stack_top[0] > -1)
				expression_stack[expression_stack_top[0]].addExpression(this);
			expression_stack[++expression_stack_top[0]] = this;
		}
	}


	/** solely used by copy() */
	private ArrayDefinition (final ArrayDefinition original)  {
		super(original.getScriptName(), original.getScriptLine(), original.getScriptColumn());
		setOperator(ARRAY_DEFINITION);
		final int count = original.array_definition.length;
		array_definition = new Expression[count];
		for (int i = 0; i < count; i++)
			array_definition[i] = original.array_definition[i].copy();
	}


	/** this appends the expression that defines the next array entry to this ARRAY definition */
	public void addExpression (final Expression e)  {
		array_definition = (Expression[]) ArrayModifier.addElement(array_definition, new Expression[array_definition.length+1], e);
	}


	public Expression copy ()  { return new ArrayDefinition(this); }


	public Value execute ()  {
		// creates a new array and returns it
		final int count = array_definition.length;
		final Value[] array = new Value[count];
		for (int i = 0; i < count; i++)
			array[i] = new Value().set(array_definition[i].execute());
		return new Value().set(new ArrayWrapper(array));
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start, final int depth)  {
		final int count = array_definition.length;
		if (array_definition.length == 0) {
			out.print((line_start?indentation:"") + "ARRAY");
			return false;
		}
		else {
			out.println((line_start?indentation:"") + "ARRAY");
			final String ind = indentation + Global.BLOCK_INDENT;
			if (depth <= 0)
				System.out.println(ind+"...");
			else
				for (int i = 0; i < count; i++)
					if (!array_definition[i].print(out, ind, true, depth-1))
						out.println();
			return true;
		}
	}


	/** used to replace strings with their localized versions */
	public void replace (final String original, final boolean starts_with, final String replacement)  {
		final int count = array_definition.length;
		for (int i = 0; i < count; i++)
			array_definition[i].replace(original, starts_with, replacement);
	}


	int size ()  {
		return array_definition.length;
	}
}