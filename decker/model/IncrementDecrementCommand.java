package decker.model;
import java.io.PrintStream;



final class IncrementDecrementCommand extends ScriptNode
{
	private boolean prefix;
	private int modifier;
	private Expression variable;


	IncrementDecrementCommand (final String _command, final boolean _prefix, final Expression _variable, final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
		modifier = _command.equals("++") ? 1 : -1;
		prefix = _prefix;
		variable = _variable;
		print(System.out, "", true);
		System.out.println("****");
	}


	IncrementDecrementCommand (final IncrementDecrementCommand original)  {
		super(original);
		prefix = original.prefix;
		modifier = original.modifier;
		variable = original.variable.copy();
	}


	ScriptNode copy ()  { return new IncrementDecrementCommand(this); }


	public Value execute ()  {
		Value v = variable.execute();
		if (v.type() != Value.INTEGER && v.type() != Value.REAL)
			throwException("integer or real required but "+v+" ("+v.typeName()+") found");
		final Value ret = new Value();
		if (!prefix)
			ret.set(v);
		if (v.type() == Value.INTEGER)
			v.set(v.integer()+modifier);
		else
			v.set(v.real()+modifier);
		if (prefix)
			ret.set(v);
		return ret;
	}


	public void replace(String original, boolean starts_with, String replacement)  {
		if(variable != null)
			variable.replace(original, starts_with, replacement);
	}


	boolean print (final PrintStream out, final String indentation, boolean line_start)  {
		if (prefix)
			out.print((line_start?indentation:"") + ((modifier>0)?"++":"--"));
		variable.print(out, indentation, line_start);
		if (!prefix)
			out.print((line_start?indentation:"") + ((modifier>0)?"++":"--"));
		return false;
	}
}