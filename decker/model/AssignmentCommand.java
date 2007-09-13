package decker.model;
import java.io.PrintStream;



final class AssignmentCommand extends ScriptNode
{
	private Expression variable;
	private ScriptNode value_definition;


	AssignmentCommand (final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
	}


	AssignmentCommand (final AssignmentCommand original)  {
		super(original);
		variable = original.variable.copy();
		value_definition = original.value_definition.copy();
	}


	ScriptNode copy ()  { return new AssignmentCommand(this); }


	public Value execute ()  {
		// fetch the variable
		Value ret = null, v;
		// if there exists a LOCAL variable of that name, use that
		if (variable.getOperator() == Expression.VARIABLE) {
			for (int i = stack_size; --i >= 0; ) {
				if (stack[i].get("structure_type").equals("LOCAL")) {
					ret = stack[i].get(variable.toString());
				}
			}
		}
		// if the topmost structure on the stack is expandable and not of type LOCAL (a sign that we're inside a function body), add the variable to it (if it doesn't exist yet)
		if (ret == null && variable.getOperator() == Expression.VARIABLE && stack[stack_size-1].canHoldCustomVariables() && !stack[stack_size-1].get("structure_type").equals("LOCAL")) {
			ret = stack[stack_size-1].get(variable.toString());
			if (ret == null)
				ret = stack[stack_size-1].add(variable.toString());
		}
		else
			ret = variable.execute();
		// if the variable doesn't exist yet, its enclosing structure is null
		final Structure k = ret.getEnclosingStructure();
		if (k == null) {
			// if it's just a variable name, add it to the innermost Structure on the stack that can hold custom variables
			if (variable.getOperator() == Expression.VARIABLE) {
				final String varname = variable.toString();
				testVariableName(varname);
				boolean var_created = false;
				for (int i = stack_size; --i >= 0; )
					if (stack[i].canHoldCustomVariables()) {
						ret = stack[i].add(varname);
						var_created = true;
						break;
					}
				if (!var_created)
					throwException("failed to create variable. there is no structure that can hold custom variables on the stack atm");
			}
			else if (variable.getOperator() == Expression.MEMBER) {
				// the variable gets added to some structure, find the structure
				if (variable.getSecondOperand().getOperator() != Expression.VARIABLE)
					throwException("failed to create variable. variable name expected but operator type "+variable.getSecondOperand().getOperator()+" found:\n"+variable.getSecondOperand().toString());
				final String varname = variable.getSecondOperand().toString();
				testVariableName(varname);
				// fetch the structure manually if the first operand is a variable name. otherwise execute the expression in the first operand to obtain the Structure
				Value structure = null;
				if (variable.getFirstOperand().getOperator() != Expression.VARIABLE)
					structure = variable.getFirstOperand().execute();
				else {
					final String structurename = variable.getFirstOperand().toString();
					for (int i = stack_size; --i >= 0; ) {
						if (stack[i].get("structure_type").equals(structurename)) {
							structure = new Value().set(stack[i]);
							break;
						}
						if ((structure=stack[i].get(structurename)) != null)
							break;
					}
				}
				if (structure == null)
					throwException("failed to create variable. structure "+variable.getFirstOperand().toString()+" not found");
				if (structure.type() != Value.STRUCTURE)
					throwException("failed to create variable. "+variable.getFirstOperand().toString()+" gives a "+structure.typeName()+" instead of a structure");
				if (!structure.structure().canHoldCustomVariables())
					throwException("failed to create variable. the structure "+variable.getFirstOperand().toString()+" of type "+structure.get("structure_type")+" cannot hold custom variables");
				ret = structure.structure().add(varname);
			}
			else if (variable.getOperator() == Expression.ARRAY_INDEX) {
				// fetch the array
				final Value array = variable.getFirstOperand().execute();
				if (array.type() != Value.STRUCTURE)
					throwException("failed to create variable. "+variable.getFirstOperand().toString()+" gives a "+array.typeName()+" instead of a structure");
				if (!array.get("structure_type").equals("ARRAY"))
					throwException("failed to create variable. "+variable.getFirstOperand().toString()+" gives a "+array.get("structure_type")+" instead of an ARRAY");
				// make sure it's a valid array index
				final String varname = variable.getSecondOperand().execute().toString();
				if (varname.length() > 0 && !varname.equals(array.get("size").toString()))
					throwException("failed to create variable. Array index must be either empty or the current array size ("+array.get("size").toString()+"), not "+varname);
				ret = array.structure().add(varname);
			}
			else
				throwException("failed to create variable. unable to handle expressions whose top level operator is "+variable.getOperatorElement().toString());
		}
		if (value_definition instanceof Function)
			return ret.set((Function)value_definition);
		else if (value_definition instanceof Expression) {
			if (((Expression)value_definition).getOperator() == Expression.FETCH_VALUE)
				return ret.setFetchValue((Expression)value_definition);
			if (((Expression)value_definition).getOperator() == Expression.RAW_VALUE)
				return ret.setDirectly(value_definition.execute(), false);
		}
		return ret.set(value_definition.execute());
	}


	public void replace(String original, boolean starts_with, String replacement)  {
		if(variable != null)
			variable.replace(original, starts_with, replacement);
		if(value_definition != null)
			value_definition.replace(original, starts_with, replacement);
	}


	public void setValueExpression (final ScriptNode _value)  { value_definition = _value; }


	public void setVariableExpression (final Expression _variable)  { variable = _variable; }


	boolean print (final PrintStream out, final String indentation, boolean line_start)  {
		line_start = variable.print(out, indentation, line_start);
		out.print((line_start?indentation:" ") + "= ");
		return print(out, indentation, line_start, value_definition);
	}
}