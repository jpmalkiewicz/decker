package decker.model;
import java.io.*;
import java.util.Locale;



final class ScriptParser extends ScriptReader
{
	private String script_name;

	private int last_expression_line = -1; // the last line of the expression that has last been parsed
	private int block_column; // the column in which the lines of the last parsed block started. -1 if the block was empty
	private boolean inside_loop = false;


	static Script parse (final String file_name, final Reader in)  {
		return new ScriptParser(file_name, in).parseScript();
	}


	static Script parse (final File file)  {
		try{
			return parse(file.getName(), new FileReader(file));
		} catch(IOException ex) {
			System.err.println("File "+file.getName()+" not found");
			throw new RuntimeException("File "+file.getName()+" not found");
		}
	}


	static Expression parseExpression (final String expression)  {
		// the } is needed to let the parser realize the expression has ended without throwing an end of stream exception
		final ScriptParser msr = new ScriptParser(expression, new StringReader(expression+"}"));
		return msr.parseExpression(msr.getLine(),msr.getColumn(),true);
	}


	private ScriptParser (final String _script_name, final Reader in)  {
		super(_script_name, in);
		script_name = _script_name;
	}


	private Expression parseArrayDefinition (final int command_column, final Expression[] expression_stack, final int[] expression_stack_top)  {
		final ArrayDefinition array_definition = new ArrayDefinition(script_name, getLine(), getColumn(), expression_stack, expression_stack_top);
		// check whether there are any entries
		last_expression_line = getLine();
		String s = previewElement();
		final int array_entry_column = getColumn();
		if (array_entry_column <= command_column)
			return array_definition;

		// read the elements of the array definition
		final Expression[] es = new Expression[Global.PARSER_EXPRESSION_STACK_SIZE];
		final int[] est = { -1 };
		while (s != null && getColumn() == array_entry_column) {
			last_expression_line = getLine();
			// parse the expression that defines the next array entry
			array_definition.addExpression(parseExpression(getLine(), array_entry_column, new int[]{ -1 }, es, est, true));
			s = previewElement();
		}
		// make sure the next element doesn't start in an illegal column
		if (s != null && getColumn() != array_entry_column && getColumn() > command_column)
			throwException(s+" should start in column "+array_entry_column + ", or in column "+command_column+" or further left");
		block_column = array_entry_column;
		return array_definition;
	}


	private ScriptNode parseAssignmentCommandOrExpression (final int block_column)  {
		final int line = getLine(); // so we can tell the AssignmentCommand where it started, if this is one
		// parse the expression that will sit on the left side of the = or by itself if it's a function call
		final Expression x = parseExpression(line, block_column, false);
		if (x instanceof FunctionCall)
			return x;
		String s = previewElement();
		// check whether it's an assignment command
		if (s != null && s.equals("=")) {
			readElement();
			final AssignmentCommand ac = new AssignmentCommand(false, false, script_name, line, block_column);
			ac.setVariableExpression(x); // the stuff that sits on the left side of the =
			s = previewElement();
			if(s == null)
				throwException("Expression of FUNCTION definition on the right side of the = expected but end of script found");
			if (getLine() != line)
				throwException("Expression of FUNCTION definition on the right side of the = must start on the same line as the expression on the left side did");
			if(s.equals("FUNCTION"))
				ac.setValueExpression(parseFunctionDefinition(line, getColumn(), line, block_column));
			else
				ac.setValueExpression(parseExpression(line, block_column, true));
			return ac;
		}
		else if (s != null &&( s.equals("--") || s.equals("++") )) {
			readElement();
			return new IncrementDecrementCommand(s, false, x, script_name, line, block_column);
		}
		else { // it's probably a "create variable" command without an initial value
			final AssignmentCommand ac = new AssignmentCommand(false, false, script_name, line, block_column);
			ac.setVariableExpression(x);
			return ac;
		}
	}


	private Block parseBlock (final Block block, final int enclosing_line, final int enclosing_column)  {
		String s = previewElement();
		final int column = getColumn();
		// check whether the block contains any commands
		if (s != null && column > enclosing_column && getLine() != enclosing_line) {
			while(s != null && getColumn() == column)  {
				block.addScriptNode(parseSingleCommand(column));
				s = previewElement();
			}
			// make sure the next command isn't on an illegal column
			if (s != null && getColumn() > enclosing_column)
				throwException(s+" should start in column "+column+", or in or left of column "+enclosing_column);
			block_column = column;
		}
		else
			block_column = -1; // the block was empty
		return block;
	}


