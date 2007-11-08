package decker.model;
import java.io.*;
import decker.util.*;



public final class Structure
{
	private final StringTreeMap members = new StringTreeMap();
	private ValueListener[] valueListener;
	private int valueListenerCount;


	public Structure (final String type_name)  {
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
					else if (!k.equals("pixelwidth") && !k.equals("pixelheight") && !k.equals("expandable")) // pixelwidth and pixelheight stay with their structure type and aren't copied to the instantiated structures
						add(k).set((Value)n.getValue());
				}
				// execute the STRUCTURE_TYPE's initializer function if there is one
				if (initializer != null && initializer.type() == Value.FUNCTION)
					FunctionCall.executeFunctionCall(initializer, null, this);
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
		this ("LOCAL");
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


	/** called whenever a variable stored in this Structure changes its value */
	void eventValueChanged (final String varname, final Value old_value, final Value new_value) {
		for (int i = valueListenerCount; --i >= 0; ) {
			valueListener[i].eventValueChanged(varname, this, old_value, new_value);
		}
	}


	/** returns a member variable from this collection but doesn't add it if it doesn't exist yet */
	public Value get (final String name) {
		return (Value) members.get(name);
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
				if (!n.getKey().equals("structure_type")) {
					out.print(ind + n.getKey() + " = ");
					final Value v = (Value) n.getValue();
					final int vt = v.type();
					if (vt == Value.STRUCTURE)
						v.structure().print(out, ind, false);
					else if (vt == Value.FUNCTION)
						v.function().print(out, ind, false);
					else if (vt == Value.ARRAY)
						v.arrayWrapper().print(out, ind, false);
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


	public int size ()  { return members.size(); }


	public String toString ()  { return ((Value)members.get("structure_type")).string(); }
}