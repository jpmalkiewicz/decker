package decker.model;
import java.io.PrintStream;



/** this class implements the script command "for" */
final class ForLoopCommand extends Block
{
	private Expression variable, initial_value, condition, final_value, step;
	private ScriptNode increment;
	private boolean java_style; // true : for (i=0;i<=5;i++), false : for i = 0 to 5


	/** used for the syntax : for i = 0 to 5 step 2 */
	ForLoopCommand (final Expression _variable, final Expression _initial_value, final Expression _final_value, final Expression _step, final String _script_name, final int _script_line, final int _script_column) {
		super(_script_name, _script_line, _script_column);
		java_style = false;
	}


	/** used for the syntax : for (i=0; i<=5; i=i+2) */
	ForLoopCommand (final Expression _variable, final Expression _initial_value, final Expression _condition, final ScriptNode _increment, final String _script_name, final int _script_line, final int _script_column) {
		super(_script_name, _script_line, _script_column);
		java_style = true;
	}


	ForLoopCommand (final ForLoopCommand original)  {
		super(original);
	}


	public ScriptNode copy ()  {
		return new ForLoopCommand(this);
	}


	public Value execute ()  {
		return null;
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
/*		out.println((line_start?indentation:"") + ((type==IF)?"if":"while") + " " + ((conditional_expression!=null)?conditional_expression:"[ CONDITIONAL EXPRESSION NOT DEFINED ]"));
		if (else_branch == null)
			return super.print(out, indentation, true);
		else {
			if (!super.print(out, indentation, true))
				out.println();
			out.println(indentation+"else");
			return else_branch.print(out, indentation, true);
		}
*/
return line_start;
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  {
//		super.replace(original, starts_with, replacement);
//		conditional_expression.replace(original, starts_with, replacement);
	}
}