package decker.model;
import java.io.PrintStream;
import decker.util.*;



final class AssignmentCommand extends ScriptNode
{
	private Expression variable;
	private ScriptNode value_definition;
	private boolean its_a_global, its_a_type_definition;


	AssignmentCommand (final boolean _its_a_global, final boolean _its_a_type_definition, final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
		its_a_global = _its_a_global;
		its_a_type_definition = _its_a_type_definition;
	}


	AssignmentCommand (final AssignmentCommand original)  {
		super(original);
		variable = original.variable.copy();
		value_definition = original.value_definition.copy();
		its_a_global = original.its_a_global;
		its_a_type_definition = original.its_a_type_definition;
	}


	ScriptNode copy ()  { return new AssignmentCommand(this); }


	static Object[] fetchOrCreateVariable (final Expression e, final boolean create_in_LOCAL, final ScriptNode caller, final Value replace_with_global, final boolean its_a_type_definition) {
		final int voperator = e.getOperator();
		// it's just a variable name
		if (voperator == Expression.VARIABLE) {
			final String varname = e.toString();
			if (!its_a_type_definition) {
				// if there exists a LOCAL variable of that name, use that
				for (int i = stack_size; --i >= 0; ) {
					if (stack[i].get("structure_type").equals("LOCAL")) {
						final Value ret = stack[i].get(varname);
						if (ret != null)
							return new Object[]{ stack[i], varname, fetchStructureMember(stack[i], varname, ret, replace_with_global, caller), ret };
						break;
					}
				}
			}
			// if the topmost structure on the stack is expandable and not of type LOCAL, add the variable to it (if it doesn't exist yet)
			if (!create_in_LOCAL &&( its_a_type_definition || stack[stack_size-1].canHoldCustomVariables() )&& !stack[stack_size-1].get("structure_type").equals("LOCAL"))
				return new Object[]{ stack[stack_size-1], varname, fetchStructureMember(stack[stack_size-1], varname, stack[stack_size-1].get(varname), replace_with_global, caller), stack[stack_size-1].get(varname) };
			// try to find it an existing variable of that name
			for (int i = stack_size; --i >= 0; ) {
				final Value ret = stack[i].get(varname);
				if (ret != null)
					return new Object[]{ stack[i], varname, fetchStructureMember(stack[i], varname, ret, replace_with_global, caller), ret };
			}
			// add it to the innermost Structure on the stack that can hold custom variables
			for (int i = stack_size; --i >= 0; ) {
				if (( !create_in_LOCAL && stack[i].canHoldCustomVariables() )|| stack[i].get("structure_type").equals("LOCAL")) {
					return new Object[]{ stack[i], varname, fetchStructureMember(stack[i], varname, null, replace_with_global, caller), null };
				}
			}
			// unreachable code, because there's always an expandable structure on the stack
			caller.throwException("failed to create variable. there is no structure that can hold custom variables on the stack atm");
		}
		else if (voperator == Expression.MEMBER) {
			// find the structure our variable is or will be a member of
			if (e.getSecondOperand().getOperator() != Expression.VARIABLE)
				caller.throwException("failed to create variable. variable name expected but operator type "+e.getSecondOperand().getOperator()+" found:\n"+e.getSecondOperand().toString());
			final String varname = e.getSecondOperand().toString();
			// fetch the structure manually if the first operand is a variable name. otherwise execute the expression in the first operand to obtain the Structure
			final Expression first_operand = e.getFirstOperand();
			Value structure_value = null;
			Structure structure = null;
			if (first_operand.getOperator() != Expression.VARIABLE)
				structure_value = e.getFirstOperand().execute();
			else {
				final String structurename = e.getFirstOperand().toString();
				for (int i = stack_size; --i >= 0; ) {
					if (stack[i].get("structure_type").equals(structurename)) {
						structure = stack[i];
						break;
					}
					if ((structure_value=stack[i].get(structurename)) != null)
						break;
				}
			}
			if (structure == null) {
				if (structure_value == null)
					caller.throwException("failed to create variable. structure "+e.getFirstOperand().toString()+" not found");
				if (structure_value.type() != Value.STRUCTURE)
					caller.throwException("failed to create variable. "+e.getFirstOperand().toString()+" gives a "+structure_value.typeName()+" ("+structure_value+") instead of a structure");
				structure = structure_value.structure();
			}
			final Value ret = structure.get(varname);
			if (ret == null && !structure.canHoldCustomVariables())
				caller.throwException("failed to create variable. the structure "+e.getFirstOperand().toString()+" of type "+structure.get("structure_type")+" cannot hold custom variables");
			return new Object[]{ structure, varname, fetchStructureMember(structure, varname, ret, replace_with_global, caller), ret };
		}
		else if (voperator == Expression.ARRAY_INDEX) {
			// fetch the array
			final Value varray = e.getFirstOperand().execute();
			if (varray.type() != Value.ARRAY)
				caller.throwException("failed to fetch assigned variable. "+e.getFirstOperand().toString()+" gives a "+varray.typeName()+" instead of an array");
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
				caller.throwException("failed to fetch or create variable. Array index must be between 0 and "+(array.array.length-1)+" (inclusive), not "+index);
			if (index == array.array.length) {
				array.array = (Value[]) ArrayModifier.addElement(array.array, new Value[index+1], (replace_with_global!=null)?replace_with_global:new Value());
				return new Object[]{ array, new Value().set(index), array.array[index], null };
			}
			else if (replace_with_global != null) { // the returned variable is not a structure member, so we can't use fetchStructureMember() to handle global variables here
				final Value old_value = array.array[index];
				array.array[index] = replace_with_global;
				return new Object[]{ array, new Value().set(index), replace_with_global, old_value };
			}
			else if (array.array[index].isGlobal()) {
				final Value old_value = array.array[index];
				array.array[index] = new Value(); // undo the previous replacement with a global variable
				return new Object[]{ array, new Value().set(index), array.array[index], old_value };
			}
			return new Object[]{ array, new Value().set(index), array.array[index], new Value().set(array.array[index]) };
		}
		else
			caller.throwException("failed to create variable. unable to handle expressions whose top level operator is "+e.getOperatorElement().toString());
		throw new RuntimeException("unreachable statement");
	}


