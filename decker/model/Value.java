package decker.model;


/** A Value holds a value that is generated during script execution or held in a variable.
*   Value objects can hold any type of value. */
public final class Value
{
	public final static int CONSTANT = 0, BOOLEAN = 1, INTEGER = 2, STRING = 3, STRUCTURE = 4, FUNCTION = 5, REAL = 6, ARRAY = 7, GLOBAL_VALUE = 8;
	private final static String[] TYPE_NAME = { "CONSTANT", "BOOLEAN", "INTEGER", "STRING", "STRUCTURE", "FUNCTION", "REAL", "ARRAY", "GLOBAL_VALUE" };



	private int type;
	private boolean bool;
	private int integer;
	private double real;
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


	private Value executeExpression ()  {
		// this if will only happen if the function stored in this variable is really an @ expression
		if (type == GLOBAL_VALUE) {
			Value v = (Value) ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT].get("GLOBAL_VALUES").get((String)object);
			return (v!=null) ? v : new Value();
		}
		return FunctionCall.executeFunctionCall(object, null, visible_structure);
	}


// set    ***************************************************************************************************************


	public Value set (Value value)  {
		// if the value contains a function, execute the function. it's okay for the function to return another function. such a returned function will not be executed but stored in this Value
		if (value.type == FUNCTION || value.type == GLOBAL_VALUE)
			value = value.executeExpression();
		type = value.type;
		bool = value.bool;
		integer = value.integer;
		real = value.real;
		object = value.object;
		return this;
	}


	public Value set (final boolean b)  {
		type = BOOLEAN;
		bool = b;
		object = null; // this is done to get rid off references to obsolete objects
		return this;
	}


	public Value set (final int i)  {
		type = INTEGER;
		integer = i;
		object = null; // this is done to get rid off references to obsolete objects
		return this;
	}


	public Value set (final double r)  {
		type = REAL;
		real = r;
		object = null; // this is done to get rid off references to obsolete objects
		return this;
	}


	public Value set (final String s)  {
		type = STRING;
		object = s;
		return this;
	}


	public Value set (final Structure s)  {
		type = STRUCTURE;
		object = s;
		return this;
	}


	public Value set (final Function f)  {
		type = FUNCTION;
		object = f;
		return this;
	}


	public Value set (final ArrayWrapper a)  {
		type = ARRAY;
		object = a;
		return this;
	}


	public Value setConstant (final String constant)  {
		type = CONSTANT;
		object = constant;
		return this;
	}


	/** sets this Value to value without implicitly calling the function if v contains one */
	public Value setDirectly (final Value value)  {
		type = value.type;
		bool = value.bool;
		integer = value.integer;
		real = value.real;
		object = value.object;
		return this;
	}


	void setEnclosingStructure (final Structure s)  {
		visible_structure = s;
	}


	public Value setGlobalValue (final String value_name)  {
		type = GLOBAL_VALUE;
		object = value_name;
		return this;
	}


// equals    ************************************************************************************************************


	public boolean equalsAbsolute (final Value value)  {
		if(type != value.type || type == REAL || value.type == REAL) {
			if (( type == INTEGER || type == REAL )&&( value.type == INTEGER || value.type == REAL)) {
				final Value v = ScriptNode.getValue("REAL_EQUAL_RANGE");
				final double range = (v==null||v.type!=REAL) ? 0.000000001 : v.real;
				final double a = (type==INTEGER) ? integer : real;
				final double b = (value.type==INTEGER) ? value.integer : value.real;
				return (a-b<range) && (b-a<range);
			}
			return false;
		}
		switch(type) {
			case BOOLEAN :
				return bool == value.bool;
			case INTEGER :
				return integer == value.integer;
		// case REAL : // REAL is handled in the if clause above
			case CONSTANT :
			case STRING :
				return ((String)object).equals((String)value.object);
			default : // STRUCTURE or FUNCTION or ARRAY or GLOBAL_VALUE
				return object == value.object;
		}
	}



	public boolean equals (Value value)  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (value.type == FUNCTION || value.type == GLOBAL_VALUE)
			value = value.executeExpression();
		if(v.type != value.type || v.type == REAL)
			return v.equalsAbsolute(value);
		switch(type) {
			case BOOLEAN :
				return v.bool == value.bool;
			case INTEGER :
				return v.integer == value.integer;
		// case REAL : // this case is handled above by calling equalsAbsolute for REAL values
			case CONSTANT :
			case STRING :
				return ((String)v.object).equals((String)value.object);
			default : // STRUCTURE or FUNCTION or ARRAY or GLOBAL_VALUE
				return v.object == value.object;
		}
	}


	public boolean equals (final boolean b)  {
		if (type == FUNCTION || type == GLOBAL_VALUE) {
			final Value v = executeExpression();
			return v.type == BOOLEAN && v.bool == b;
		}
		return type == BOOLEAN && bool == b;
	}


	public boolean equals (final int i)  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type == REAL) {
			final Value rer = ScriptNode.getValue("REAL_EQUAL_RANGE");
			final double range = (rer==null||rer.type!=REAL) ? 0.000000001 : rer.real;
			final double difference = v.real-i;
			return difference < range && -difference < range;
		}
		return v.type == INTEGER && v.integer == i;
	}


	public boolean equals (final double r)  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != REAL && v.type != INTEGER)
			return false;
		final Value rer = ScriptNode.getValue("REAL_EQUAL_RANGE");
		final double range = (rer==null||rer.type!=REAL) ? 0.000000001 : rer.real;
		final double difference = r - ((v.type==REAL)?v.real:v.integer);
		return difference < range && -difference < range;
	}


	public boolean equals (final String s)  {
		if (type == FUNCTION || type == GLOBAL_VALUE) {
			final Value v = executeExpression();
			return v.type == STRING && v.object.equals(s);
		}
		return type == STRING && object.equals(s);
	}


	public boolean equals (final Structure s)  {
		if (type == FUNCTION || type == GLOBAL_VALUE) {
			final Value v = executeExpression();
			return v.type == STRUCTURE && v.object == s;
		}
		return type == STRUCTURE && object == s;
	}


	public boolean equals (final ArrayWrapper array)  {
		if (type == FUNCTION || type == GLOBAL_VALUE) {
			final Value v = executeExpression();
			return v.type == ARRAY && v.object == array;
		}
		return type == ARRAY && object == array;
	}


	public boolean equalsConstant (final String c) {
		if (type == FUNCTION || type == GLOBAL_VALUE) {
			final Value v = executeExpression();
			return v.type == CONSTANT && v.object.equals(c);
		}
		return type == CONSTANT && object.equals(c);
	}


