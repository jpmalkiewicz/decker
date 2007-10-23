package decker.model;
import java.io.PrintStream;



/** this class implements the script command "with" */
final class WithCommand extends Block
{
	private Expression variable_expression;


	WithCommand (final Expression _variable_expression, final String _script_name, final int _script_line, final int _script_column)  {
		super (_script_name, _script_line, _script_column);
		variable_expression = _variable_expression;
	}


	/** solely used by copy() */
	private WithCommand (final WithCommand original)  {
		super(original);
		if(original.variable_expression != null)
			variable_expression = original.variable_expression.copy();
	}


	public ScriptNode copy ()  { return new WithCommand(this); }


	public Value execute ()  {
		final Value v = variable_expression.execute();
		if (v.typeDirect() == Value.STRUCTURE)
			addStackItem(v.structure());
		else {
			System.err.println("Error in "+getScriptName()+" line "+getScriptLine()+" column "+getScriptColumn()+" :");
			System.err.println(Global.BLOCK_INDENT + "expression does not describe a structure, instead it has the value "+v.toString()+" ("+v.typeNameDirect()+")");
			if (!print(System.err, Global.BLOCK_INDENT, true)) {
				System.err.println();
			}
		}
		final Value ret = super.execute();
		if (v.typeDirect() == Value.STRUCTURE)
			removeStackItem(v.structure());
		return ret;
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
		if (!line_start)
			out.println();
		out.print(indentation + "with ");
		if (variable_expression == null)
			out.println("[expression not defined]");
		else if (!variable_expression.print(out, indentation, false))
			out.println();
		return super.print(out, indentation, true);
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  {
		variable_expression.replace(original, starts_with, replacement);
		super.replace(original, starts_with, replacement);
	}
}