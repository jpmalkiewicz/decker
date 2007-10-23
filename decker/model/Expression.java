package decker.model;
import java.io.PrintStream;



public class Expression extends ScriptNode
{
	final static int STRUCTURE_DEFINITION = 1, ARRAY_DEFINITION = 3, NOT_AN_OPERATOR = 4, VARIABLE = 5, CONSTANT = 6, BRACKET = 7, GLOBAL_VALUE = 8, MEMBER = 9, ARRAY_INDEX = 10, FUNCTION_CALL = 11, RAW_VALUE = 12, MULTIPLY = 13, DIVIDE = 14, NOT = 15, ADD = 16, SUBSTRACT = 17, NEGATIVE = 18, GREATER = 19, LESS = 20, GREATER_OR_EQUAL = 21, LESS_OR_EQUAL = 22, NOT_EQUAL = 23, EQUAL = 24, AND = 25, OR = 26, CONDITIONAL_COLON = 27, CONDITIONAL = 28;
	final static int[] OPERATOR_PRIORITY = new int[29];
	// the string below contains all the one character operators, with OPERATOR_STRING.charAt(x) having the operator id x
	final static String OPERATOR_STRING = "(.[*/!+-><:?@";
	final static int[] OPERATOR_STRING_ID = { BRACKET, MEMBER, ARRAY_INDEX, MULTIPLY, DIVIDE, NOT, ADD, SUBSTRACT, GREATER, LESS, CONDITIONAL_COLON, CONDITIONAL, GLOBAL_VALUE };
	final static String OPERATOR_STRING_2 = "==!=<>>=<=&&||";
	final static int[] OPERATOR_STRING_2_ID = { EQUAL, NOT_EQUAL, NOT_EQUAL, GREATER_OR_EQUAL, LESS_OR_EQUAL, AND, OR };

	private int operator;
	private Expression first_operand;
	private Expression second_operand;
	private final Value operator_element = new Value(); // contains the constant, variable name or operator (as a string), depending on the expression type


	static {
		// set the operator priorities
		for (int i = 0; i < OPERATOR_PRIORITY.length; i++)
			OPERATOR_PRIORITY[i] = i;

		for (int i = 0; i <= BRACKET; i++)
			OPERATOR_PRIORITY[i] = 0; // leaf expressions

		OPERATOR_PRIORITY[ARRAY_INDEX]   = MEMBER;
		OPERATOR_PRIORITY[FUNCTION_CALL] = MEMBER;

		OPERATOR_PRIORITY[DIVIDE] = MULTIPLY;

		OPERATOR_PRIORITY[SUBSTRACT] = ADD;
		OPERATOR_PRIORITY[NEGATIVE] = ADD;
	}


	/** returns the id of the operator contained in the string
	*   assumes that the "operator" parameter contains a single script element */
	static int operatorID (final String operator)  {
		if(operator == null || "]){}=".indexOf(operator) > -1 || Global.COMMANDS.indexOf(" "+operator+" ") > -1)
			return NOT_AN_OPERATOR;

		// check whether it's a one character operator
		int index = OPERATOR_STRING.indexOf(operator);
		if(index > -1 && operator.length() == 1)
			return OPERATOR_STRING_ID[index];

		// check whether it's a two character operator
		index = OPERATOR_STRING_2.indexOf(operator);
		if(index > -1 && (index&1) == 0 && operator.length() == 2)
			return OPERATOR_STRING_2_ID[index/2];

		// check whether it's the & operator
		if(operator.equals("&"))
			return RAW_VALUE;

		if(operator.startsWith("\""))
			return CONSTANT;

		try{
			Double.parseDouble(operator);
			return CONSTANT;
		} catch(NumberFormatException ex) {}

		if(operator.equals("true") || operator.equals("false"))
			return CONSTANT;

		return VARIABLE;
	}


	/** if the second parameter is true the method returns FUNCTION_CALL instead of BRACKET */
	static int operatorID (final String operator, final boolean operatorExpression)  {
		final int ret = operatorID(operator);
		if (operatorExpression && ret == BRACKET)
			return FUNCTION_CALL;
		return ret;
	}


