package decker.model;
import java.io.PrintStream;
import decker.util.*;



final class AssignmentCommand extends ScriptNode
{
	private Expression variable;
	private ScriptNode value_definition;
	private boolean its_a_global;


	AssignmentCommand (final boolean _its_a_global, final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
		its_a_global = _its_a_global;
	}


	AssignmentCommand (final AssignmentCommand original)  {
		super(original);
		variable = original.variable.copy();
		value_definition = original.value_definition.copy();
		its_a_global = original.its_a_global;
	}


	ScriptNode copy ()  { return new AssignmentCommand(this); }


	static Value fetchOrCreateVariable (final Expression e, final boolean create_in_LOCAL, final ScriptNode caller) {
		Value ret = null;
		final int voperator = e.getOperator();
		// if there exists a LOCAL variable of that name, use that
		if (voperator == Expression.VARIABLE) {
			for (int i = stack_size; --i >= 0; ) {
				if (stack[i].get("structure_type").equals("LOCAL")) {
					ret = stack[i].get(e.toString());
					if (ret != null) {
						return ret;
					}
				}
			}
		}
		// if the topmost structure on the stack is expandable and not of type LOCAL, add the variable to it (if it doesn't exist yet)
		if (!create_in_LOCAL && voperator == Expression.VARIABLE && stack[stack_size-1].canHoldCustomVariables() && !stack[stack_size-1].get("structure_type").equals("LOCAL")) {
			ret = stack[stack_size-1].get(e.toString());
			if (ret == null)
				ret = stack[stack_size-1].add(e.toString());
		}
		else
			ret = e.execute();
		// if the e doesn't exist yet, its enclosing structure is null
		final Structure k = ret.getEnclosingStructure();
		if (k == null) {
			// if it's just a e name, add it to the innermost Structure on the stack that can hold custom variables
			if (voperator == Expression.VARIABLE) {
				final String varname = e.toString();
				caller.testVariableName(varname);
				for (int i = stack_size; --i >= 0; ) {
					if (( !create_in_LOCAL && stack[i].canHoldCustomVariables() )|| stack[i].get("structure_type").equals("LOCAL")) {
						return stack[i].add(varname);
					}
				}
				// unreachable code, because there's always an expandable structure on the stack
				caller.throwException("failed to create e. there is no structure that can hold custom variables on the stack atm");
			}
			else if (voperator == Expression.MEMBER) {
				// the e gets added to some structure, find the structure
				if (e.getSecondOperand().getOperator() != Expression.VARIABLE)
					caller.throwException("failed to create e. e name expected but operator type "+e.getSecondOperand().getOperator()+" found:\n"+e.getSecondOperand().toString());
				final String varname = e.getSecondOperand().toString();
				caller.testVariableName(varname);
				// fetch the structure manually if the first operand is a e name. otherwise execute the expression in the first operand to obtain the Structure
				Value structure = null;
				if (e.getFirstOperand().getOperator() != Expression.VARIABLE)
					structure = e.getFirstOperand().execute();
				else {
					final String structurename = e.getFirstOperand().toString();
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
					caller.throwException("failed to create e. structure "+e.getFirstOperand().toString()+" not found");
				if (structure.type() != Value.STRUCTURE)
					caller.throwException("failed to create e. "+e.getFirstOperand().toString()+" gives a "+structure.typeName()+" instead of a structure");
				if (!structure.structure().canHoldCustomVariables())
					caller.throwException("failed to create e. the structure "+e.getFirstOperand().toString()+" of type "+structure.get("structure_type")+" cannot hold custom variables");
				ret = structure.structure().add(varname);
			}
			else if (voperator == Expression.ARRAY_INDEX) {
				// fetch the array
				final Value varray = e.getFirstOperand().execute();
				if (varray.type() != Value.ARRAY)
					caller.throwException("failed to fetch assigned e. "+e.getFirstOperand().toString()+" gives a "+varray.typeName()+" instead of an array");
				final ArrayWrapper array = varray.arrayWrapper();
				// make sure it's a valid array index
				final Value vindex = e.getSecondOperand().execute();
				final int vit = vindex.type();
				int index = Integer.MIN_VALUE;
				if (vit == Value.INTEGER)
					index = vindex.integer();
				else if (vit == Value.REAL)
					index = Math.round(vit);
				else {
					String s = vindex.toString();
					if (s.length() == 0)
						index = array.array.length;
					else try {
						index = Integer.parseInt(s);
					} catch (Throwable t) {
						caller.throwException("\""+s+"\" is no a valid array index in "+e.toString());
					}
				}
				if (index < 0 || index > array.array.length)
					caller.throwException("failed to fetch or create e. Array index must be between 0 and "+(array.array.length-1)+" (inclusive), not "+index);
				else if (index < array.array.length)
					ret = array.array[index]; // array entries don't have an enclosing variable, so we need this special case
				else {
					ret = new Value();
					array.array = (Value[]) ArrayModifier.addElement(array.array, new Value[index+1], ret);
				}
			}
			else
				caller.throwException("failed to create e. unable to handle expressions whose top level operator is "+e.getOperatorElement().toString());
		}
		return ret;
	}


	public Value execute ()  {
		// fetch the variable
		Value ret = null, v;
		if (its_a_global) {
			// since it's a global value it must be a variable from the set of global variables
			ret = ((Value)stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES")).get(variable.toString());
			if (ret == null)
				ret = ((Value)stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES")).structure().add(variable.toString());
		}
		else {
			ret = fetchOrCreateVariable(variable, false, this);
		}

		// assign the value to the variable *************************************************************************************************************
		if (value_definition instanceof Function)
			return ret.set((Function)value_definition);
		else if (value_definition instanceof Expression) {
			final int operator = ((Expression)value_definition).getOperator();
			if (operator == Expression.GLOBAL_VALUE)
				return ret.setDirectly(value_definition.execute());
			if (operator == Expression.RAW_VALUE)
				return ret.setDirectly(value_definition.execute());
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


	public void setVariableExpression (final Expression _variable)  {
		variable = _variable;
		if (its_a_global && variable.getOperator() != Expression.VARIABLE)
			throwException("only simple variables allowed on the left side of the = in global definition blocks, no complex expressions, member operators, array operators and so on");
	}


	boolean print (final PrintStream out, final String indentation, boolean line_start)  {
		line_start = variable.print(out, indentation, line_start);
		out.print((line_start?indentation:" ") + "= ");
		return print(out, indentation, line_start, value_definition);
	}
}