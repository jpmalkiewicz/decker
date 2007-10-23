package decker.model;
import java.io.PrintStream;



/** this class implements the script command "break" */
final class BreakCommand extends ScriptNode
{
	BreakCommand (final String _script_name, final int _script_line, final int _script_column)  {
		super (_script_name, _script_line, _script_column);
	}


	/** solely used by copy() */
	private BreakCommand (final BreakCommand original)  {
		super(original);
	}


	public ScriptNode copy()  { return new BreakCommand(this); }


	public Value execute ()  { return BREAK_VALUE; }


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
		if (!line_start)
			out.print("break");
		else
			out.println(indentation + "break");
		return line_start;
	}
}