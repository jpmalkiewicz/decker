package decker.model;
import decker.util.*;



public final class FunctionCall extends Expression
{
	private ScriptNode[] argument = new Expression[0];
	private final static Value[] DUMMY_ARGS = new Value[0];


	/** called by decker.model.Global.executeExpression() and by FunctionCall.execute()
	*   _function must be either a Function or a Value containing a Function
	*   the enclosing ScriptNodes are already sitting on the stack for data retrieval */
	public final static Value executeFunctionCall (final Object _function, Value[] args, final Structure enclosing_structure) {
		final Function function = (Function) ( (_function instanceof Function) ? _function : ((Value)_function).function() );
		if (args == null)
			args = DUMMY_ARGS;
		// if it is a hard coded function, call it immediately
		if (function.getFunctionBody() == null)
			return StaticScriptFunctions.execute(function.getFunctionID(), args);
		else {
			// calculate the values of the supplied arguments and create the FUNCTION_CALL structure
			args = function.insertDefaultArgumentValues(args);
			final Structure function_data = new Structure(args, function.getArgumentNames());
			// unless the "enclosing_structure" is KEEP_STACK, remove all local stack items from the stack, then put the optional structure the function's variable is stored in, the FUNCTION_CALL and a new LOCAL structure on it
			Structure[] old_stack = null;
			if (enclosing_structure != KEEP_STACK) {
				old_stack = removeLocalStackItems();
				if (enclosing_structure != null)
					addStackItem(enclosing_structure);
			}
			addStackItem(function_data);
			// execute the function
			function.getFunctionBody().execute();
			// restore the original local stack and return the function's return value
			if (enclosing_structure != KEEP_STACK)
				restoreLocalStack(old_stack);
			else
				removeStackItem(function_data);
			return function_data.get("return_value");
		}
	}


// ****************************************************************************************************************************************************
// non-static methods *********************************************************************************************************************************
// ****************************************************************************************************************************************************


	FunctionCall (final String _script_name, final int _script_line, final int _script_column, final Expression[] expression_stack, final int[] expression_stack_top)  {
		super("(", _script_name, _script_line, _script_column, true, expression_stack, expression_stack_top);
	}


	/** solely used by copy() */
	private FunctionCall (final FunctionCall original)  {
		super(original.getScriptName(), original.getScriptLine(), original.getScriptColumn());
		setOperator(FUNCTION_CALL);
		final int count = original.argument.length;
		argument = new ScriptNode[count];
		for (int i = 0; i < count; i++)
			if (original.argument[i] != null)
				argument[i] = original.argument[i].copy();
		// set the first operand to the expression in front of the brackets
		addExpression(original.getFirstOperand().copy());
	}


	void addArgument (final ScriptNode _argument)  {
		argument = (ScriptNode[]) ArrayModifier.addElement(argument, new ScriptNode[argument.length+1], _argument);
	}


	public Expression copy ()  { return new FunctionCall(this); }


	public Value execute ()  {
		Function function = null;
		try {
			// fetch the function we will execute
			function = (Function) getFirstOperand().execute().function();
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			throwException(ex.toString());
		}
		// calculate the values of the supplied arguments
		final Value[] arguments = new Value[argument.length];
		for (int i = 0; i < argument.length; i++)
			if (argument[i] != null) {
				arguments[i] = argument[i].execute();
				// we need to create a new variable so just the value of the original variable from the expression is transferred, and not the variable itself
				if (arguments[i].getEnclosingStructure() != null)
					arguments[i] = new Value().set(arguments[i]);
			}
		// execute the function call and return the resulting Value
		return executeFunctionCall(function, arguments, null);
	}


	/** used to replace strings with their localized versions */
	public void replace (final String original, final boolean starts_with, final String replacement)  {
		for (int i = 0; i < argument.length; i++)
			argument[i].replace(original, starts_with, replacement);
	}


	public String toString()  {
		String ret = getFirstOperand().toString() + " (";
		if (argument.length > 0) {
			ret += (argument[0]!=null) ? argument[0].toString() : " ";
			for (int i = 1; i < argument.length; i++)
				ret += ", "+((argument[i]!=null)?argument[i].toString():"");
		}
		ret += ")";
		return ret;
	}
}