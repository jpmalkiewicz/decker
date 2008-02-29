package decker.model;
import java.io.*;


/** this class returns mission script elements from a stream */
abstract class ScriptReader
{
	private final static String WHITESPACE = " \t\r\n";
	private final static String SINGLE_CHARACTER_ELEMENTS = ".*/+-(){}[],?:;%";
	final static String VARIABLE_NAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyz_ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	final static String VARIABLE_NAME_CHARACTERS_AND_DOT = VARIABLE_NAME_CHARACTERS + ".";
	static int TABULATOR_SIZE = 3;
	static boolean FORCE_TABULATOR_BLOCK_INDENTATION = true;


	private String script_name;
	private Reader in;


	private int current_line, current_column;
	private int next_line, next_column;
	private int previous_element_line; // the line of the previous element
	private int previous_line_start, current_line_start, next_line_start; // column on which the line starts
	private String stored_element;

	// the vars below are used by preview(), preview2() and readChar()
	private int stored_char, stored_char2, last_char;


	ScriptReader (final String _script_name, final Reader _source)  {
		script_name = _script_name;
		in = _source;
		stored_char = -1;
		stored_char2 = -1;
		next_line = 1;
		next_column = 1;
		current_line_start = 1;
		previous_line_start = 1;
		previous_element_line = 0;
		skipWhitespace();
		previewElement(); // this initializes the current cursor position
	}


	/** returns the column of the element that has last been read or previewed */
	protected final int getColumn ()  { return current_column; }


	/** returns the number of the currently parsed script line */
	protected final int getLine ()  { return current_line; }


	/** returns the number of the currently parsed script line */
	protected final int getLineStart ()  { return current_line_start; }


	/** returns the line on which the last script element was */
	protected final int getPreviousElementLine ()  { return previous_element_line; }


	/** returns the number of the previous script line that contined a script element */
	protected final int getPreviousLineStart ()  { return previous_line_start; }


	/** reads an element of the mission script without removing it from the stream */
	final String previewElement ()  {
		if(stored_element != null)
			return stored_element;

		stored_element = readElement();
		return stored_element;
	}


	final String readElement ()  {
		// check whether there is an element left over from the parsing of the last command
		if(stored_element != null) {
			final String ret = stored_element;
			stored_element = null;
			return ret;
		}

		// move the cursor to the element we'll read now
		previous_element_line = current_line;
		current_line = next_line;
		current_column = next_column;
		current_line_start = next_line_start;

		final int c = read(), c2 = preview();

		// return null if the end of the stream has been reached
		if(c == -1)
			return null;

		// catch string constants
		if(c == '"')
			return readString();

		// catch  - -- + ++
		if(c == '-' || c == '+') {
			if(c2 == c)  {
				read();
				skipWhitespace();
				return (c=='-') ? "--" : "++";
			}
			skipWhitespace();
			return (c=='-') ? "-" : "+";
		}

		// catch  . *  /  (  )  {  }  [  ] ,
		if(SINGLE_CHARACTER_ELEMENTS.indexOf(c) > -1)  {
			skipWhitespace();
			return ((char)c)+"";
		}

		// catch  &   &&
		if(c == '&') {
			if(c2 == '&')  {
				read();
				skipWhitespace();
				return "&&";
			}
			skipWhitespace();
			return "&";
		}

		// catch  =  <  >  ==  >=  <=  !=
		if(c == '=' || c == '<' || c == '>' || c == '!')  {
			if(c2 == '=')  {
				read();
				skipWhitespace();
				return ((char)c)+"=";
			}
			if (c == '<' && c2 == '>') {
				read();
				skipWhitespace();
				return "<>";
			}
			skipWhitespace();
			return ((char)c)+"";
		}

		// catch ||
		if( c == '|' && c2 == '|')  {
			read();
			skipWhitespace();
			return "||";
		}

		// the current element is a variable name or an integer value or a real value
		if(VARIABLE_NAME_CHARACTERS.indexOf(c) == -1)
			throwException("illegal variable name character or number digit encountered : "+(char)c+" ("+c+")");
		final StringBuffer ret = new StringBuffer();
		ret.append((char)c);
		int d = preview();
		boolean is_real = (d>='0' && d<='9'), dot_found = false;
		while (d != -1 && VARIABLE_NAME_CHARACTERS_AND_DOT.indexOf(d) > -1)  {
			if (d == '.') {
				if (!is_real || dot_found) // stop reading characters if we've encountered a . and it's a variable name or if the real number would otherwise contain two .
					break;
				else
					dot_found = true;
			}
			else
				is_real &= (d>='0' && d<='9');
			ret.append((char)d);
			read();
			d = preview();
		}
		if (is_real && dot_found && ret.charAt(ret.length()-1) == '.')
			throwException("real number "+ret.toString()+" must end with a digit, not with a .");
		skipWhitespace();
		return ret.toString();
	}


