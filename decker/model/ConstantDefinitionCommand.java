package decker.model;
import java.io.PrintStream;



/** with this command you define constants (one word strings which can be used without quotes in scripts) */
final class ConstantDefinitionCommand extends ScriptNode
{
	private String[] constant = new String[0]; // the list of constants defined by this command


	ConstantDefinitionCommand (final String[] _constants, final String _script_name, final int _script_line, final int _script_column)  {
		super (_script_name, _script_line, _script_column);
		constant = _constants;
	}


	/** solely used by copy() */
	private ConstantDefinitionCommand (final ConstantDefinitionCommand original)  {
		super(original);
		constant = new String[original.constant.length];
		System.arraycopy(original.constant, 0, constant, 0, constant.length);
	}


	public ScriptNode copy()  { return new ConstantDefinitionCommand(this); }


	public Value execute ()  {
		final Structure s = stack[RULESET_STACK_SLOT].get("CONSTANTS").structure();
		final String[] c = constant;
		for (int i = c.length; --i >= 0; )
			s.add(c[i]).setConstant(c[i]);
		return null;
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start, final int depth)  {
		if (!line_start)
			out.println();
		if (constant.length == 1)
			out.println(indentation + "constant " + constant[0]);
		else {
			out.println(indentation + "constant");
			final String ind = indentation + Global.BLOCK_INDENT;
			if (depth <= 0)
				System.out.println(ind+"[...]");
			else
				for (int i = 0; i < constant.length; i++)
					out.println(ind + constant[i]);
		}
		return true;
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  {}
}