package decker.model;
import java.io.*;
import decker.util.*;


/** if variables_see_each_other is true the variables inside the structure see each other
*   that is, when an expression stored in a variable is executed it can reference other variables from the structure */
public final class Structure
{
	private final StringTreeMap members = new StringTreeMap();


	public Structure (final String type_name)  {
		this (type_name, false);
	}


	Structure (final Structure original)  {
		StringTreeMap.Iterator i = original.members.getIterator();
		while (i.hasNext()) {
			final StringTreeMap.TreeNode n = i.nextNode();
			addDirectly(n.getKey()).setDirectly((Value)n.getValue(), false);
		}
	}


	Structure (final String type_name, final boolean explicitly_expandable)  {
		addDirectly("structure_type").set(type_name);
		// if there exists a template structure for this type, copy it
		if (ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] != null)  {
			final Value template_value = ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT].get("STRUCTURE_TYPES").get(type_name);
			// copy the predefined STRUCTURE_TYPE. instead of copying the optional initializer function of the STRUCTURE_TYPE,
			// that function gets executed for this new Structure
			if (template_value != null) {
				final Structure template = template_value.structure();
				StringTreeMap.Iterator i = template.members.getIterator();
				Value initializer = null;
				while (i.hasNext()) {
					final StringTreeMap.TreeNode n = i.nextNode();
					if (n.getKey().equals("initializer"))
						initializer = (Value) n.getValue();
					else
						addDirectly(n.getKey()).setDirectly((Value)n.getValue(), false);
				}
				// execute the STRUCTURE_TYPE's initializer function if there is one
				if (initializer != null && initializer.typeDirect() == Value.FUNCTION)
					FunctionCall.executeFunctionCall(initializer, null, this);
			}
		}
		if (explicitly_expandable)
			add("expandable").set(true);
	}


	/** creates a special LOCAL structure for a FunctionCall. arguments must be an ARRAY structure containing all the argument values the caller supplied in the function call*/
	Structure (final Structure arguments, final String[] argument_names)  {
		this ("LOCAL");
		if (!arguments.get("structure_type").equals("ARRAY"))
			throw new RuntimeException("the list of arguments must be a an ARRAY structure");
		Structure ret = new Structure("FUNCTION_CALL");
		add("argument").set(arguments);
		add("return_value");
		add("expandable").set(true);
		// manually add all the named arguments to the structure - they contain the same variable as the arguments array, not just the same value
		final int argument_count = arguments.get("size").integer();
		for (int i = argument_names.length; --i >= 0; ) {
			if (i >= argument_count)
				add(argument_names[i]);
			else
				members.put(argument_names[i], arguments.get(i+""));
		}
	}


	/** adds a new variable to this collection */
	public Value add (String name)  {
		final Value ret = new Value(this);
		if (get("structure_type").equals("ARRAY") &&( name.equals("") || name.equals(((Value)members.get("size")).toString()) )) {
			final Value size = (Value) members.get("size");
			final int size_int = size.integer();
			size.set(size_int+1);
			name = size_int+"";
		}
		final Value overwritten_value = (Value) members.put(name, ret);
		if (overwritten_value != null)
			overwritten_value.destroy();
		return ret;
	}


	/** adds a vriable to this Structure without doing any tests, like whether the Structure is an ARRAY and the added variable is "" */
	public Value addDirectly (String name)  {
		final Value ret = new Value(this);
		members.put(name, ret);
		return ret;
	}


	boolean canHoldCustomVariables ()  {
		final Object o = members.get("expandable");
		return o != null && ((Value)o).equals(true);
	}


	/** removes all members from the structure */
	public void clear ()  { members.clear(); }


	public Value deleteFromArray (final int index)  {
		Value vsize = (Value) members.get("size");
		if (get("structure_type").equals("ARRAY") && index >= 0 && index < vsize.integer()) {
			final int size = vsize.integer();
			final Value ret = (Value) members.get(index+"");
			for (int i = index+1; i < size; i++)
				members.put((i-1)+"", members.get(i+""));
			vsize.set(size-1);
			members.remove((size-1)+"");
			return ret;
		}
		return null;
	}


	/** returns a member variable from this collection but doesn't add it if it doesn't exist yet */
	public Value get (final String name) {
		return (Value) members.get(name);
	}


	public Value getValue (final String name) {
		final Value ret = (Value) members.get(name);
		return (ret == null) ? null : ret.getValue();
	}


	public Value insertIntoArray (final int index)  {
		Value vsize = (Value) members.get("size");
		if (get("structure_type").equals("ARRAY") && index >= 0 && index <= vsize.integer()) {
			final int size = vsize.integer();
			for (int i = size; i > index; i--)
				members.put(i+"", members.get((i-1)+""));
			final Value ret = new Value(this);
			members.put(index+"", ret);
			vsize.set(size+1);
			return ret;
		}
		return null;
	}


	public boolean print (final PrintStream out, final String indentation, boolean line_start)  {
		final String ind = indentation+Global.BLOCK_INDENT;
		out.print((line_start?indentation:"") + get("structure_type"));
		// make sure we're not going too deep into recurions
		if (ind.length() == 10*Global.BLOCK_INDENT.length()) {
			if (members.size() > 0)
				out.println(ind+"...");
		}
		else {
			// print everything except for ARRAYs
			if (!get("structure_type").equals("ARRAY")) {
				out.println(); // gotta add a line feed behind the structure type
				final StringTreeMap.Iterator i = members.getIterator();
				while (i.hasNext()) {
					final StringTreeMap.TreeNode n = i.nextNode();
					out.print(ind + n.getKey() + " = ");
					final Value v = (Value) n.getValue();
					if (v.typeDirect() == Value.STRUCTURE)
						v.structure().print(out, ind, false);
					else if (v.typeDirect() == Value.FUNCTION)
						v.function().print(out, ind, false);
					else
						out.println(v.toString());
				}
			}
			else {
				// it's an array. display it
				// display the arraysize behind the ARRAY tag
				final int count = get("size").integer();
				out.println(" ("+count+")");
				for (int j = 0; j < count; j++) {
					final Value v = get(j+"");
					if (v.type() == Value.STRUCTURE)
						v.structure().print(out, ind, true);
					else if (v.type() == Value.FUNCTION)
						v.function().print(out, ind, true);
					else
						out.println(ind+v.toString());
				}
			}
		}
		return true;
	}


	void putDirectlyIntoStringTreeMap (final String key, final Value v)  { members.put(key, v); }


	void remove (final String key)  {
		final Value overwritten_value = (Value) members.remove(key);
		if (overwritten_value != null)
			overwritten_value.destroy();
	}


	public int size ()  { return members.size(); }


	public String toString ()  { return ((Value)members.get("structure_type")).string(); }


	boolean staticStackEntry ()  { final Object o = members.get("static_stack_entry"); return o != null && ((Value)o).equals(true); }


	public boolean variablesSeeEachOther ()  { final Object o = members.get("variables_see_each_other"); return o != null && ((Value)o).equals(true); }
}