	/** turns all the characters from the stream up to the next " into a string and returns it */
	private String readString()
	{
		StringBuffer ret = new StringBuffer();
		ret.append('"');
		int c = read();
		int last = 0;
		while(c != -1 &&( c != '"' || last == '\\' )) {
			ret.append((char)c);
			last = c;
			c = read();
		}

		if(c == -1)
			throwException("script ends without encountering a \" marking the end of the string below :\r\n"+ret);
		ret.append('"');
		skipWhitespace();
		return ret.toString();
	}


	/** moves through the stream until a non-whitespace character is encountered */
	private void skipWhitespace ()  {
		int c;
		boolean at_line_start = false;
		do {
			c = preview();
			// check whether there's a comment starting
			if(c == '/') {
				int c2 = preview2();
				if(c2 == '/') {
					// a comment was found. Skip the remaining line
					while (c2 != '\n' && c2 != -1)
						c2 = read();
					c = ' ';
					at_line_start = true;
				}
			}
			else if (c != -1 && WHITESPACE.indexOf(c) > -1) {
				if (c == ' ' && at_line_start && FORCE_TABULATOR_BLOCK_INDENTATION) {
					current_line = next_line;
					current_column = next_column;
					throwException("space instead of tab used to indent block. only tabs are allowed for block indentation in the current ruleset");
				}
				read();
			}

		} while(c != -1 && WHITESPACE.indexOf(c) > -1);
		if (next_line_start == 0) {
			previous_line_start = current_line_start;
			next_line_start = next_column;
		}
	}


	/** looks one character ahead, doesn't move the read cursor coordinates */
	private int preview ()  {
		return (stored_char = readChar());
	}


	/** looks two characters ahead, doesn't move the read cursor coordinates
	*   only call this after calling preview() */
	private int preview2 ()  {
		final int sc = stored_char;
		stored_char = -1;
		final int ret = read();
		stored_char = sc;
		return (stored_char2 = ret);
	}


	/** fetches the next character from the stream and moves the cursor coordinates accordingly */
	private int read ()  {
		final int ret = readChar();
		// update the read cursor coordinates
		if (ret == '\n') {
			next_line++;
			next_column = 1;
			next_line_start = 0;
		}
		else if (ret == '\t')
			next_column += TABULATOR_SIZE;
		else
			next_column++;
		// return the char
		return ret;
	}


	/** reads a char from the stream without moving the cursor coordinates */
	private int readChar ()  {
		int ret = 0;
		if (stored_char > -1)  {
			ret = stored_char;
			stored_char = -1;
		}
		else if (stored_char2 > -1)  {
			ret = stored_char2;
			stored_char2 = -1;
		}
		else {
			try {
				ret = in.read();
			} catch(IOException ex) { throwException(ex.toString()); }
			// catch \r\n and \n\r. if those occur, skip the second character
			if (( ret == '\n' && last_char == '\r' )||( ret == '\r' && last_char == '\n' ))  {
//System.out.println(((ret == '\n') ? "skipping \\n (" : "skipping \\r (") + next_line+","+next_column+")");
				last_char = 0;
				return readChar();
			}
			// store the char to be able to find \r\n and \n\r on the next character
			last_char = ret;
		}
		// return \r as \n
		return (ret == '\r') ? '\n' : ret;
	}


	protected void throwException(String cause)  {
		java.lang.System.err.println("Error in script "+script_name+" line "+getLine()+", column "+getColumn()+" :\n"+cause);
		throw new RuntimeException("Error in script "+script_name+" line "+getLine()+", column "+getColumn()+" :\n"+cause);
	}
}