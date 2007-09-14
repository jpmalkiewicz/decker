package decker.model;
import decker.util.*;



public final class FunctionCall extends Expression
{
	private ScriptNode[] argument = new Expression[0];


	/** called by decker.model.Global.executeExpression() and by FunctionCall.execute()
	*   _function must be either a Function or a Value containing a Function
	*   the enclosing ScriptNodes are already sitting on the stack for data retrieval */
	public final static Value executeFunctionCall (final Object _function, final Value[] args, final Structure enclosing_structure) {
		final Function function = (Function) ( (_function instanceof Function) ? _function : ((Value)_function).function() );
		// calculate the values of the supplied arguments and create the FUNCTION_CALL structure
		final Structure arguments = new Structure("ARRAY");
		if (args != null) {
			// create the list of arguments
			for (int i = 0; i < args.length; i++)
				if (args[i] != null)
					arguments.add("").set(args[i]);
				else
					arguments.add("");
		}
		function.insertDefaultArgumentValues(arguments);
		final Structure function_data = new Structure(arguments, function.getArgumentNames());
		// unless the "enclosing_structure" is KEEP_STACK, remove all local stack items from the stack, then put the optional structure the function's variable is stored in, the FUNCTION_CALL and a new LOCAL structure on it
		Structure[] old_stack = null;
		if (enclosing_structure != KEEP_STACK && function.getFunctionBody() != null) {
			old_stack = removeLocalStackItems();
			if (enclosing_structure != null)
				addStackItem(enclosing_structure);
		}
		addStackItem(function_data);

		// execute the function
		Value v;
		if (function.getFunctionBody() != null)
			function.getFunctionBody().execute();
		else
			StaticScriptFunctions.execute(function.getFunctionID(), function_data, args);
		// restore the original local stack and return the function's return value
		if (enclosing_structure != KEEP_STACK && function.getFunctionBody() != null)
			restoreLocalStack(old_stack);
		else
			removeStackItem(function_data);
		return function_data.get("return_value");
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
			throwException(ex.toString());
		}
		// calculate the values of the supplied arguments
		final Value[] arguments = new Value[argument.length];
		for (int i = 0; i < argument.length; i++)
			if (argument[i] != null)
				arguments[i] = argument[i].execute();
		// execute the function call nd return the resulting Value
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
			ret += argument[0].toString();
			for (int i = 1; i < argument.length; i++)
				ret += ", "+argument[i].toString();
		}
		ret += ")";
		return ret;
	}
}