	private Expression parseBracket (final String opening_bracket, final int current_line, final int command_column, final int[] expression_column, final Expression[] expression_stack, final int[] expression_stack_top) {
		// if the expression inside the brackets contains an indented block, the ) or ] must sit on the start of a new line
		// otherwise it must follow the expression inside the bracket on the same line
		final Expression ret = new Expression(opening_bracket, script_name, getLine(), getColumn(), expression_stack, expression_stack_top);
		// parse the expression inside the brackets
		final int l = current_line;
		final int b = block_column;
		block_column = -1;
		ret.addExpression(parseExpression(current_line, command_column, expression_column, new Expression[Global.PARSER_EXPRESSION_STACK_SIZE], new int[]{ -1 }, true));
		final String s2 = readElement();
		if(s2 == null ||( opening_bracket.equals("(") && !s2.equals(")") )||( opening_bracket.equals("[") && !s2.equals("]") ))
			throwException((opening_bracket.equals("(")?")":"]")+" expected but "+((opening_bracket==null)?"end of script":s2)+" found");
		// check whether the ) or ] sits on the beginning of a new line
		if (getColumn() == getLineStart()) {
			if (block_column == -1 || getPreviousLineStart() != block_column)
				throwException(s2+" can only sit at the start of a new line if it comes right after an indented block (for example an array definition block) inside the brackets");
			else if (getColumn() >= block_column)
				throwException(s2+" must sit left of the last indented block inside the brackets");
			else if (expression_column[0] != -1) {
				if (getColumn() != expression_column[0])
					throwException(s2+" should sit in column "+expression_column[0]+", not in column "+getColumn());
			}
			else if (getColumn() <= command_column)
				throwException(s2+" should sit right of column "+command_column);
			else
				expression_column[0] = getColumn();
			last_expression_line = getLine();
		}
		// the closing bracket sits on the end of a line. make sure it doesn't sit on the last line of an enclosed block
		else if (block_column > -1 && getLineStart() == block_column)
			throwException(s2+" should sit on the next line, not on the end of a line that belongs to an indented block inside the brackets");
		// clean up
		block_column = b;
		last_expression_line = getLine();
		return ret;
	}


	private BreakCommand parseBreakCommand (final int command_line, final int command_column) {
		readElement();
		if (!inside_loop)
			throwException("break without enclosing loop encountered");
		return new BreakCommand(script_name, command_line, command_column);
	}


	private ConditionalCommand parseConditionalCommand (final int command_line, final int command_column)  {
		final String command = readElement();
		final ConditionalCommand cc = new ConditionalCommand(command, script_name, command_line, command_column);
		cc.setConditionalExpression(parseExpression(command_line, command_column, false));
		if (command.equals("while"))  {
			final boolean was_inside_loop = inside_loop;
			inside_loop = true;
			parseBlock(cc, last_expression_line, command_column);
			inside_loop = was_inside_loop;
		}
		else { // "if" command
			parseBlock(cc, last_expression_line, command_column);
			// check whether there's an else block
			String s = previewElement();
			if (s != null && getColumn() == command_column &&( s.equals("else") || s.equals("elseif") )) {
				final int else_line = getLine();
				readElement();
				// check whether it's an "else if" clause
				if (s.equals("else"))
					s = previewElement();
				if (s != null) {
					if (s.equals("elseif") ||( s.equals("if") && getLine() == else_line ))
						cc.addElseBranch(parseConditionalCommand(else_line, command_column));
					else
						cc.addElseBranch(parseBlock(new Block(script_name, else_line, command_column), else_line, command_column));
				}
			}
		}
		return cc;
	}


	private ConstantDefinitionCommand parseConstantDefinitionCommand (final int command_line, final int command_column)  {
		readElement(); // discard the "command" tag
		int count = 0;
		String[] constant = new String[10];
		// determine whether it's a single line definition or a block definition
		String s = previewElement();
		if (s != null) {
			if (getLine() == command_line)  {
				constant[count++] = readElement();
				// make sure there's only one constant following the tag
				s = previewElement();
				if (getLine() == command_line)
					throwException("you can only have one constant following the 'constant' command on the same line. to define multiple constants through a single command, list them in an indented block below the command");
			}
			else { // the constant command is followed by a block of constants
				final int column = getColumn();
				if (column > command_column)  {
					do {
						// make sure the list of constants can hold another item
						if (count == constant.length) {
							final String[] c = constant;
							constant = new String[constant.length*2];
							System.arraycopy(c, 0, constant, 0, count);
						}
						// add the next constant to the list and check whether there's another one following it
						constant[count++] = readElement();
						s = previewElement();
					} while (s != null && getColumn() == column);
				}
				if (s != null && getColumn() > column)
					throwException(s + " should be starting in column "+column+", column "+command_column+" or further left");
			}
		}
		if (count == 0)
			throwException("the constant command must be followed by at least one constant");
		// create and return the corresponding ConstantDefinitionCommand
		final String[] constant_list = new String[count];
		System.arraycopy(constant, 0, constant_list, 0, count);
		return new ConstantDefinitionCommand(constant_list, script_name, command_line, command_column);
	}


	private Expression parseExpression (final int current_line, final int command_column, final boolean allow_structure_definition_blocks)  {
		return parseExpression(current_line, command_column, new int[]{ -1 }, new Expression[Global.PARSER_EXPRESSION_STACK_SIZE], new int[]{ -1 }, allow_structure_definition_blocks);
	}