	/** this constructor is solely used by ArrayDefinition */
	Expression (final String _script_name, final int _script_line, final int _script_column)  {
		super(_script_name, _script_line, _script_column);
	}


	Expression (final String operator_element, final String _script_name, final int _script_line, final int _script_column)  {
		this(operator_element, _script_name, _script_line, _script_column, false, null, null);
	}


	Expression (final String operator_element, final String _script_name, final int _script_line, final int _script_column, final Expression[] expression_stack, final int[] expression_stack_top)  {
		this(operator_element, _script_name, _script_line, _script_column, false, expression_stack, expression_stack_top);
	}


	Expression (final String _operator_element, final String _script_name, final int _script_line, final int _script_column, final boolean operator_expression, final Expression[] expression_stack, final int[] expression_stack_top)  {
		super(_script_name, _script_line, _script_column);

		operator = operatorID(_operator_element, operator_expression);
		if (operator == NOT_AN_OPERATOR)
			throwException(_operator_element+" is not a valid operator");

		if (operator == CONSTANT) {
			if (_operator_element.startsWith("\""))
				operator_element.set(_operator_element.substring(1, _operator_element.length()-1));
			else if (_operator_element.equals("true") || _operator_element.equals("false"))
				operator_element.set(_operator_element.equals("true"));
			else {
				// determine whether it's an integer or a real number
				double d = Double.parseDouble(_operator_element);
				if (d == (int) d)
					operator_element.set((int)d);
				else
					operator_element.set(d);
			}
		}
		else
			operator_element.set(_operator_element);

		// move this expression to the right place in the current Expression tree - the lower its priority the higher it gets moved
		if (expression_stack != null) {
try {
			int index = expression_stack_top[0];
			if(OPERATOR_PRIORITY[operator] == 0) {
				// do nothing. it's a leaf operator and does not move up within the expression tree
			}
			else if (operator == CONDITIONAL_COLON) {
				// move up through the tree until you find the corresponding ? operator. stop when the ? operator is found, the top of the expression_stack is reached, the top of the expression is reached or there is an enclosing Expression (it puts a dummy ScriptNode that is not an Expression on the expression_stack while its contents are parsed)
				if (index >= 0) {
					do {
						first_operand = expression_stack[index];
						expression_stack[index] = null;
						index--;
					} while (index > -1 && first_operand.operator != CONDITIONAL);
				}
				if (first_operand == null || first_operand.operator != CONDITIONAL)
					throwException("found the : but not the corresponding ? of the a?b:c operator");
			}
			else { // all other expressions are sorted by their order of priority
				// the ?'s of a?b:c are executed right before left so a ? that is added is stronger than an existing one
				final int priority = OPERATOR_PRIORITY[operator] - ((operator==CONDITIONAL) ? 1 : 0);
				// now find the place in the expression tree where this expression belongs
				while (index > -1 && OPERATOR_PRIORITY[expression_stack[index].operator] <= priority) {
					first_operand = expression_stack[index];
					expression_stack[index] = null;
					index--;
				}
			}
			// add this Expression to the Expression it's nested in if there is one
			if (index > -1)
				expression_stack[index].addExpression(this);
			expression_stack[++index] = this;
			expression_stack_top[0] = index;
} catch (Throwable t) {
	t.printStackTrace();
	throwException(t.toString());
}
		}
	}


	/** copies the Expression e */
	private Expression (final Expression e)  {
		super(e);

		operator = e.operator;
		if(e.first_operand != null)
			first_operand = e.first_operand.copy();
		if(e.second_operand != null)
			second_operand = e.second_operand.copy();
		operator_element.set(e.operator_element);
	}