	private static Value fetchStructureMember (final Structure s, final String varname, final Value current_variable, final Value replace_with_global, final ScriptNode caller) {
		// the variable may have become, or no longer is a reference to a global
		if (replace_with_global != null) {
			s.putDirectlyIntoStringTreeMap(varname, replace_with_global);
			return replace_with_global;
		}
		else if (current_variable == null || current_variable.isGlobal()) {
			if (current_variable == null)
				caller.testVariableName(varname);
			final Value ret = new Value();
			// using putDirectlyIntoStringTreeMap() here to avoid calling the ValueListeners twice
			s.putDirectlyIntoStringTreeMap(varname, ret); // undo the previous replacement with a global variable, or add a new variable
			return ret;
		}
		return current_variable;
	}


	public Value execute ()  {
		Object[] data;
//		Value old_value = variable.execute();
		// determine the new value of the variable
		Value assigned_value = null;
		if (value_definition != null && value_definition instanceof Expression && ((Expression)value_definition).getOperator() != Expression.GLOBAL_VALUE)
			assigned_value = ((Expression)value_definition).execute();
		// fetch the variable *****************************************************************************************************************************
		Value new_value = null;
		if (its_a_global) {
			// since it's a global value it must be a variable from the set of global values
			final Structure global_values = ((Value)stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES")).structure();
			final String varname = variable.toString();
			Value old_value = global_values.get(varname);
			new_value = old_value;
			if (new_value == null)
				new_value = ((Value)stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES")).structure().add(variable.toString());
			data = new Object[]{ global_values, varname, new_value, old_value, varname };
		}
		else {
			// check whether we're replacing a variable with a global value
			Value replacement = null;
			if (value_definition != null && value_definition instanceof Expression && ((Expression)value_definition).getOperator() == Expression.GLOBAL_VALUE) {
				final String value_name = ((Expression)value_definition).getFirstOperand().toString();
				replacement = ((Value)stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES")).get(value_name);
				if (replacement == null)
					replacement = ((Value)stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES")).structure().add(value_name);
			}
			data = fetchOrCreateVariable(variable, false, this, replacement, its_a_type_definition);
			new_value = (Value) data[2];
		}
		// then assign the value to the variable *************************************************************************************************************
		if (value_definition instanceof Function) {
			// if the variable will have a new value and hasn't been replaced by another variable, store its old value in a dummy variable
			if (new_value == data[3])
				data[3] = new Value().set(new_value);
			new_value.set((Function)value_definition);
		}
		if (value_definition != null && value_definition instanceof Expression && ((Expression)value_definition).getOperator() != Expression.GLOBAL_VALUE) {
			if (new_value == data[3])
				data[3] = new Value().set(new_value);
			new_value.set(assigned_value);
		}
		// finally adjust what ValueListeners are listening to it and tell them about it, if the value has changed
		final Value old_value = (Value)data[3];
		final ValueListener container = (ValueListener) data[0];
		if (old_value == null || !new_value.equals(old_value)) {
			// if the new/old value is an array, we have to manually add/remove ValueListeners to/from it (unless container is a LOCAL or RULESET structure)
			if (old_value != null && old_value.type() == Value.ARRAY) {
				old_value.arrayWrapper().removeValueListener(container);
			}
			String s;
			if (new_value.type() == Value.ARRAY &&( !(container instanceof Structure) || -1 == " LOCAL RULESET ENGINE ".indexOf(((Structure)container).get("structure_type").string()) )) {
				new_value.arrayWrapper().addValueListener(container);
//System.out.println("adding container "+container.toString()+" as value listener to array in "+data[1]);
			}
			// notify all listeners of the value change
			if (container instanceof Structure)
				((Structure)container).eventValueChanged((String)data[1], (Structure)container, old_value, new_value);
			else
				((ArrayWrapper)container).eventValueChanged(((Value)data[1]).integer(), (ArrayWrapper)container, old_value, new_value);
		}
		return new_value;
	}


	boolean print (final PrintStream out, final String indentation, boolean line_start, final int depth)  {
		line_start = variable.print(out, indentation, line_start, depth);
		out.print((line_start?indentation:" ") + "= ");
		return print(out, indentation, line_start, value_definition, depth);
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
}