	private Expression parseExpression (final int current_line, final int command_column, final int[] expression_column, final Expression[] expression_stack, final int[] expression_stack_top, final boolean allow_structure_definition_blocks)  {
		Expression e = null, ret = null;
		String s = readElement();
		if (s == null)
			throwException("variable, constant, -, ! or ( expected but end of script found");
		if (getLine() != current_line)
			throwException(s+" should be on line "+current_line+", not on line "+getLine());

		last_expression_line = current_line; // in case it's the start of the expression
		final int column = getColumn();
		final int operator_id = Expression.operatorID(s);

		switch (Expression.operatorID(s)) {
			case Expression.BRACKET   : e = parseBracket(s, current_line, command_column, expression_column, expression_stack, expression_stack_top); break;
			case Expression.CONSTANT  : e = new Expression(s, script_name, current_line, column, expression_stack, expression_stack_top); break;
			case Expression.SUBSTRACT : // it's an expression that starts with a -
					e = new Expression("-", script_name, current_line, column, expression_stack, expression_stack_top);
					e.setOperator(Expression.NEGATIVE);
					ret = parseExpression(current_line, command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks); // parse the expression that the - negates
				break;
			case Expression.GLOBAL_VALUE :
					e = new Expression(s, script_name, current_line, column, expression_stack, expression_stack_top);
					ret = parseExpression(current_line, command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks); // parse the expression behind the @
				break;
			case Expression.RAW_VALUE :
					e = new Expression(s, script_name, current_line, column, expression_stack, expression_stack_top);
					ret = parseExpression(current_line, command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks); // parse the expression behind the &
				break;
			case Expression.NOT :
					e = new Expression("!", script_name, current_line, column, expression_stack, expression_stack_top);
					ret = parseExpression(current_line, command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks); // parse the expression behind the !
				break;
			case Expression.VARIABLE  :
					if (s.equals("ARRAY")) {
						e = parseArrayDefinition(command_column, expression_stack, expression_stack_top);
						break;
					}
					else if (allow_structure_definition_blocks) {
						// check whether it's a variable followed by a block. in that case it's a structure definition
						final String s2 = previewElement();
						if (s2 != null && Expression.operatorID(s2) == Expression.VARIABLE && getLine() != current_line && getColumn() > command_column &&( expression_column[0] > -1 || getColumn() > expression_column[0] )) {
							e = parseStructureDefinition(s, command_column, expression_column, current_line, column, expression_stack, expression_stack_top, allow_structure_definition_blocks);
							break;
						}
					}
					e = new Expression(s, script_name, current_line, column, expression_stack, expression_stack_top);
				break;
			default :
					if (s.equals("copy")) {
						e = parseStructureDefinition(s, command_column, expression_column, current_line, column, expression_stack, expression_stack_top, allow_structure_definition_blocks);
						break;
					}
					throwException("variable, constant, structure, ARRAY, FUNCTION, -, !, @ or ( expected but "+s+" found");
		}
		// check whether there's an operator following the operand e
		if (ret == null)
			ret = parseExpressionOperator(command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks);
		// remove the Expression from the stack and return it if it's still on there - in that case it must be the parent of whatever is stored in ret (if anything)
		if (expression_stack_top[0] > -1 && expression_stack[expression_stack_top[0]] == e) {
			expression_stack[expression_stack_top[0]--] = null;
			return e;
		}
		return ret;
	}


	/** parses the next operator within an expression if there is one */
	private Expression parseExpressionOperator (final int command_column, final int[] expression_column, final Expression[] expression_stack, final int[] expression_stack_top, final boolean allow_structure_definition_blocks)  {
		// check for end of script or end of expression
		String s = previewElement();
		if(s == null)
			return null;
		final int id = Expression.operatorID(s, true);
		if(id == Expression.NOT_AN_OPERATOR || id == Expression.VARIABLE || id == Expression.CONSTANT || getColumn() <= command_column)
			return null;
		// if it's the ARRAY_INDEX operator and it sits on a new line and is on the block column or further left, it's really the ASSEMBLED_VARIABLE operator or an ARRAY_INDEX operator that belongs to a different expression
		if (id == Expression.ARRAY_INDEX && getColumn() == getLineStart() && getColumn() <= command_column )
			return null;

		// check whether it's a function call
		if (id == Expression.FUNCTION_CALL)
			return parseFunctionCall(command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks);

		// remove the operator from the stream and create the expression for it
		readElement();
		Expression e = new Expression(s, script_name, getLine(), getColumn(), true, expression_stack, expression_stack_top);
		Expression ret;
		// make sure it sits in the right place
		if (getColumn() == getLineStart()) {
			if (expression_column[0] == -1)
				expression_column[0] = getColumn();
			else if (expression_column[0] != getColumn())
				throwException(s+" must sit in column "+expression_column[0]+" like the previous lines of the expression, and not in column "+getColumn());
			last_expression_line = getLine();
		}
		else if (last_expression_line != getLine())
			throwException("strange error : "+s+" doesn't sit at the beginning of a line, but it also doesn't sit on the same line as the previous part of the expression");

		// parse its second operand if it hasn't been omitted
		if (id != Expression.ARRAY_INDEX)
			ret = parseExpression(getLine(), command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks);
		else { // it's the array index operator []
			// if the brackets are empty, use an empty string "" as the second operand
			final int line = getLine();
			s = previewElement();
			if (s != null && s.equals("]")) {
				if (getLine() != line)
					throwException("the ] must sit on the same line as the [ if the array index bracket [ ] it belongs to is empty");
				readElement(); // remove the ] from the stream
				final Expression empty_brackets = new Expression("\"\"", script_name, getLine(), getColumn(), expression_stack, expression_stack_top);
				e.addExpression(empty_brackets);
				expression_stack[expression_stack_top[0]--] = null; // empty_brackets has paced itself on the stack. remove it
			}
			else {
				// read the expression inside the bracket
				e.addExpression(parseExpression(line, command_column, expression_column, new Expression[Global.PARSER_EXPRESSION_STACK_SIZE], new int[]{ -1 }, true));
				// we need to remove the ] from the stream
				s = readElement();
				if(s == null || !s.equals("]"))
					throwException("] expected but "+((s==null)?"end of script":s)+" found");
				if (getColumn() == getLineStart())
					throwException("the ] of the array index brackets [ ] must follow right behind the expression within the brackets. it cannot stand at the beginning of a new line");
			}
			last_expression_line = getLine();
			// check whether there's another operator following the ]
			ret = parseExpressionOperator(command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks);
		}
		// remove the operator Expression from the stack and return it if it's still on there - in that case it must be the parent of whatever is stored in ret (if anything)
		if (expression_stack_top[0] > -1 && expression_stack[expression_stack_top[0]] == e) {
			expression_stack[expression_stack_top[0]--] = null;
			return e;
		}
		return ret;
	}


