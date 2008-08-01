package decker.model;

import java.io.*;
import decker.util.*;

public final class Structure implements Comparable, ValueListener
{
	private final StringTreeMap members = new StringTreeMap();
	private ValueListener[] valueListener;
	private int valueListenerCount;

	public Structure (String type_name, ScriptNode caller)  {
		if (type_name == null)
			throw new RuntimeException("trying to use null as a structure type");
		add("structure_type").set(type_name);
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
					else if (k.equals("scriptname") && caller != null && ((Value)n.getValue()).equalsConstant("UNDEFINED"))
						add(k).set(caller.getScriptName());
					else if (!k.equals("pixelwidth") && !k.equals("pixelheight") && !k.equals("expandable")) // pixelwidth and pixelheight stay with their structure type and aren't copied to the instantiated structures
						add(k).set((Value)n.getValue());
				}
				// execute the STRUCTURE_TYPE's initializer function if there is one
				if (initializer != null && initializer.type() == Value.FUNCTION)
					FunctionCall.executeFunctionCall(initializer, null, new Structure[]{ this });
			}
		}
	}

	Structure (final Structure original)  {
		StringTreeMap.Iterator i = original.members.getIterator();
		while (i.hasNext()) {
			final StringTreeMap.TreeNode n = i.nextNode();
			add(n.getKey()).set((Value)n.getValue());
		}
	}



	/** creates a special LOCAL structure for a FunctionCall */
	Structure (final Value[] arguments, final String[] argument_names)  {
		this ("LOCAL", null);
		add("argument").set(new ArrayWrapper(arguments));
		add("return_value");
//System.out.println("Structure : "+arguments.length+"   "+argument_names.length+"  "+((argument_names.length > 0)?argument_names[0]:""));
		// manually add all the named arguments to the structure - they contain the same variable as the arguments array, not just the same value
		final int argument_count = arguments.length;
		for (int i = argument_names.length; --i >= 0; ) {
			if (i >= argument_count)
				add(argument_names[i]);
			else {
				members.put(argument_names[i], arguments[i]);
				if (arguments[i].getEnclosingStructure() == null)
					arguments[i].setEnclosingStructure(this);
			}
		}
	}



	/** adds a new variable to this collection or replaces an old variable */
	public Value add (String name)  {
		final Value ret = new Value(this);
		members.put(name, ret);
		if (valueListenerCount>0) {
			for (int i = valueListenerCount; --i >= 0; ) {
				valueListener[i].eventValueChanged(name, this, null, ret);
			}
		}
		return ret;
	}



	/** this value listener will automatically be called for all value changes of variables in this Structure */
	public void addValueListener (final ValueListener vl) {
		if (valueListener == null) {
			valueListener = new ValueListener[1];
		}
		else if (valueListenerCount == valueListener.length) {
			final ValueListener[] newList = new ValueListener[valueListenerCount*2];
			System.arraycopy(valueListener, 0, newList, 0, valueListenerCount);
			valueListener = newList;
		}
		valueListener[valueListenerCount++] = vl;
	}



	boolean canHoldCustomVariables ()  {
		final String typename = members.get("structure_type").toString();
		Value type = ScriptNode.getStructureType(typename);
		Value v;
		return type != null && (v=type.get("expandable")) != null && v.equals(true);
	}



	/** removes all members from the structure */
	public void clear ()  { members.clear(); }



	public int compareTo (Object o) {
		if (o == null)
			return 1;
		int ret = o.hashCode() - hashCode();
		if (ret != 0 || o == this)
			return ret;
		if (o instanceof Structure) {
			ret = get("structure_type").string().compareTo(((Structure)o).get("structure_type").string());
			if (ret != 0)
				return ret;
		}
		else {
			return 1;
		}
		throw new RuntimeException("identical hash code for different Structures");
	}



	/** returns a member variable from this collection but doesn't add it if it doesn't exist yet */
	public Value get (final String name) {
		return (Value) members.get(name);
	}



	String getKey (final Value v) {
		StringTreeMap.Iterator i = members.getIterator();
		StringTreeMap.TreeNode n;
		while ((n=i.nextNode()) != null) {
			if (v == n.getValue()) {
				return n.getKey();
			}
		}
		return null;
	}



	/** Global.setCurrentRuleset() uses this to replaces global values in ENGINE when replacing the ruleset */
	StringTreeMap.Iterator getStringTreeMapIterator () {
		return members.getIterator();
	}



	/** called whenever a variable stored in this Structure changes its value */
	public void eventValueChanged (final String varname, final Structure structure, final Value old_value, final Value new_value) {
		if (structure != this)
			throw new RuntimeException("(structure != this) should never be true when calling this function");
		for (int i = valueListenerCount; --i >= 0; ) {
			valueListener[i].eventValueChanged(varname, structure, old_value, new_value);
		}
	}



	/** called whenever a variable stored in an array stored in a variable in this Structure (or stored in an array, that is stored in an array {and so on} that is ...) changes its value */
	public void eventValueChanged (final int index, final ArrayWrapper array, final Value old_value, final Value new_value) {
		for (int i = valueListenerCount; --i >= 0; ) {
			valueListener[i].eventValueChanged(index, array, old_value, new_value);
		}
	}



	public boolean print (final PrintStream out, final String indentation, boolean line_start, final int depth)  {
		final String ind = indentation+Global.BLOCK_INDENT;
		out.print((line_start?indentation:"") + get("structure_type"));
		// make sure we're not going too deep into recurions
		if (depth <= 0) {
			if (members.size() > 0) {
				out.println(); // add a line feed behind the structure type
				out.println(ind+"[...]");
			}
		}
		else {
			out.println(); // add a line feed behind the structure type
			final StringTreeMap.Iterator i = members.getIterator();
			while (i.hasNext()) {
				final StringTreeMap.TreeNode n = i.nextNode();
				if (!n.getKey().equals("structure_type")) {
					out.print(ind + n.getKey() + " = ");
					final Value v = (Value) n.getValue();
					final int vt = v.type();
					if (vt == Value.STRUCTURE)
						v.structure().print(out, ind, false, depth-1);
					else if (vt == Value.FUNCTION)
						v.function().print(out, ind, false, depth-1);
					else if (vt == Value.ARRAY) {
						if (!v.arrayWrapper().print(out, ind, false, depth-1))
							out.println();
					}
					else
						out.println(v.toStringForPrinting());
				}
			}
		}
		return true;
	}



	void putDirectlyIntoStringTreeMap (final String key, final Value v)  {
		if (v == null)
			throw new RuntimeException("who's putting null into a StringTreeMap ?!?");
		members.put(key, v);
	}



	void remove (final String key)  {
		members.remove(key);
	}



	/** this value listener will automatically be called for all value changes of variables in this Structure */
	public void removeValueListener (final ValueListener vl) {
		if (valueListener != null) {
			for (int i = valueListenerCount; --i >= 0; ) {
				if (valueListener[i] == vl) {
					System.arraycopy(valueListener, i+1, valueListener, i, valueListenerCount-i-1);
					valueListener[valueListenerCount-1] = null;
					valueListenerCount--;
					return;
				}
			}
		}
	}



	public int size ()  { return members.size(); }



	public String toString ()  { return ((Value)members.get("structure_type")).string(); }
}