package decker.model;
import decker.util.StringPrintStream;
import java.io.PrintStream;



public abstract class ScriptNode
{
// *****************************************************************************************************************************************************
// enclosing structures stack ***********************************************************************************************************************************
// *****************************************************************************************************************************************************

	public final static int ENGINE_STACK_SLOT = 0, RULESET_STACK_SLOT = 1, GLOBAL_STACK_SLOT = 2, DEFAULT_GLOBAL_STACK_SIZE = 3;
	final static Structure[] stack = new Structure[1000];
	static int stack_size;
	static int global_stack_size;
	public final static Structure KEEP_STACK = new Structure("KEEP_STACK"); // when this is used for the enclosing structure when calling functionCall(), the stack will not be emptied for the function call


	public final static void addStackItem (final Structure n)  { stack[stack_size++] = n; }


	/** returns a mirror of the stack */
	final static Structure[] dumpStack ()  {
		final Structure[] ret = new Structure[stack_size];
		System.arraycopy(stack, 0, ret, 0, stack_size);
		return ret;
	}


	public final static Structure getStackEntry (final int slot)  { return stack[slot]; }


	final static int getStackTop ()  { return stack_size-1; }


	public final static void printStack (final PrintStream out, final String indentation)  {
		out.println(indentation+"stack :");
		for (int i = 0; i < stack_size; i++)
			out.println(indentation+Global.BLOCK_INDENT+stack[i].toString());
		out.println();
	}


	public final static void removeStackItem (final Structure n)  {
		if (stack[--stack_size]!=n)
			new Block("<no script>",-1,-1).throwException("structure stack corrupted");
		stack[stack_size] = null;
	}


	public final static void removeStackItem (final Structure n, final ScriptNode caller)  {
		if (stack[--stack_size]!=n)
			caller.throwException("structure stack corrupted");
		stack[stack_size] = null;
	}


	final static Structure[] removeLocalStackItems ()  {
		final int k = global_stack_size;
		final Structure[] ret = new Structure[stack_size-k];
		System.arraycopy(stack, k, ret, 0, stack_size-k);
		for (int i = stack_size; --i >= k; )
			stack[stack_size] = null;
		stack_size = k;
		return ret;
	}


	final static void restoreLocalStack (final Structure[] old_stack)  {
		final int k = global_stack_size;
		final int new_size = old_stack.length+k;
		// manually wipe out the structure entries which won't be overwritten
		for (int i = stack_size; --i >= new_size; )
			stack[i] = null;
		System.arraycopy(old_stack, 0, stack, k, old_stack.length);
		stack_size = new_size;
	}


// *****************************************************************************************************************************************************
// *****************************************************************************************************************************************************
// *****************************************************************************************************************************************************


	// the variables below store the exact location in script where the definition of this ScriptNode came from
	private int script_line, script_column;
	private String script_name;


	/** this is the default constructor */
	ScriptNode (final String _script_name, final int _script_line, final int _script_column) {
		script_name = _script_name;
		script_line = _script_line;
		script_column = _script_column;
	}


	/** called when copying a ScriptNode, to transfer the position of this ScriptNode in the script file it comes from */
	ScriptNode (final ScriptNode original) {
		if (original != null) {
			script_name = original.script_name;
			script_line = original.script_line;
			script_column = original.script_column;
		}
		else {
			script_name = "";
			script_line = -1;
			script_column = -1;
		}
	}


	abstract ScriptNode copy ();


	public abstract Value execute ();


	int getScriptColumn () { return script_column; }


	int getScriptLine () { return script_line; }


	String getScriptName () { return script_name; }


	final public static Value getValue (final String name)  {
		final Value ret = getVariable(name);
		return (ret == null) ? null : ret.getValue();
	}


	final public static Value getVariable (final String name)  {
		// check whether the "variable" is a constant or structure type
		Value ret;
		if ((ret=stack[RULESET_STACK_SLOT].get("CONSTANTS").get(name)) != null)
			return ret;
		if ((ret=stack[RULESET_STACK_SLOT].get("STRUCTURE_TYPES").get(name)) != null)
			return ret;
//		if ((ret=stack[ENGINE_STACK_SLOT].get("STRUCTURE_TYPES").get(name)) != null)
//			return ret;
		// try to fetch the variable from one of the Structures in the global structures stack
		for (int i = stack_size; --i >= 0; )
			if ((ret=stack[i].get(name)) != null)
				return ret;
		return null;
	}


	/** returns true iff the cursor is sitting at the beginning of a new line */
	abstract boolean print (final PrintStream out, final String indentation, final boolean line_start);


	/** called whenever a ScriptNode that may no exist gets printed */
	boolean print (final PrintStream out, final String indentation, boolean line_start, final ScriptNode e) {
		if (e == null) {
			out.print("%MISSING OPERAND%");
			return false;
		}
		return e.print(out, indentation, line_start);
	}


	/** localized this SriptNode and its child nodes */
	void replace (String original, boolean starts_with, String replacement) {}


	void testVariableName (final String variable_name) {
		if(variable_name.length() == 0)
			throwException("An empty string is not a valid variable name.");
		for(int i = 0; i < variable_name.length(); i++)  {
			if(ScriptReader.VARIABLE_NAME_CHARACTERS.indexOf(variable_name.charAt(i)) == -1)
				throwException(variable_name+" is not a valid variable name.");
		}
		if (Global.COMMANDS.indexOf(" "+variable_name+" ") > -1)
			throwException("trying to use command name "+variable_name+" as a variable name");
		if (stack[RULESET_STACK_SLOT] != null) {
			final Value c = stack[RULESET_STACK_SLOT].get("CONSTANTS");
			if (c != null && c.type() == Value.STRUCTURE && c.get(variable_name) != null)
				throwException(variable_name+" is not a valid variable name, it's already being used as a constant");
		}
	}


	void throwException (final String s)  {
		// display the current stack
		printStack(System.err, "");
		throw new RuntimeException("Error in "+script_name+" line "+script_line+" column "+script_column+" :\n"+toString()+"\n"+s);
	}


	public String toString ()  {
		final StringPrintStream sps = new StringPrintStream();
		print(sps, "", true);
		return sps.toString();
	}
}