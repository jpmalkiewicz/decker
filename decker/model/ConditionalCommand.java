package decker.model;
import java.io.PrintStream;



/** this class implements the script commands "if" and "while" */
final class ConditionalCommand extends Block
{
	final static int IF = 0, WHILE = 1;

	private int type;
	private Expression conditional_expression;
	private ScriptNode else_branch;


	ConditionalCommand (final String _type, final String _script_name, final int _script_line, final int _script_column) {
		super(_script_name, _script_line, _script_column);
		// determine the type of the conditional command
		if (_type.equals("while"))
			type = WHILE;
		else if (_type.equals("if") || _type.equals("elseif"))
			type = IF;
		else
			throwException(type+" is not a valid conditional command");
	}


	ConditionalCommand (final ConditionalCommand original)  {
		super(original);
		type = original.type;
		conditional_expression = original.conditional_expression.copy();
		if (original.else_branch != null)
			if (original.else_branch.getClass() == Block.class)
				else_branch = new Block((Block)(original.else_branch));
			else
				else_branch = original.else_branch.copy();
	}


	public void addElseBranch (final ScriptNode _else_branch) {
		if(type != IF)
			throwException("method only implemented for the if command");
		else_branch = _else_branch;
	}


	public ScriptNode copy ()  {
		return new ConditionalCommand(this);
	}


	public Value execute ()  {
		if (type == IF) {
			if (conditional_expression.execute().equals(true)) {
				if (super.execute() == BREAK_VALUE) {
					return BREAK_VALUE;
				}
			}
			else if (else_branch != null) {
				if (else_branch.execute() == BREAK_VALUE) {
					return BREAK_VALUE;
				}
			}
		}
		else { // type == WHILE
			while (conditional_expression.execute().equals(true)) {
				// execute the loop's body. if a "break" command has been reached, stop the loop
				if (super.execute() == BREAK_VALUE) {
					return null;
				}
			}
		}
		return null;
	}


	int getType ()  { return type; }


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
		out.println((line_start?indentation:"") + ((type==IF)?"if":"while") + " " + ((conditional_expression!=null)?conditional_expression:"[ CONDITIONAL EXPRESSION NOT DEFINED ]"));
		if (else_branch == null)
			return super.print(out, indentation, true);
		else {
			if (!super.print(out, indentation, true))
				out.println();
			out.println(indentation+"else");
			return else_branch.print(out, indentation, true);
		}
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  {
		super.replace(original, starts_with, replacement);
		conditional_expression.replace(original, starts_with, replacement);
	}


	public void setConditionalExpression (final Expression expression)  { conditional_expression = expression; }
}