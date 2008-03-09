package decker.model;
import java.io.PrintStream;



/** this class implements the script command "display" */
final class PrintCommand extends ScriptNode
{
	private ScriptNode displayed_expression;


	PrintCommand (final String _script_name, final int _script_line, final int _script_column)  {
		super (_script_name, _script_line, _script_column);
	}


	/** solely used by copy() */
	private PrintCommand (final PrintCommand original)  {
		super(original);
		if(original.displayed_expression != null)
			displayed_expression = original.displayed_expression.copy();
	}


	public ScriptNode copy()  { return new PrintCommand(this); }


	public Value execute ()  {
System.out.println("PrintCommand : "+displayed_expression.execute().toString());
Global.displayTickerMessage(displayed_expression.execute().toString()); return null; }


	boolean print (final PrintStream out, final String indentation, final boolean line_start, final int depth)  {
		if (!line_start)
			out.println();
		out.print(indentation + "print ");
		if (displayed_expression == null)
			out.println("[displayed expression not defined]");
		else if (!displayed_expression.print(out, indentation, false, depth))
			out.println();
		return true;
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  { displayed_expression.replace(original, starts_with, replacement); }


	public void setDisplayedExpression (final ScriptNode expression)  { displayed_expression = expression; }
}