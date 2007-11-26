package decker.model;
import java.io.PrintStream;



/** with this command you define constants (one word strings which can be used without quotes in scripts) */
final class TypeDefinitionCommand extends ScriptNode
{
	private TypeDefinition[] type; // the list of constants defined by this command


	TypeDefinitionCommand (final TypeDefinition[] _types, final String _script_name, final int _script_line, final int _script_column)  {
		super (_script_name, _script_line, _script_column);
		type = _types;
	}


	/** solely used by copy() */
	private TypeDefinitionCommand (final TypeDefinitionCommand original)  {
		super(original);
		type = new TypeDefinition[original.type.length];
		for (int i = type.length; --i >= 0; )
			type[i] = original.type[i].copy();
	}


	public ScriptNode copy()  { return new TypeDefinitionCommand(this); }


	public Value execute ()  {
		final TypeDefinition[] t = type;
		final int count = t.length;
		for (int i = 0; i < count; i++)
			t[i].execute();
		return null;
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start, final int depth)  {
		if (!line_start)
			out.println();
		if (type.length == 1) {
			out.println(indentation + "structure ");
			if (!type[0].print(out, indentation, true, depth))
				out.println();
		}
		else {
			out.println(indentation + "structure");
			final String ind = indentation + Global.BLOCK_INDENT;
			for (int i = 0; i < type.length; i++)
				if (!type[i].print(out, ind, true, depth-1))
					out.println();
		}
		return true;
	}


	public void replace (final String original, final boolean starts_with, final String replacement)  {}
}