	private ForLoopCommand parseForLoop (final int command_line, final int command_column)  {
		readElement(); // remove the for tag from the stream
		// we now have an enclosing loop (important if we run into break commands)
		final boolean was_inside_loop = inside_loop;
		inside_loop = true;
		String s = previewElement();
		if (s == null)
			throwException("variable or ( expected right after the \"for\" but end of script found");
		if (getLine() != command_line)
			throwException(s+" must follow directly after the \"for\" on the same line");
		if (s.equals("(")) { // it's a java style loop, e.g.    for (i=0; i<5; i++)
			Expression variable = null, initial_value = null, condition = null;
			ScriptNode increment = null;
			readElement();
			s = previewElement();
			if (s == null)
				throwException("variable or ; expected right after the \"for (\" but end of script found");
			if (!s.equals(";")) {
				// parse the expression that describes the variable
				variable = parseExpression(command_line, command_column, false);
				if (last_expression_line != command_line)
					throwException("the variable definition  "+variable+"  must follow directly after the \"for (\" on the same line and must end on the same line");
				// parse the expression for the initial value if there is one
				s = previewElement();
				if (s == null ||( !s.equals("=") && !s.equals(";") ))
					throwException("; or = expected but "+((s!=null)?s:"end of script")+" found");
				if (getLine() != command_line)
					throwException(s+" must be on the same line as the \"for (\"");
				if (s.equals("=")) {
					readElement();
					initial_value = parseExpression(command_line, command_column, false);
					if (last_expression_line != command_line)
						throwException("the definition  "+initial_value+"  for the initial value of  "+variable+"  must be on the same line as the \"for\"");
					s = previewElement();
					if (s == null || !s.equals(";"))
						throwException("; expected but "+((s!=null)?s:"end of script")+" found");
					if (getLine() != command_line)
						throwException("; must be on the same line as the \"for (\"");
				}
			}
			// remove the ; after the variable definition from the stream
			readElement();
			s = previewElement();
			if (s == null)
				throwException("; or conditional expression expected but end of script found");
			if (getLine() != command_line)
				throwException(s+" must be on the same line as the \"for (\"");
			// parse the conditional expression if there is one
			if (!s.equals(";")) {
				condition = parseExpression(command_line, command_column, false);
				if (last_expression_line != command_line)
					throwException("the conditional expression  "+condition+"  must be on the same line as the \"for\"");
				s = previewElement();
				if (s == null || !s.equals(";"))
					throwException("; expected but "+((s!=null)?s:"end of script")+" found");
				if (getLine() != command_line)
					throwException("; must be on the same line as the \"for (\"");
			}
			// remove the ; after the condition from the stream
			readElement();
			s = previewElement();
			if (s == null)
				throwException("; or increment command expected but end of script found");
			if (getLine() != command_line)
				throwException(s+" must be on the same line as the \"for (\"");
			// parse the increment command
			if (!s.equals(")")) {
				increment = parseSingleCommand(getColumn());
				s = previewElement();
				if (s == null || !s.equals(")"))
					throwException(") expected but "+((s!=null)?s:"end of script")+" found");
				if (getLine() != command_line)
					throwException("the closing ) around the for loop parameters must be on the same line as the \"for (\"");
			}
			// remove the ) after the increment command from the stream
			readElement();
			// return the complete java-style loop, with the block inside the loop parsed
			ForLoopCommand ret = new ForLoopCommand(variable, initial_value, condition, increment, script_name, command_line, command_column);
			parseBlock(ret, command_line, command_column);
			// we are no longer inside this loop, although there may still be other, enclosing loops
			inside_loop = was_inside_loop;
			return ret;
		}
		else { // it's a BASIC style loop, e.g.   for i = 0 to 5 step 2
			// parse the variable expression
			final Expression variable = parseExpression(command_line, command_column, false);
			// remove the = from the stream
			s = readElement();
			if (s == null || !s.equals("="))
				throwException("= expected but "+((s!=null)?s:"end of script")+" found");
			if (getLine() != command_line)
				throwException("the = must sit on the same line as the \"for\"");
			// parse the initial value
			final Expression initial_value = parseExpression(command_line, command_column, false);
			// remove the "to"/"downto" from the stream
			s = readElement();
			if (s == null ||( !s.equals("to") && !s.equals("downto")))
				throwException("\"to\" or \"downto\" expected but "+((s!=null)?s:"end of script")+" found");
			if (getLine() != command_line)
				throwException("the \""+s+"\" must sit on the same line as the \"for\"");
			final boolean go_up = s.equals("to");
			// read the final value expression
			final Expression final_value = parseExpression(command_line, command_column, false);
			if (last_expression_line != command_line)
				throwException("the definition  "+final_value+"  for the limiting value of  "+variable+"  must be on the same line as the \"for\"");
			// check wether there's a step expression
			Expression step = null;
			s = previewElement();
			if (s != null && getLine() == command_line) {
				// remove the "step" from the stream
				if (!s.equals("step"))
					throwException("step  expected but  "+s+"  found");
				readElement();
				// read the step expression
				step = parseExpression(command_line, command_column, false);
				if (last_expression_line != command_line)
					throwException("the the step expression  "+step+"  must end on the same line as the \"for\"");
			}
			// return the complete BASIC-style loop, with the block inside the loop parsed
			ForLoopCommand ret = new ForLoopCommand(variable, initial_value, go_up, final_value, step, script_name, command_line, command_column);
			parseBlock(ret, command_line, command_column);
			// we are no longer inside this loop, although there may still be other, enclosing loops
			inside_loop = was_inside_loop;
			return ret;
		}
	}


