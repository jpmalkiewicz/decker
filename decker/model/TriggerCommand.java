package decker.model;
import java.io.PrintStream;



/** this class implements the script commands "trigger", "if" and "while" */
final class TriggerCommand extends Block
{
	private Expression conditional_expression;
	private boolean remove_trigger_after_execution = true;
	private boolean global_trigger; // it's either global or bound to its ruleset


	TriggerCommand (final String command, final String _script_name, final int _script_line, final int _script_column) {
		super(_script_name, _script_line, _script_column);
		global_trigger = command.equals("global_trigger");
	}


	private TriggerCommand (final TriggerCommand original)  {
		super(original);
		conditional_expression = original.conditional_expression.copy();
	}


	public ScriptNode copy ()  { return new TriggerCommand(this); }


	public Value execute ()  {
		Global.addTrigger(this);
		return null;
	}


	boolean isGlobal ()  { return global_trigger; }


	public void replace (final String original, final boolean starts_with, final String replacement)  {
		super.replace(original, starts_with, replacement);
		conditional_expression.replace(original, starts_with, replacement);
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
		out.println((line_start?indentation:"") + "trigger " + ((conditional_expression!=null)?conditional_expression:"[ CONDITIONAL EXPRESSION NOT DEFINED ]"));
		return super.print(out, indentation, true);
	}


	/** returns true if trigger is to be removed from the list */
	boolean testTrigger ()  {
		if(conditional_expression.execute().equals(true)) {
			// the trigger uses its own local variables set during execution
			final Structure l = new Structure("LOCAL");
			l.add("remove_trigger_after_execution").set(remove_trigger_after_execution);
			addStackItem(l);
			super.execute();
			removeStackItem(l, this);
			remove_trigger_after_execution = !l.get("remove_trigger_after_execution").equals(false);
			return remove_trigger_after_execution;
		}
		return false;
	}


	public void setConditionalExpression (final Expression expression)  { conditional_expression = expression; }
}