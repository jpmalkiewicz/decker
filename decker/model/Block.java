package decker.model;
import java.io.PrintStream;
import decker.util.ArrayModifier;



class Block extends ScriptNode
{
	private ScriptNode[] child_command = new ScriptNode[0];


	Block (final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
	}


	Block (final Block original, final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
		final int count = original.child_command.length;
		child_command = new ScriptNode[count];
		for (int i = 0; i < count; i++)
			child_command[i] = original.child_command[i].copy();
	}


	/** solely used to copy Blocks */
	Block (final Block original)  {
		this(original, original.getScriptName(), original.getScriptLine(), original.getScriptColumn());
	}


	void addScriptNode (final ScriptNode command)  {
		child_command = (ScriptNode[]) ArrayModifier.addElement(child_command, new ScriptNode[child_command.length+1], command);
	}


	ScriptNode copy ()  {
		throwException("Block.copy() is not implemented, since Block is abstract ("+this.getClass().getName()+")");
		throw new RuntimeException("unreachable statement");
	}


	public Value execute ()  {
		final int count = child_command.length;
		for (int i = 0; i < count; i++) {
			if (child_command[i].execute() == BREAK_VALUE) {
				return BREAK_VALUE;
			}
		}
		return null;
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start, final int depth)  {
		if (!line_start)
			out.println();
		final int count = child_command.length;
		if (count > 0) {
			final String ind = indentation + Global.BLOCK_INDENT;
			if (depth <= 0)
				System.out.println(ind+"...");
			else
				for (int i = 0; i < count; i++)
					if (!child_command[i].print(out, ind, true, depth-1))
						out.println();
		}
		return true;
	}


	/** used to replace strings with their localized versions */
	void replace (final String original, final boolean starts_with, final String replacement)  {
		final int count = child_command.length;
		for (int i = 0; i < count; i++) {
			child_command[i].replace(original, starts_with, replacement);
		}
	}
}