	private Expression parseFunctionCall (final int command_column, final int[] expression_column, final Expression[] expression_stack, final int[] expression_stack_top, final boolean allow_structure_definition_blocks)  {
		readElement(); // remove the ( from the stream
		if (getColumn() == getLineStart())
			throwException("the ( of a function call must follow directly after the variable that contains the function, on the same line. it must not sit on a new line");
		FunctionCall fc = new FunctionCall(script_name, getLine(), getColumn(), expression_stack, expression_stack_top);
		// parse the arguments
		int argument_column = -1;
		final Expression[] es = new Expression[Global.PARSER_EXPRESSION_STACK_SIZE];
		final int[] est = { -1 };
		String s = previewElement();
		if (s == null)
			throwException(") or function call argument expected but end of script found");
		if (s.equals(")"))
			readElement(); // remove the ) from the stream
		while (!s.equals(")")) {
			s = previewElement();
			// make sure the next argument expression starts in a legal column
			if (getColumn() == getLineStart()) {
				if (expression_column[0] > -1 && getColumn() <= expression_column[0])
					throwException(s+" must sit right of column "+expression_column[0]);
				if (getColumn() <= command_column)
					throwException(s+" must sit right of column "+command_column);
				if (argument_column == -1)
					argument_column = getColumn();
				else if (getColumn() != argument_column)
					throwException(s+" must startin column "+argument_column);
//				if (s.equals(","))
//					throwException("the , between function call arguments must not sit at the beginning of a line");
			}
			// if the argument has been omitted, add an empty argument to the function call, otherwise parse the argument
			if (s.equals(","))
				fc.addArgument(null);
			else
				fc.addArgument(parseExpression(getLine(), command_column, expression_column, es, est, true));
			s = readElement();
			if (s == null ||( !s.equals(",") && !s.equals(")") ))
				throwException(") or , expected but "+((s!=null)?s:"end of script")+" found");
		}

		// make sure the ) sits in a legal column
		if (getColumn() == getLineStart()) {
			if (getPreviousLineStart() > getColumn()) {
				// the last argument expression of the function call ended with an indented block. the ) must sit on the expression_column
				if (expression_column[0] > -1) {
					if (getColumn() != expression_column[0])
						throwException("the ) of the function call should sit in column "+expression_column[0]+", not in column "+getColumn());
				}
				else {
					if (block_column == -1)
						throwException("why is block_column -1 even though we just parsed a block ?");
					if (getColumn() >= block_column)
						throwException("the ) of the function call must sit left of column "+block_column+", the column of the last block in the argument list");
					if (getColumn() <= command_column)
						throwException("the ) of the function call must sit right of column "+command_column);
					expression_column[0] = getColumn();
				}
			}
			else
				throwException(") should sit right behind the last function argument, not on the beginning of a new line");
		}

		last_expression_line = getLine();
		// check whether there's another operator following the function call
		final Expression ret = parseExpressionOperator(command_column, expression_column, expression_stack, expression_stack_top, allow_structure_definition_blocks);

		// remove the function call from the stack and return it if it's still on there - in that case it must be the parent of whatever is stored in ret (if anything)
		if (expression_stack_top[0] > -1 && expression_stack[expression_stack_top[0]] == fc) {
			expression_stack[expression_stack_top[0]--] = null;
			return fc;
		}
		return ret;
	}


