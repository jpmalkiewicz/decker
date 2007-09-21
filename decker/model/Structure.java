package decker.model;
import java.io.*;
import decker.util.*;



public final class Structure
{
	private final StringTreeMap members = new StringTreeMap();


	public Structure (final String type_name)  {
		addDirectly("structure_type").set(type_name);
		// if there exists a template structure for this type, copy it
		if (ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] != null)  {
			Value template_value = ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT].get("STRUCTURE_TYPES").get(type_name);
			if (template_value == null)
				template_value = ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT].get("STRUCTURE_TYPES").get(type_name);
			// copy the predefined STRUCTURE_TYPE. instead of copying the optional initializer function of the STRUCTURE_TYPE,
			// that function gets executed for this new Structure
			if (template_value != null) {
				final Structure template = template_value.structure();
				StringTreeMap.Iterator i = template.members.getIterator();
				Value initializer = null;
				while (i.hasNext()) {
					final StringTreeMap.TreeNode n = i.nextNode();
					final String k = n.getKey();
					if (k.equals("initializer"))
						initializer = (Value) n.getValue();
					else if (!k.equals("pixelwidth") && !k.equals("pixelheight") && !k.equals("expandable")) // pixelwidth and pixelheight stay with their structure type and aren't copied to the instantiated structures
						addDirectly(k).setDirectly((Value)n.getValue(), false);
				}
				// execute the STRUCTURE_TYPE's initializer function if there is one
				if (initializer != null && initializer.typeDirect() == Value.FUNCTION)
					FunctionCall.executeFunctionCall(initializer, null, this);
			}
		}
	}


	Structure (final Structure original)  {
		StringTreeMap.Iterator i = original.members.getIterator();
		while (i.hasNext()) {
			final StringTreeMap.TreeNode n = i.nextNode();
			addDirectly(n.getKey()).setDirectly((Value)n.getValue(), false);
		}
	}


	/** creates a special LOCAL structure for a FunctionCall */
	Structure (final Value[] arguments, final String[] argument_names)  {
		this ("LOCAL");
		add("argument").set(new ArrayWrapper(arguments));
		add("return_value");
		// manually add all the named arguments to the structure - they contain the same variable as the arguments array, not just the same value
		final int argument_count = arguments.length;
		for (int i = argument_names.length; --i >= 0; ) {
			if (i >= argument_count)
				add(argument_names[i]);
			else
				members.put(argument_names[i], arguments[i]);
		}
	}


	/** adds a new variable to this collection */
	public Value add (String name)  {
		final Value ret = new Value(this);
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
		final String typename = members.get("structure_type").toString();
		Value type = ScriptNode.getStructureType(typename);
		Value v;
		return type != null && (v=type.get("expandable")) != null && v.equals(true);
	}


	/** removes all members from the structure */
	public void clear ()  { members.clear(); }


	/** returns a member variable from this collection but doesn't add it if it doesn't exist yet */
	public Value get (final String name) {
		return (Value) members.get(name);
	}


	public Value getValue (final String name) {
		final Value ret = (Value) members.get(name);
		return (ret == null) ? null : ret.getValue();
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
			out.println(); // gotta add a line feed behind the structure type
			final StringTreeMap.Iterator i = members.getIterator();
			while (i.hasNext()) {
				final StringTreeMap.TreeNode n = i.nextNode();
				out.print(ind + n.getKey() + " = ");
				final Value v = (Value) n.getValue();
				if (v.typeDirect() == Value.STRUCTURE)
					v.structure().print(out, ind, false);
				else if (v.isExpression())
					v.expression().print(out, ind, false);
				else if (v.isFunction())
					v.function().print(out, ind, false);
				else
					out.println(v.toString());
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
}