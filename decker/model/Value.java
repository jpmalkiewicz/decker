package decker.model;


/** A Value holds a value that is generated during script execution or held in a variable.
*   Value objects can hold any type of value. */
public final class Value
{
	public final static int CONSTANT = 0, BOOLEAN = 1, INTEGER = 2, STRING = 3, STRUCTURE = 4, FUNCTION = 5, REAL = 6, ARRAY = 7;
	private final static String[] TYPE_NAME = { "CONSTANT", "BOOLEAN", "INTEGER", "STRING", "STRUCTURE", "FUNCTION", "REAL", "ARRAY" };

	private static Structure global_values;



	private int type;
	private boolean bool;
	private int integer;
	private double real;
	private Object object;

	private Structure enclosing_structure;


	/** TEST method */
	static Structure getGlobalValues () {
		return global_values;
	}


	static void setGlobalValues (final Structure _global_values) {
		global_values = _global_values;
	}




	public Value ()  {
		type = CONSTANT;
		object = "UNDEFINED";
	}


	public Value (final Structure _visible_structure)  {
		type = CONSTANT;
		object = "UNDEFINED";
		enclosing_structure = _visible_structure;
	}


// set    ***************************************************************************************************************


	public Value set (final Value value)  {
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


	void setEnclosingStructure (final Structure s)  {
		enclosing_structure = s;
	}


// equals    ************************************************************************************************************


	public boolean equals (final Value value)  {
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


	public boolean equals (final boolean b)  {
		return type == BOOLEAN && bool == b;
	}


	public boolean equals (final int i)  {
		if (type == REAL) {
			final Value rer = ScriptNode.getValue("REAL_EQUAL_RANGE");
			final double range = (rer==null||rer.type!=REAL) ? 0.000000001 : rer.real;
			final double difference = real-i;
			return difference < range && -difference < range;
		}
		return integer == i && type == INTEGER;
	}


	public boolean equals (final double r)  {
		if (type != REAL && type != INTEGER)
			return false;
		final Value rer = ScriptNode.getValue("REAL_EQUAL_RANGE");
		final double range = (rer==null||rer.type!=REAL) ? 0.000000001 : rer.real;
		final double difference = r - ((type==REAL)?real:integer);
		return difference < range && -difference < range;
	}


	public boolean equals (final String s)  {
		return type == STRING && object.equals(s);
	}


	public boolean equals (final Structure s)  {
		return object == s; // && type == STRUCTURE
	}


	public boolean equals (final ArrayWrapper array)  {
		return object == array; // && type == ARRAY
	}


	public boolean equalsConstant (final String c) {
		return type == CONSTANT && ((String)object).equals(c);
	}


// "get"    *************************************************************************************************************


	public Value get (final String name)  {
		if(type != STRUCTURE) {
			if (type != ARRAY)
				throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to STRUCTURE");
			else {
				final Value[] a = ((ArrayWrapper)object).array;
				if (name.equals("size"))
					return new Value().set(a.length);
				throw new RuntimeException("Cannot use get(\""+name+"\") with an ARRAY");
			}
		}
		return ((Structure)object).get(name);
	}


	public Value get (int index) {
		if (type != ARRAY)
			throw new RuntimeException("Cannot use an array index ("+TYPE_NAME[type]+") with a "+TYPE_NAME[type]);
		else {
			final Value[] a = ((ArrayWrapper)object).array;;
			return ( index < 0 || index >= a.length ) ? new Value() : a[index]; // returns UNDEFINED if the index doesn't exist
		}
	}


	public Structure getEnclosingStructure ()  { return enclosing_structure; }


	public boolean isGlobal ()  { return enclosing_structure == global_values; }


	public int type ()  { return type; }


	public String typeName ()  { return TYPE_NAME[type]; }


	public final static String typeName (final int type)  { return TYPE_NAME[type]; }


	public Value[] array ()  {
		if (type != ARRAY)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to ARRAY");
		return ((ArrayWrapper)object).array;
	}


	public ArrayWrapper arrayWrapper ()  {
		if (type != ARRAY)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to ARRAY");
		return (ArrayWrapper) object;
	}


	public boolean bool ()  {
		if (type != BOOLEAN)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to BOOLEAN");
		return bool;
	}


	public int integer ()  {
		if (type != INTEGER)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to INTEGER");
		return integer;
	}


	public double real ()  {
		if (type != REAL) {
			if (type == INTEGER)
				return integer;
			else
				throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to REAL");
		}
		return real;
	}


	public String string ()  {
		if (type != STRING)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to STRING");
		return (String) object;
	}


	public Structure structure ()  {
		if (type != STRUCTURE)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to STRUCTURE");
		return (Structure) object;
	}


	public String constant ()  {
		if (type != STRUCTURE)
			throw new RuntimeException("Cannot convert "+TYPE_NAME[type]+" to STRUCTURE");
		return (String) object;
	}


	public Function function ()  {
		if(type != FUNCTION)
			throw new RuntimeException("Cannot convert the "+TYPE_NAME[type]+" \""+toString()+"\" to FUNCTION");
		return (Function) object;
	}


	/** string representation of this value when displayed by a print() call. all it does it put quoates around the value if it is a STRING, otherwise it just returns the same as toString() */
	public String toStringForPrinting () { return (type!=STRING) ? toString() : ("\""+((String)object)+"\""); }


	public String toString ()  { return (type==INTEGER) ? (integer+"") : ( (type==BOOLEAN) ? (bool+"") : ( (type==REAL) ? (real+"") : ( (type==ARRAY) ? "ARRAY" : object.toString() ))); }
}