	private ScriptNode parseFunctionDefinition (final int command_line, final int command_column, final int enclosing_line, final int enclosing_column)  {
		readElement(); // discard the FUNCTION tag
		final Function f = new Function(script_name, command_line, command_column);
		String s = previewElement();
		// if it's a function without function body and argument list at the end of the script, s will be null and we're done parsing the function
		if(s != null) {
			// we are no longer inside a loop, if we were before
			final boolean was_inside_loop = inside_loop;
			inside_loop = false;
			// parse the list of argument names, if there is one
			if (getLine() == enclosing_line) {
				if (!s.equals("("))
					throwException("the only thing you can put behind a FUNCTION tag is the list of argument names, enclosed in ( ) brackets.\nThe function body must start on the next line, if there is one");
				readElement(); // remove the ( from the stream
				s = readElement();
				while (s == null || !s.equals(")")) {
					if (s == null)
						throwException("variable name or ) expected but end of script found");
					if (getLine() != enclosing_line)
						throwException(s+" must sit on the same line as the FUNCTION tag, since it's part of the argument name list");
					final String argname = s;
					Expression argvalue = null;
					s = readElement();
					if (s != null && s.equals("=")) {
						argvalue = parseExpression(enclosing_line, command_column, false);
						s = readElement();
					}
					f.addArgument(argname, argvalue);
					if (s == null ||( !s.equals(")") && !s.equals(",") ))
						throwException(") or , expected but "+((s==null)?"end of script":s)+" found");
					if (getLine() != enclosing_line)
						throwException(s+" must sit on the same line as the FUNCTION tag, since it's part of the argument name list");
					if (!s.equals(")"))
						s = readElement();
				}
				// s contains the ) of the argument name list, make sure it sits on the same line as the FUNCTION tag
				if (getLine() != enclosing_line)
					throwException(s+" must sit on the same line as the FUNCTION tag, since it's part of the argument name list");
			}
			// parse the function body
			parseBlock(f.getFunctionBody(), enclosing_line, enclosing_column);
			// the function definition is over. if it was inside a loop, we are back in that loop
			inside_loop = was_inside_loop;
		}
		return f;
	}


	private GlobalDefinitionCommand parseGlobalDefinitionCommand (final int command_line, final int command_column)  {
		readElement(); // discard the "global" tag
		int count = 0;
		AssignmentCommand[] globals = new AssignmentCommand[10];
		// determine whether it's a single line definition or a block definition
		String s = previewElement();
		if (s != null) {
			if (getLine() == command_line)
				throwException("cannot have anything on the line where the \"global\" command stands. put the global definitions in the block that follows the command");
			final int column = getColumn();
			if (column > command_column)  {
				do {
					// make sure the list of globals can hold another item
					if (count == globals.length) {
						final AssignmentCommand[] c = globals;
						globals = new AssignmentCommand[globals.length*2];
						System.arraycopy(c, 0, globals, 0, count);
					}
					// add the next definition of a global to the list
					final int line = getLine(); // so we can tell the AssignmentCommand where it started, if this is one
					// parse the expression that will sit on the left side of the = or by itself if it's a function call
					final Expression x = parseExpression(line, column, false);
					if (x.getOperator() != Expression.VARIABLE)
						throwException("can only have simple variable names on the left side of a = in \"global\" blocks");
					s = readElement();
					// check whether it's an assignment command
					if (s == null || !s.equals("="))
						throwException("= expected but "+((s==null)?"end of script":s)+" found");
					final AssignmentCommand ac = new AssignmentCommand(true, false, script_name, line, column);
					ac.setVariableExpression(x); // the stuff that sits on the left side of the =
					s = previewElement();
					if(s == null)
						throwException("Expression of FUNCTION definition on the right side of the = expected but end of script found");
					if (getLine() != line)
						throwException("Expression of FUNCTION definition on the right side of the = must start on the same line as the expression on the left side did");
					if(s.equals("FUNCTION"))
						ac.setValueExpression(parseFunctionDefinition(line, getColumn(), line, column));
					else {
						final Expression value = parseExpression(line, column, true);
						if (value.getOperator() == Expression.GLOBAL_VALUE)
							throwException("can't assign a global value to a global value");
						ac.setValueExpression(value);
					}
					globals[count++] = ac;
					// check whether there's another global definition following this one
					s = previewElement();
				} while (s != null && getColumn() == column);
			}
			if (s != null && getColumn() > command_column)
				throwException(s + " should be starting in column "+column+", column "+command_column+" or further left");
		}
		if (count == 0)
			throwException("the globals command must be followed by at least one global value");
		// create and return the corresponding ConstantDefinitionCommand
		final AssignmentCommand[] global_list = new AssignmentCommand[count];
		System.arraycopy(globals, 0, global_list, 0, count);
		return new GlobalDefinitionCommand(global_list, script_name, command_line, command_column);
	}


	private void parseLocalizedScript (final Script script)  {
		final int line = getLine(), column = getColumn();
		// check whether it's a valid language
		String language = readElement();
		if(language.length() <= 2)
			throwException("Unknown language : "+language);
		Locale locale = new Locale(language.substring(0,2));
		if(locale.getDisplayLanguage(locale).length() <= 2 || !locale.getDisplayLanguage(locale).equalsIgnoreCase(language))
			throwException("Unknown language : "+language);
		if(script.getLocalization(locale.getDisplayLanguage(locale)) != null)
			throwException("Duplicate language localization : "+language);
		// create the localzed script
		// check whether this localization of the script is defined explicitly or by replacing string constants from another version
		String s = previewElement();
		if(s != null) {
			if(s.equals("extends"))
				parseReplacementScript(locale, script, line, column);
			else  {
				final Block localized_script = new Block(script_name, line, column);
				parseBlock(localized_script, line, column);
				script.addLocalization(locale.getDisplayLanguage(locale), localized_script);
			}
			// make sure there is only one loalization on each line - prefent lines like       english deutsch francais
			s = previewElement();
			if (s != null && getLine() == line)
				throwException("there must not be more than one localization per script line. Put "+s+" on a different line");
		}
	}


