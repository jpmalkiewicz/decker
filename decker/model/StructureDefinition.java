package decker.model;
import java.io.PrintStream;



final class StructureDefinition extends Expression
{
	private Object structure_type;
	private Block definition_body;


	/** _structure_type is either a String or, if this is a copy() command, an Expression */
	StructureDefinition (Object _structure_type, final String _script_name, final int _script_line, final int _script_column, final Expression[] expression_stack, final int[] expression_stack_top)  {
		super(_script_name, _script_line, _script_column);
		structure_type = _structure_type;
		setOperator(STRUCTURE_DEFINITION);
		// add this structure definition to the current Expression tree
		if (expression_stack != null) {
			if (expression_stack_top[0] > -1)
				expression_stack[expression_stack_top[0]].addExpression(this);
			expression_stack[++expression_stack_top[0]] = this;
		}
	}


	/** solely used by copy() */
	private StructureDefinition (final StructureDefinition original)  {
		super(original.getScriptName(), original.getScriptLine(), original.getScriptColumn());
		if (structure_type instanceof String)
			structure_type = original.structure_type;
		else
			structure_type = ((Expression)original.structure_type).copy();
		if (original.definition_body != null)
			definition_body = new Block(original.definition_body);
	}


	void addDefinitionBody (final Block _definition_body) { definition_body = _definition_body; }


	public StructureDefinition copy ()  { return new StructureDefinition(this); }


	public Value execute () {
		// create the new structure
		final Value x = (structure_type instanceof String) ? null : ((Expression)structure_type).execute();
		if (x != null && x.type() != Value.STRUCTURE)
			throwException(structure_type.toString() + " should return a structure, but it returned the "+x.typeName()+" "+x.toString());
		final Structure k = (structure_type instanceof String) ? new Structure((String)structure_type, this) : new Structure(x.structure());
		// execute the definition body if there is one
		if (definition_body != null) {
			addStackItem(k); // in case the structure is referenced by Expressions in the definition body
			definition_body.execute();
			removeStackItem(k, this);
		}
		return new Value().set(k);
	}


	String getStructureType ()  { return (structure_type instanceof String) ? (String) structure_type : null; }


	boolean print (final PrintStream out, final String indentation, final boolean line_start, final int depth)  {
		out.print((line_start?indentation:"") + ((structure_type instanceof String)?(String)structure_type:("copy("+((Expression)structure_type).toString()+")")));
		if (definition_body == null)
			return false;
		else {
			out.println();
			return definition_body.print(out, indentation, true, depth-1);
		}
	}


	void replace (String original, boolean starts_with, String replacement)  {
		if (definition_body != null)
			definition_body.replace(original, starts_with, replacement);
		if (structure_type instanceof Expression)
			((Expression)structure_type).replace(original, starts_with, replacement);
	}
}