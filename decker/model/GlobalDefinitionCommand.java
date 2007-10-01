package decker.model;
import java.io.PrintStream;



/** with this command you define constants (one word strings which can be used without quotes in scripts) */
final class GlobalDefinitionCommand extends ScriptNode
{
	private AssignmentCommand[] globals = new AssignmentCommand[0]; // the list of constants defined by this command


	GlobalDefinitionCommand (final AssignmentCommand[] _globals, final String _script_name, final int _script_line, final int _script_column)  {
		super (_script_name, _script_line, _script_column);
		globals = _globals;
	}


	/** solely used by copy() */
	private GlobalDefinitionCommand (final GlobalDefinitionCommand original)  {
		super(original);
		globals = new AssignmentCommand[original.globals.length];
		for (int i = globals.length; --i >= 0; )
			globals[i] = new AssignmentCommand(original.globals[i]);
	}


	public ScriptNode copy()  { return new GlobalDefinitionCommand(this); }


	public Value execute ()  {
		final Structure s = stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES").structure();
		addStackItem(s);
		final AssignmentCommand[] g = globals;
		for (int i = 0; i < g.length; i++) {
			g[i].execute();
		}
		removeStackItem(s);
		return null;
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
		if (!line_start)
			out.println();
		out.println(indentation + "global");
		final String ind = indentation + Global.BLOCK_INDENT;
		for (int i = 0; i < globals.length; i++)
			if (!globals[i].print(out, ind, true))
				out.println();
		return true;
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  {
		for (int i = 0; i < globals.length; i++)
			globals[i].replace(original, starts_with, replacement);
	}
}