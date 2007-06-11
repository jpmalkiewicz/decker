package decker.model;


/** A Value holds a value that is generated during script execution or held in a variable.
*   Value objects can hold any type of value. */
public final class Value
{
	public final static int CONSTANT = 0, BOOLEAN = 1, INTEGER = 2, STRING = 3, STRUCTURE = 4, FUNCTION = 5;
	private final static String[] TYPE_NAME = {"CONSTANT", "BOOLEAN", "INTEGER", "STRING", "STRUCTURE", "FUNCTION"};



	private int type;
	private boolean bool;
	private int integer;
	private Object object;

	private Structure visible_structure;


	public Value ()  {
		type = CONSTANT;
		object = "UNDEFINED";
	}


	public Value (final Structure _visible_structure)  {
		type = CONSTANT;
		object = "UNDEFINED";
		visible_structure = _visible_structure;
	}


	/** removes all object references from this Value, to make garbage collection easier */
	void destroy ()  {
		object = null;
		visible_structure = null;
	}


	private Value executeExpression ()  {
		return FunctionCall.executeFunctionCall(object, null, (visible_structure==null||!visible_structure.variablesSeeEachOther()) ? null : visible_structure);
	}


// set    ***************************************************************************************************************


	public Value set (Value value)  {
		// if the value contains a function, execute the function. it's okay for the function to return another function. such a returned function will not be executed but stored in this Value
		if (value.type == FUNCTION)
			value = value.executeExpression();
		if (!equalsAbsolute(value)) {
			type = value.type;
			bool = value.bool;
			integer = value.integer;
			object = value.object;
			if(visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


	public Value set (final boolean b)  {
		if (type != BOOLEAN || bool != b) {
			type = BOOLEAN;
			bool = b;
			object = null; // this is done to get rid off references to obsolete objects
			if (visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


	public Value set (final int i)  {
		if (type != INTEGER || integer != i) {
			type = INTEGER;
			integer = i;
			object = null; // this is done to get rid off references to obsolete objects
			if (visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


	public Value set (final String s)  {
		if (type != STRING || !((String)object).equals(s)) {
			type = STRING;
			object = s;
			if (visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


	public Value set (final Structure s)  {
		if (object != s)  { // no need to check whether type is not STRUCTURE here
			type = STRUCTURE;
			object = s;
			if (visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


	public Value set (final Function f)  {
		if (object != f) { // no need to check whether type is not FUNCTION here
			type = FUNCTION;
			object = f;
			if (visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


	public Value setConstant (final String constant)  {
		if (type != CONSTANT || !((String)object).equals(constant)) {
			type = CONSTANT;
			object = constant;
			if (visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


	/** sets this Value to value without implicitly calling the function if v contains one */
	public Value setDirectly (final Value value, final boolean test_triggers)  {
		if (!equalsAbsolute(value)) {
			type = value.type;
			bool = value.bool;
			integer = value.integer;
			object = value.object;
			if (test_triggers && visible_structure != null && !visible_structure.get("structure_type").equals("LOCAL")) // otherwise it's a temporary Value object and there cannot possibly a listener for it
				Global.testTriggers();
		}
		return this;
	}


// add    ***************************************************************************************************************


	public void add (final Value value)  {
		if(type == INTEGER && value.type == INTEGER)
			set(integer + value.integer);
		else
			throw new RuntimeException("can only add up two integers");
	}


	public void add (final int i)  {
		if(type == INTEGER)
			set(integer + i);
		else
			throw new RuntimeException("can only add up two integers");
	}


// equals    ************************************************************************************************************


	public boolean equalsAbsolute (final Value value)  {
		if(type != value.type)
			return false;
		switch(type) {
			case BOOLEAN :
				return bool == value.bool;
			case INTEGER :
				return integer == value.integer;
			case CONSTANT :
			case STRING :
				return ((String)object).equals((String)value.object);
			default : // STRUCTURE or FUNCTION
				return object == value.object;
		}
	}



	public boolean equals (Value value)  {
		final Value v = (type==FUNCTION) ? executeExpression() : this;
		if (value.type == FUNCTION)
			value = value.executeExpression();
		if(v.type != value.type)
			return false;
		switch(type) {
			case BOOLEAN :
				return v.bool == value.bool;
			case INTEGER :
				return v.integer == value.integer;
			case CONSTANT :
			case STRING :
				return ((String)v.object).equals((String)value.object);
			default : // STRUCTURE or FUNCTION
				return v.object == value.object;
		}
	}


	public boolean equals (final boolean b)  {
		if (type == FUNCTION) {
			final Value v = executeExpression();
			return v.type == BOOLEAN && v.bool == b;
		}
		return type == BOOLEAN && bool == b;
	}


	public boolean equals (final int i)  {
		if (type == FUNCTION) {
			final Value v = executeExpression();
			return v.type == INTEGER && v.integer == i;
		}
		return type == INTEGER && integer == i;
	}


	public boolean equals (final String s)  {
		if (type == FUNCTION) {
			final Value v = executeExpression();
			return v.type == STRING && v.object.equals(s);
		}
		return type == STRING && object.equals(s);
	}


	public boolean equals (final Structure s)  {
		if (type == FUNCTION) {
			final Value v = executeExpression();
			return v.type == STRUCTURE && v.object == s;
		}
		return type == STRUCTURE && object == s;
	}


	public boolean equalsConstant (final String c) {
		if (type == FUNCTION) {
			final Value v = executeExpression();
			return v.type == CONSTANT && v.object.equals(c);
		}
		return type == CONSTANT && object.equals(c);
	}


// "get"    *************************************************************************************************************


	public Value get (final String name)  {
		final Value v = (type!=FUNCTION) ? this : executeExpression();
		if(v.type != STRUCTURE)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRUCTURE");
		return ((Structure)v.object).get(name);
	}


	public Structure getEnclosingStructure ()  { return visible_structure; }


	public Value getValue ()  { return (type != FUNCTION) ? this : executeExpression(); }


	public boolean isFunction () { return type == FUNCTION; }


	public int type ()  { return (type!=FUNCTION) ? type : executeExpression().type; }


	public int typeDirect ()  { return type; }


	public String typeName ()  { return TYPE_NAME[type()]; }


	public String typeNameDirect ()  { return TYPE_NAME[type]; }


	public boolean bool ()  {
		final Value v = (type!=FUNCTION) ? this : executeExpression();
		if (v.type != BOOLEAN)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to BOOLEAN");
		return v.bool;
	}


	public int integer ()  {
		final Value v = (type!=FUNCTION) ? this : executeExpression();
		if (v.type != INTEGER)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to INTEGER");
		return v.integer;
	}


	public String string ()  {
		final Value v = (type!=FUNCTION) ? this : executeExpression();
		if (v.type != STRING)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRING");
		return (String) v.object;
	}


	public Structure structure ()  {
		final Value v = (type!=FUNCTION) ? this : executeExpression();
		if (v.type != STRUCTURE)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRUCTURE");
		return (Structure) v.object;
	}


	public String constant ()  {
		final Value v = (type!=FUNCTION) ? this : executeExpression();
		if (v.type != STRUCTURE)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRUCTURE");
		return (String) v.object;
	}


	public Function function ()  {
		if(type != FUNCTION)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to FUNCTION");
		return (Function) object;
	}


	public String toString ()  { return (type==BOOLEAN) ? (bool+"") : ((type==INTEGER) ? (integer+"") : object.toString()); }
}