	private PrintCommand parsePrintCommand ()  {
		final int line = getLine(), column = getColumn();
		readElement(); // discard the print command tag
		final PrintCommand pc = new PrintCommand(script_name, line, column);
		pc.setDisplayedExpression(parseExpression(line, column, true));
		return pc;
	}


	private void parseReplacement (final Block ls, final int enclosing_column)  {
		readElement(); // just discard the 'replace' tag - it has already been checked
		final int tag_line = getLine();

		String original = readElement();
		if(original == null || !original.startsWith("\""))
			throwException("Original string expected but "+((original==null)?"end of script":original)+" found");
		if (getColumn() <= enclosing_column)
			throwException("the original string must begin right of column "+enclosing_column);
		final int original_line = getLine();
		if (original_line == tag_line)
			throwException("the original string must sit on a line by itself");
		original = original.substring(1, original.length()-1);
		if(original.length() == 0)
			throwException("The string to be replaced must not be the empty string \"\"");

		final boolean dots = original.endsWith("...");
		if(dots)
			original = original.substring(0, original.length()-3);
		if(original.length() == 0)
			throwException("The string to be replaced must contain at least one character in front of the ... (a space qualifies)");

		String replacement = readElement();
		if(replacement == null || !replacement.startsWith("\""))
			throwException("Replacement string expected but "+((replacement==null)?"end of script":replacement)+" found");
		if (getLine() == original_line)
			throwException("the replacement string must sit on a line by itself");
		if (getColumn() != getPreviousLineStart())
			throwException("original string and replacement string must start in the same column");

		ls.replace(original, dots, replacement.substring(1,replacement.length()-1));
	}


	private void parseReplacementScript (final Locale new_locale, final Script script, final int enclosing_line, final int enclosing_column)  {
		// discard the "extends" tag
		readElement();
		// clone the original localization we will derive the new localization from
		final String original_language = readElement();
		final Locale original_locale = new Locale(original_language.substring(0,2));
		if(original_language.length() <= 2 || !original_locale.getDisplayLanguage(original_locale).equalsIgnoreCase(original_language))
			throwException("Unknown language : "+original_language);
		final Block original_localization = script.getLocalization(original_locale.getDisplayLanguage(original_locale)); // the localization our new localization will be derived from
		if(original_localization == null)
			throwException("The localization "+original_locale.getDisplayLanguage(original_locale)+" is not defined yet. It cannot be used to define the localization "+new_locale.getDisplayLanguage(new_locale));
		final Block localized_script = new Block(script_name, enclosing_line, enclosing_column);

		// replace strings if there is a replacement block
		String s = previewElement();
		if (getLine() == enclosing_line)
			throwException("whatever follows \""+original_locale.getDisplayLanguage(original_locale)+" extends "+new_locale.getDisplayLanguage(new_locale)+"\" must not sit on the same line");

		final int column = getColumn();
		if (column > enclosing_column) {
			while(s != null && getColumn() == column) {
				parseReplacement(localized_script, column);
				s = previewElement();
			}
			// make sure all block entries start in the same column
			if (s != null && getColumn() > enclosing_column && getColumn() != column) {
				if (s.equals("replace"))
					throwException("the replace command starts in column "+getColumn()+". it should start in column "+column);
				else
					throwException(previewElement() +" starts in column "+getColumn()+". it should start in column "+enclosing_column+((enclosing_column==1)?"":" or left of it"));
			}
		}

		// finally add the new localization to the script script
		script.addLocalization(new_locale.getDisplayLanguage(new_locale), localized_script);
	}


	private Script parseScript ()  {
		final Script ret = new Script(script_name);
		// parse the script script
System.out.println(script_name);
		while(previewElement() != null)
			parseLocalizedScript(ret);
		if(ret.localizationCount() == 0)
			throwException("Every script script must contain at least one localized script");
		return ret;
	}


	private ScriptNode parseSingleCommand (final int _block_column)  {
		final int line = getLine();
		final String s = previewElement();
		if(s.equals("if") || s.equals("while"))
			return parseConditionalCommand(line, _block_column);
		else if(s.equals("for"))
			return parseForLoop(line, _block_column);
		else if (s.equals("print"))
			return parsePrintCommand();
		else if (s.equals("constant"))
			return parseConstantDefinitionCommand(line, _block_column);
		else if (s.equals("global"))
			return parseGlobalDefinitionCommand(line, _block_column);
		else if (s.equals("structure"))
			return parseTypeDefinitionCommand(line, _block_column);
		else if (s.equals("with"))
			return parseWithCommand(line, _block_column);
		else if (s.equals("else") || s.equals("elseif"))
			throwException(s+" without if found");
		else if (s.equals("++") || s.equals("--"))
			return new IncrementDecrementCommand(readElement(), true, parseExpression(line, _block_column, false), script_name, line, _block_column);
		else if (s.equals("break"))
			return parseBreakCommand(line, _block_column);
		else if (Expression.operatorID(s) == Expression.VARIABLE)
			return parseAssignmentCommandOrExpression(_block_column);
		else
			throwException("Command expected but "+s+" found");
		// unreachable statement
		return null;
	}