// "get"    *************************************************************************************************************


	public Value evaluate ()  { return (type != FUNCTION && type != GLOBAL_VALUE) ? this : executeExpression(); }


	public Value get (final String name)  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if(v.type != STRUCTURE) {
			if (v.type != ARRAY)
				throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRUCTURE");
			else {
				final Value[] a = ((ArrayWrapper)v.object).array;
				if (name.equals("size"))
					return new Value().set(a.length);
				if (name.equals(""))
					return new Value();
				try {
					final int index = Integer.parseInt(name);
					return ( index < 0 || index >= a.length ) ? new Value() : a[index]; // returns UNDEFINED if the index doesn't exist
				} catch (NumberFormatException x) {
					throw new RuntimeException("Cannot convert "+name+" to an array index");
				}
			}
		}
		return ((Structure)v.object).get(name);
	}


	public Value get (int index) {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != ARRAY)
			throw new RuntimeException("Cannot use an array index ("+TYPE_NAME[v.type]+") with a "+TYPE_NAME[v.type]);
		else {
			final Value[] a = ((ArrayWrapper)v.object).array;;
			return ( index < 0 || index >= a.length ) ? new Value() : a[index]; // returns UNDEFINED if the index doesn't exist
		}
	}


	public Structure getEnclosingStructure ()  { return visible_structure; }


	public Value getValue ()  { return (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression(); }


	public int type ()  { return (type!=FUNCTION&&type!=GLOBAL_VALUE) ? type : executeExpression().type; }


	public int typeDirect ()  { return type; }


	public String typeName ()  { return TYPE_NAME[type()]; }


	public final static String typeName (final int type)  { return TYPE_NAME[type]; }


	public String typeNameDirect ()  { return TYPE_NAME[type]; }


	public Value[] array ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != ARRAY)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to ARRAY");
		return ((ArrayWrapper)v.object).array;
	}


	public ArrayWrapper arrayWrapper ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != ARRAY)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to ARRAY");
		return (ArrayWrapper) v.object;
	}


	public boolean bool ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != BOOLEAN)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to BOOLEAN");
		return v.bool;
	}


	public int integer ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != INTEGER)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to INTEGER");
		return v.integer;
	}


	public double real ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != REAL)
			if (v.type == INTEGER)
				return v.integer;
			else
				throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to INTEGER");
		return v.real;
	}


	public String string ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != STRING)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRING");
		return (String) v.object;
	}


	public Structure structure ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != STRUCTURE)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRUCTURE");
		return (Structure) v.object;
	}


	public String constant ()  {
		final Value v = (type!=FUNCTION&&type!=GLOBAL_VALUE) ? this : executeExpression();
		if (v.type != STRUCTURE)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[v.type]+" to STRUCTURE");
		return (String) v.object;
	}


	public Function function ()  {
		if(type != FUNCTION)
			throw new RuntimeException("Cannot convert the "+TYPE_NAME[type]+" \""+toString()+"\" to FUNCTION");
		return (Function) object;
	}


	/** string representation of this value when displayed by a print() call. all it does it put quoates around the value if it is a STRING, otherwise it just returns the same as toString() */
	public String toStringForPrinting () { return (type!=STRING) ? toString() : ("\""+((String)object)+"\""); }


	public String toString ()  { return (type==INTEGER) ? (integer+"") : ( (type==BOOLEAN) ? (bool+"") : ( (type==REAL) ? (real+"") : ( (type==ARRAY) ? "ARRAY" : ( (type==GLOBAL_VALUE) ? ("@"+(String)object) : object.toString() )))); }
}