	void addExpression (final Expression expression)  {
		// if this expression is the GLOBAL_VALUE operator @, make sure that the added expression describes a variable
		if ( operator == GLOBAL_VALUE && expression.operator != VARIABLE)
			throwException("the "+operator_element.toString()+" operator requires the name of a global value as its operand");
		if (operator == RAW_VALUE && expression.operator != VARIABLE && expression.operator != MEMBER && expression.operator != ARRAY_INDEX)
			throwException("the "+operator_element.toString()+" operator requires a variable as its operand");
		// add the expression to this expression as the first or second operand
		if (first_operand != null && operator != NEGATIVE && operator != NOT && operator != BRACKET && operator != GLOBAL_VALUE && operator != RAW_VALUE)
			second_operand = expression;
		else
			first_operand = expression;
	}


	Expression copy ()  { return new Expression(this); }


	public Value execute ()  {
		final Value return_value = new Value();

		// most operators use the value of their two operands. fetch them unless they won't be used
		Value a = null, b = null;
		if (operator != MEMBER && operator != GLOBAL_VALUE) {
			if(first_operand != null) {
				a = first_operand.execute();
				if (a == null && operator != CONDITIONAL_COLON)
					a = new Value();
			}
			else if (operator != VARIABLE && operator != CONSTANT)
				throwException("first operand missing");
			if(second_operand != null && operator != AND && operator != OR && operator != CONDITIONAL_COLON && operator != CONDITIONAL) {
				b = second_operand.execute();
				if (b == null)
					b = new Value();
			}
		}
		final int at = (a!=null)?a.type():-1;
		final int bt = (b!=null)?b.type():-1;

		// now execute the operator
		switch(operator) {
			case VARIABLE :
					final Value r = getVar(operator_element.string());
					final Value r2 = stack[RULESET_STACK_SLOT].get("STRUCTURE_TYPES").get(operator_element.string());
					final Value r3 = stack[ENGINE_STACK_SLOT].get("STRUCTURE_TYPES").get(operator_element.string());
				return ( r != r2 && r != r3 ) ? r : new Value().set(new Structure(operator_element.string())); // return a new instance if the found value is a structure type
			case CONSTANT :
					return_value.set(operator_element);
				break;
			case BRACKET :
				return a;
			case ARRAY_INDEX :
					if(at != Value.ARRAY)
						throwException("The array index operator [] requires a variable containing an array in front of the brackets");
					if(bt != Value.INTEGER && !b.toString().equals(""))
						throwException("The array index operator [] requires an integer inside the brackets, or empty brackets, not the value "+b.toString()+" ("+b.typeName()+")");
				return (bt==Value.INTEGER) ? a.get(b.integer()) : a.get(b.toString());
			case GLOBAL_VALUE :
					final Value gv = stack[RULESET_STACK_SLOT].get("GLOBAL_VALUES").get(first_operand.toString());
					if (gv != null)
						return gv;
				break;
			case RAW_VALUE :
					return_value.set(a);
				break;
			case MEMBER :
					if (second_operand.operator != VARIABLE)
						throwException("The . operator requires a variable name as the right operand.");
					final String variable_name = second_operand.operator_element.string();
					// if it's a "structure_type.this" pseudo member expression, try to find a structure of that type on the stack
					if (first_operand.operator == VARIABLE) {
						final String s = first_operand.operator_element.string();
						for (int i = stack_size; --i >= 0; ) {
							if (stack[i].get("structure_type").equals(s)) {
								a = new Value().set(stack[i]);
								break;
							}
						}
					}
					if (a == null)
						a = first_operand.execute();
					if (a == null)
						throwException(first_operand+" does not exist");
					if(a.type() != Value.STRUCTURE) {
							if (a.type() == Value.ARRAY && variable_name.equals("size")) {
								return new Value().set(a.array().length);
							}
							return new Value(); // returns UNDEFINED to make scripting easier and to enable scripts to check whether a variable contains a structure ( in that case the following expression is true :   var.structure_type != UNDEFINED )
						}
					if (variable_name.equals("this"))
						return a;
					final Value val = a.get(variable_name);
				return (val!=null) ? val : new Value();
			case MULTIPLY :
					if(at == Value.INTEGER && bt == Value.INTEGER)
						return_value.set(a.integer() * b.integer());
					else if (( at == Value.REAL || at == Value.INTEGER )&&( bt == Value.REAL || bt == Value.INTEGER ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) * ((bt==Value.REAL)?b.real():b.integer()));
					else
						throwException("The * operator requires two integers or real numbers as operands, not "+Value.typeName(at)+" ("+a+") and "+Value.typeName(bt)+" ("+b+")");
				break;
			case DIVIDE :
					if(b.equals(0))
						throwException("The second operand of the / operator must not be zero.");
					if(at == Value.INTEGER && bt == Value.INTEGER)
						return_value.set(a.integer() / b.integer());
					else if (( at == Value.REAL || at == Value.INTEGER )&&( bt == Value.REAL || bt == Value.INTEGER ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) / ((bt==Value.REAL)?b.real():b.integer()));
					else
						throwException("The / operator requires two integers or real numbers as operands.");
				break;
			case NOT :
					if(at != Value.BOOLEAN)
						throwException("The ! operator requires a boolean as its operand.");
					return_value.set(!a.bool());
				break;
			case ADD :
					if(at == Value.ARRAY && bt == Value.ARRAY) {
						// create the new array from the old ones
						final Value[] aa = a.array(), bb = b.array(), array = new Value[aa.length+bb.length];
						final int asize = aa.length;
						for (int i = bb.length; --i >= 0; )
							array[i+asize] = bb[i];
						for (int i = asize; --i >= 0; )
							array[i] = aa[i];
						return_value.set(new ArrayWrapper(array));
					}
					else if (at == Value.INTEGER && bt == Value.INTEGER)
						return_value.set(a.integer() + b.integer());
					else if (( at == Value.REAL || at == Value.INTEGER )&&( bt == Value.REAL || bt == Value.INTEGER ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) / ((bt==Value.REAL)?b.real():b.integer()));
					else
						return_value.set(a.toString() + b.toString());
				break;
			case SUBSTRACT :
					if(at == Value.INTEGER && bt == Value.INTEGER)
						return_value.set(a.integer() - b.integer());
					else if (( at == Value.REAL || at == Value.INTEGER )&&( bt == Value.REAL || bt == Value.INTEGER ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) - ((bt==Value.REAL)?b.real():b.integer()));
					else
						throwException("The - operator requires two integers or real numbers as operands,  "+a+" ("+a.typeName()+")  -  "+b+" ("+a.typeName()+")  doesn't  work");
				break;
			case NEGATIVE :
					if(at == Value.INTEGER)
						return_value.set(-a.integer());
					else if (at == Value.REAL)
						return_value.set(-a.real());
					else
						throwException("The - prefix requires an integer or a real number as its operand.");
				break;
			case GREATER :
					if (( at == Value.INTEGER || at == Value.REAL )&&( bt == Value.INTEGER || bt == Value.REAL ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) > ((bt==Value.REAL)?b.real():b.integer()));
					else if (at != bt || at != Value.STRING)
						return_value.set(false);
					else // it's two strings
						return_value.set(a.string().compareTo(b.string()) > 0);
				break;
			case LESS :
					if (( at == Value.INTEGER || at == Value.REAL )&&( bt == Value.INTEGER || bt == Value.REAL ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) < ((bt==Value.REAL)?b.real():b.integer()));
					else if (at != bt || at != Value.STRING)
						return_value.set(false);
					else // it's two strings
						return_value.set(a.string().compareTo(b.string()) < 0);
				break;
			case EQUAL :
					return_value.set(a.equals(b));
				break;
			case NOT_EQUAL :
					return_value.set(!a.equals(b));
				break;
			case GREATER_OR_EQUAL :
					if (a.equals(b))
						return_value.set(true);
					else if (( at == Value.INTEGER || at == Value.REAL )&&( bt == Value.INTEGER || bt == Value.REAL ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) > ((bt==Value.REAL)?b.real():b.integer()));
					else if (at == Value.STRING && bt == Value.STRING)
						return_value.set(a.string().compareTo(b.string()) >= 0);
					else
						return_value.set(false);
				break;
			case LESS_OR_EQUAL :
					if (a.equals(b))
						return_value.set(true);
					else if (( at == Value.INTEGER || at == Value.REAL )&&( bt == Value.INTEGER || bt == Value.REAL ))
						return_value.set(((at==Value.REAL)?a.real():a.integer()) < ((bt==Value.REAL)?b.real():b.integer()));
					else if (at == Value.STRING && bt == Value.STRING)
						return_value.set(a.string().compareTo(b.string()) <= 0);
					else
						return_value.set(false);
				break;
			case AND :
					if(at != Value.BOOLEAN || !a.bool())
						return_value.set(false);
					else {
						b = second_operand.execute();
						return_value.set(b.type() == Value.BOOLEAN && b.bool());
					}
				break;
			case OR :
					if(at == Value.BOOLEAN && a.bool())
						return_value.set(true);
					else {
						b = second_operand.execute();
						return_value.set(b.type() == Value.BOOLEAN && b.bool());
					}
				break;
			case CONDITIONAL_COLON : // the : of the a?b:c operator. it's first operand is an expression with the ? operator that belongs to this :
				return (a!=null) ? a : second_operand.execute();
			case CONDITIONAL : // the : of the a?b:c operator. it's first operand is an expression with ? operator
					if (at != Value.BOOLEAN)
						throwException("a?b:c requires a boolean value in a. "+first_operand+" is of type "+Value.typeName(at)+" ("+a+")");
				return a.bool() ? second_operand.execute() : null; // returning null will lead to an error if the parent operator is not the : operator
		}

		return return_value;
	}