	private StructureDefinition parseStructureDefinition (final String structure_type, final int command_column, final int[] expression_column, final int line, final int column, final Expression[] expression_stack, final int[] expression_stack_top, final boolean allow_structure_definition_blocks)  {
		StructureDefinition  sd;
		String s;
		// check whether it's the copy(strucure) command
		if (!structure_type.equals("copy"))
			sd = new StructureDefinition(structure_type, script_name, line, column, expression_stack, expression_stack_top);
		else {
			s = readElement();
			if (s == null || !s.equals("(") || getLine() != line)
				throwException("copy must be followed immediately by an opening bracket on the same line, like so : copy(some_variable)");
			previewElement();
			final Expression original_structure = parseExpression(line, command_column, true);
			s = readElement();
			if (s == null || !s.equals(")"))
				throwException("closing bracket missing of the copy() command at line "+line+", column "+column);
			if (getColumn() <= command_column)
				throwException("closing bracket of the copy() command at line "+line+", column "+column+" must sit right of column "+command_column);
			sd = new StructureDefinition(original_structure, script_name, line, column, expression_stack, expression_stack_top);
		}
		// check whether the structure definition has a body
		s = previewElement();
		final int col = getColumn();
		if (s == null || col <= command_column ||( expression_column[0] > -1 && col <= expression_column[0] )|| Expression.operatorID(s) != Expression.VARIABLE)
			return sd;
		// parse the structure definition body
		sd.addDefinitionBody(parseBlock(new Block(script_name, line, column), line, command_column));
		return sd;
	}


	private TypeDefinition parseTypeDefinition ()  {
		final int line = getLine(), column = getColumn();
		final String structure_type = readElement();
		String s;
		// check whether it's a structure definition that extends some other type
		String extended_type = null;
		s = previewElement();
		if (getLine() == line && s.equals("extends")) {
			readElement();
			extended_type = readElement();
			if (extended_type == null || Expression.operatorID(extended_type) != Expression.VARIABLE)
				throwException("name of the structure type to be extended expected but "+((extended_type==null)?"end of script":extended_type)+" found");
			if (line != getLine())
				throwException("the name of the structure type to be extended must follow immediately after the 'extends' on the same line");
			s = previewElement();
		}
		TypeDefinition sd = new TypeDefinition(structure_type, extended_type, script_name, line, column);
		// check whether the structure definition has a body
		final int col = getColumn();
		if (s == null || col <= column || Expression.operatorID(s) != Expression.VARIABLE)
			return sd;
		// parse the structure definition body
		Object[] variable = new Object[10];
		int count = 0;
		do {
			// make sure the list of variables is big enough
			if (count == variable.length)  {
				final Object[] v = variable;
				variable = new Object[count*2];
				System.arraycopy(v, 0, variable, 0, count);
			}
			// add the new variable
			readElement(); // the variable name is already stored in s
			variable[count] = s;
			final int current_line = getLine();
			// check whether the variable has a default value
			s = previewElement();
			if (s != null && s.equals("=") && current_line == getLine()) {
				final AssignmentCommand ac = new AssignmentCommand(false, true, script_name, current_line, col);
				ac.setVariableExpression(new Expression((String)variable[count], script_name, current_line, getColumn()));
				readElement(); // remove the =
				s = previewElement();
				if (current_line != getLine())
					throwException("assigned expression must start on the line of the =");
				if (s.equals("FUNCTION"))
					ac.setValueExpression(parseFunctionDefinition(current_line, getColumn(), current_line, col));
				else
					ac.setValueExpression(parseExpression(current_line, col, true));
				variable[count] = ac;
				s = previewElement();
			}
			count++;
		} while (s != null && getColumn() == col && Expression.operatorID(s) == Expression.VARIABLE);
		last_expression_line = getPreviousElementLine();
		// add the body to the StructureDefinition object
		final Object[] v = new Object[count];
		System.arraycopy(variable, 0, v, 0, count);
		sd.setDefinitionBody(v);
		return sd;
	}


	private TypeDefinitionCommand parseTypeDefinitionCommand (final int command_line, final int command_column)  {
		readElement();
		int count = 0;
		TypeDefinition[] type = new TypeDefinition[10];
		previewElement();
		final int column = getColumn();
		if (getLine() == command_line)  {
			// the structure type name is on the same line as the type tag. this means only that one structure type is defined
			type[count++] = parseTypeDefinition();
		}
		else if (getColumn() <= command_column)
			throwException("the type command must be followed by at least one structure type to be defined");
		else {
			// the command is followed by a block containing structure type definitions
			do {
				// make sure the the list of structure types is big enough to hold another entry
				if (type.length == count) {
					final TypeDefinition[] t = type;
					type = new TypeDefinition[count*2];
					System.arraycopy(t, 0, type, 0, count);
				}
				type[count++] = parseTypeDefinition();
				previewElement();
			} while (getColumn() == column);
		}
		// create and return the new TypeDefinitionCommand
		final TypeDefinition[] t = new TypeDefinition[count];
		System.arraycopy(type, 0, t, 0, count);
		return new TypeDefinitionCommand(t, script_name, command_line, command_column);
	}


	private WithCommand parseWithCommand (final int command_line, final int command_column)  {
		readElement();  // discard the command tag
		final Expression with_expression = parseExpression(command_line, command_column, false);
		final WithCommand wc = new WithCommand(with_expression, script_name, command_line, command_column);
		parseBlock(wc, last_expression_line, command_column);
		return wc;
	}
}