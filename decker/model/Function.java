package decker.model;
import java.io.PrintStream;
import decker.util.ArrayModifier;


public final class Function extends ScriptNode
{
	private String[] argument_name = new String[0];
	private Block function_body; // can't extend both the Expression and the Block class so the Block representing the funcion body goes here
	private int id; // hard coded functions need an id to identify themselves


	Function (final int _id, final String[] _argument_list)  {
		super("", -1, -1);
		id = _id;
		argument_name = _argument_list;
	}


	Function (final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
		function_body = new Block(_script_name, _script_line, _script_column);
	}


	/** solely used by copy() */
	private Function (final Function original, final boolean dummy)  {
		super(original.getScriptName(), original.getScriptLine(), original.getScriptColumn());
		function_body = (Block) function_body.copy();
		final int count = original.argument_name.length;
		argument_name = new String[count];
		System.arraycopy(original.argument_name, 0, argument_name, 0, count);
	}


	/** when the function is called, its arguments are available in two forms in the function body.
	*   first through the ARRAY called argument, but also through the name of each argument if it has one.
	*   call this method to assign a name to an argument. the nth call will name the nth argument */
	void addArgumentName (final String _argument_name)  {
		testVariableName(_argument_name);
		if (_argument_name.equals("return_value") || _argument_name.equals("argument"))
			throwException("function arguments must not be named \""+_argument_name+"\"");
		argument_name = (String[]) ArrayModifier.addElement(argument_name, new String[argument_name.length+1], _argument_name);
	}


	public Function copy ()  { return new Function(this, false); }


	public Value execute ()  { return new Value().set(this); }


	String[] getArgumentNames ()  { return argument_name; }


	Block getFunctionBody ()  { return function_body; }


	int getFunctionID ()  { return id; }


	public boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
		out.print((line_start?indentation:"") + "FUNCTION "+((function_body==null)?Global.FUNCTION_NAME[id]:"")+" (");
		if (argument_name.length > 0) {
			out.print(argument_name[0]);
			for (int i = 1; i < argument_name.length; i++)
				out.print(", "+argument_name[i]);
		}
		out.println(")");
		if (function_body != null)
			return function_body.print(out, indentation, true);
		else
			return true;
	}


	/** used to replace strings with their localized versions */
	public void replace (final String original, final boolean starts_with, final String replacement)  {
		function_body.replace(original, starts_with, replacement);
	}
}