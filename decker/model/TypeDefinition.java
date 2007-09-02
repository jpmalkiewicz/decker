package decker.model;
import java.io.PrintStream;



final class TypeDefinition extends ScriptNode
{
	private String structure_type;
	private String extends_structure_type;
	private Object[] definition_body;


	/** _structure_type is either a String or, if this is a copy() command, an Expression */
	TypeDefinition (final String _structure_type, final String _extends_structure_type, final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
		structure_type = _structure_type;
		extends_structure_type = _extends_structure_type;
	}


	/** solely used by copy() */
	private TypeDefinition (final TypeDefinition original)  {
		super(original.getScriptName(), original.getScriptLine(), original.getScriptColumn());
		structure_type = original.structure_type;
		extends_structure_type = original.extends_structure_type;
		if (original.definition_body != null) {
			final int count = original.definition_body.length;
			definition_body = new Object[count];
			for (int i = count; --i >= 0; )
				if (original.definition_body[i] instanceof String)
					definition_body[i] = original.definition_body[i];
				else
					definition_body[i] = new Object[]{ ((Object[])original.definition_body[i])[0], ((Expression)((Object[])original.definition_body[i])[1]).copy() };
		}
	}


	public TypeDefinition copy ()  { return new TypeDefinition(this); }


	public Value execute () {
		// create the new structure
		final Structure k = (extends_structure_type!=null) ? new Structure(extends_structure_type) : new Structure(""); // using "" will keep the Structure that holds the new structure type definition from instantiating the old type definition (if there is one for this type)
		k.get("structure_type").set(structure_type);
		// execute the definition body if there is one
		if (definition_body != null) {
			addStackItem(k); // in case the structure is referenced by Expressions in the definition body
			for (int i = 0; i < definition_body.length; i++) {
				final String varname = (String) ((definition_body[i] instanceof String) ? definition_body[i] : ((Object[])definition_body[i])[0]);
				final Value v = k.add(varname);
				if (definition_body[i] instanceof Object[]) {
					Object o = ((Object[])definition_body[i])[1];
					if (o instanceof Expression) {
						final Expression e = (Expression)((Object[])definition_body[i])[1];
						if (e.getOperator() == Expression.FETCH_VALUE)
							v.setDirectly(e.execute(), false);
						else
							v.set(e.execute());
					}
					else // (o instanceof Function)
						v.set((Function)o);
				}
			}
			removeStackItem(k, this);
		}
		// add the structure type to the ruleset
		stack[RULESET_STACK_SLOT].get("STRUCTURE_TYPES").structure().add(structure_type).set(k);
		return null;
	}


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  {
		out.println((line_start?indentation:"") + structure_type + ((extends_structure_type==null)?"":(" extends "+extends_structure_type)));
		if (definition_body != null) {
			final String ind = indentation + Global.BLOCK_INDENT;
			for (int i = 0; i < definition_body.length; i++) {
				if (definition_body[i] instanceof String)
					out.println(ind + (String)definition_body[i]);
				else {
					out.print(ind + (String)((Object[])definition_body[i])[0] + " = ");
					if ( !((Expression)((Object[])definition_body[i])[1]).print(out, ind, false) )
						out.println();
				}
			}
		}
		return true;
	}


	void replace (String original, boolean starts_with, String replacement)  {
		if (definition_body != null)
			for (int i = 0; i < definition_body.length; i++)
				if (definition_body[i] instanceof Object[])
					((Expression)((Object[])definition_body[i])[0]).replace(original, starts_with, replacement);
	}


	void setDefinitionBody (final Object[] _definition_body)  { definition_body = _definition_body; }
}