	Expression getFirstOperand ()  { return first_operand; }
	int getOperator ()  { return operator; }
	Value getOperatorElement ()  { return operator_element; }
	Expression getSecondOperand ()  { return second_operand; }


	private Value getVar (final String name) {
		final Value ret = getVariable(name);
		return (ret!=null) ? ret : new Value();
	}


	boolean print (final PrintStream out, final String indentation, boolean line_start)  {
		boolean ret = false;
		switch(operator) {
			case VARIABLE :
			case CONSTANT :
					if (line_start)
						out.print(indentation);
					if(operator != CONSTANT || operator_element.type() != Value.STRING)
						out.print(operator_element.toString());
					else
						out.print("\""+operator_element.toString()+"\"");
				break;
			case BRACKET :
					if (line_start)
						out.print(indentation);
					out.print("(");
					line_start = print(out, indentation, false, first_operand);
					out.print((line_start?indentation:"") + ")");
				break;
			case ARRAY_INDEX :
					line_start = print(out, indentation, line_start, first_operand);
					out.print((line_start?indentation:"") + "[");
					line_start = print(out, indentation, false, second_operand);
					out.print((line_start?indentation:"") + "]");
				break;
			case MEMBER :
					line_start = print(out, indentation, line_start, first_operand);
					out.print((line_start?indentation:"") + ".");
					ret = print(out, indentation, false, second_operand);
				break;
			case NOT :
			case NEGATIVE :
			case GLOBAL_VALUE :
					out.print((line_start?indentation:"") + operator_element.toString());
					ret = print(out, indentation, false, first_operand);
				break;
			case RAW_VALUE :
					out.print((line_start?indentation:""));
					ret = print(out, indentation, false, first_operand);
				break;
			case FUNCTION_CALL:
					out.print((line_start?indentation:"")+toString());
				break;
			default :
					line_start = print(out, indentation, line_start, first_operand);
					out.print((line_start?indentation:" ") + operator_element.toString() + " ");
					ret = print(out, indentation, false, second_operand);
				break;
		}
		return ret;
	}


	void replace (String original, boolean starts_with, String replacement)  {
		if(operator == CONSTANT && operator_element.type() == Value.STRING &&( operator_element.string().equals(original) ||( starts_with && operator_element.string().startsWith(original) )))
			operator_element.set(replacement);
		if(first_operand != null)
			first_operand.replace(original, starts_with, replacement);
		if(second_operand != null)
			second_operand.replace(original, starts_with, replacement);
	}


	/** used by ArrayDefinition and FunctionCall */
	void setOperator (final int operator)  { this.operator = operator; }
}