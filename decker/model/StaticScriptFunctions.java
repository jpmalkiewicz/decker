package decker.model;
import decker.view.*;
import java.io.*;
import java.text.DateFormat;
import java.util.GregorianCalendar;



final class StaticScriptFunctions extends ScriptNode
{
	private final static Value DUMMY_VALUE = new Value();


	final static Value execute (final int function_id, final Value[] args) {
		Value v;
		switch (function_id) {
			case Global.F_DATE_DAY_OF_MONTH : return execute_date_day_of_month(args);
			case Global.F_DATE_DAYS_IN_MONTH : return execute_date_days_in_month(args);
			case Global.F_DATE_TEXT : return execute_date_text(args);
			case Global.F_DEBUG : return execute_debug(args);
			case Global.F_DELETE : return execute_delete(args);
			case Global.F_EXIT_PROGRAM : System.exit(0); return null;
			case Global.F_FILELIST : return execute_filelist(args);
			case Global.F_GET_STRUCTURE_STACK : return execute_get_structure_stack(args);
			case Global.F_HAS_VARIABLE : return execute_hasVariable(args);
			case Global.F_IMAGE_EXISTS : return execute_image_exists(args);
			case Global.F_INDEXOF : return execute_indexof(args);
			case Global.F_INSERT : return execute_insert(args);
			case Global.F_IS_EXPANDABLE : return execute_isExpandable(args);
			case Global.F_PIXELHEIGHT : return execute_pixelheight(args);
			case Global.F_PIXELWIDTH : return execute_pixelwidth(args);
			case Global.F_RANDOM : return execute_random(args);
			case Global.F_REPAINT : if ((v=stack[ENGINE_STACK_SLOT].get("frames_per_second")) == null || v.type() != Value.INTEGER || v.integer() <= 0) try { Global.getDisplayedComponent().repaint(); } catch (Throwable t) {}; return new Value();
			case Global.F_SIZE : return execute_size(args);
			case Global.F_SUBSTRING : return execute_substring(args);
			case Global.F_TO_LOWER_CASE : return (args.length==0) ? new Value() : new Value().set(args[0].toString().toLowerCase());
			case Global.F_TO_UPPER_CASE : return (args.length==0) ? new Value() : new Value().set(args[0].toString().toUpperCase());
			case Global.F_VALUE_TYPE : return (args.length==0) ? new Value() : new Value().set(args[0].typeNameDirect());
		}
		return null;
	}


	/** executes the hard coded script function date_day_of_month(year,month,day) */
	private final static Value execute_date_day_of_month (final Value[] args)  {
		if (args.length >= 3 && args[0] != null && args[1] != null && args[2] != null && args[0].type() == Value.INTEGER && args[1].type() == Value.INTEGER && args[2].type() == Value.INTEGER) {
			final int year = args[0].integer();
			final int month = args[1].integer();
			final int day = args[2].integer();
			return new Value().set(new GregorianCalendar(year, month, day).get(GregorianCalendar.DAY_OF_MONTH));
		}
		return DUMMY_VALUE;
	}


	/** executes the hard coded script function date_days_in_month(year,month,day) */
	private final static Value execute_date_days_in_month (final Value[] args)  {
		if (args.length >= 3 && args[0] != null && args[1] != null && args[2] != null && args[0].type() == Value.INTEGER && args[1].type() == Value.INTEGER && args[2].type() == Value.INTEGER) {
			final int year = args[0].integer();
			final int month = args[1].integer();
			final int day = args[2].integer();
			return new Value().set(new GregorianCalendar(year, month, day).getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
		}
		return DUMMY_VALUE;
	}


	/** executes the hard coded script function date_text(year,month,day) */
	private final static Value execute_date_text (final Value[] args)  {
		if (args.length >= 3 && args[0] != null && args[1] != null && args[2] != null && args[0].type() == Value.INTEGER && args[1].type() == Value.INTEGER && args[2].type() == Value.INTEGER) {
			final int year = args[0].integer();
			final int month = args[1].integer();
			final int day = args[2].integer();
			return new Value().set(DateFormat.getDateInstance(DateFormat.LONG).format(new GregorianCalendar(year, month, day).getTime()));
		}
		return DUMMY_VALUE;
	}


	/** executes the hard coded script function debug(object,to_console) */
	private final static Value execute_debug (final Value[] args)  {
		if (args.length == 0 || args[0] == null) {
			if (args.length >= 2 && args[1] != null && args[1].equals(false))
				System.err.println();
			else
				System.out.println();
		}
		else {
			final PrintStream where = (args.length >= 2 && args[1] != null && args[1].equals(false)) ? System.err : System.out;
			final Value v = args[0];
			if (v.typeDirect() == Value.FUNCTION)
				v.function().print(where, "", true);
			else if (v.typeDirect() == Value.STRUCTURE)
				v.structure().print(where, "", true);
			else if (v.typeDirect() == Value.ARRAY)
where.println("*** NOT PRINTING ARRAYS YET IN StaticScriptFunctions.execute_debug()");
			else
where.println(v);
//				where.println(v.toString());
		}
		return DUMMY_VALUE;
	}


	private final static Value execute_delete (final Value[] args)  {
		if (args.length >= 2 && args[0] != null && args[1] != null && args[0].type() == Value.ARRAY && args[1].type() == Value.INTEGER) {
			final int index = args[1].integer();
			final ArrayWrapper array = args[0].arrayWrapper();
			final int old_length = array.array.length;
			if (index >= 0 && index < old_length) {
				final Value[] new_array = new Value[old_length-1];
				System.arraycopy(array.array, 0, new_array, 0, index);
				System.arraycopy(array.array, index+1, new_array, index, old_length-index-1);
				final Value ret = array.array[index];
				array.array = new_array;
				return ret;
			}
		}
		return DUMMY_VALUE;
	}


	/** executes the hard coded script function filelist(x) */
	private final static Value execute_filelist (final Value[] args)  {
		if (args.length > 0 && args[0] != null) {
			final String s = args[0].toString();
			if (s.indexOf("..") == -1 && s.indexOf(":") == -1 && s.indexOf("~") == -1) {
				try {
					final File d = new File(("rulesets/"+Global.getCurrentRuleset().getName()+"/"+s).replace('/', File.separatorChar));
					if (d.exists() && d.isDirectory()) {
						// create an alphabetical list of file names for the files in this folder
						final File[] f = d.listFiles();
						int count = 0;
						final String[] name = new String[f.length];
						for (int i = 0; i < f.length; i++) {
							if (f[i].isFile()) {
								// insert it into the alphabetical list of file names
								final String n = f[i].getName();
								for (int j = 0; j <= count; j++)
									if (j == count)
										name[j] = n;
									else if (name[j].compareTo(n) >= 0) {
										System.arraycopy(name, j, name, j+1, count-j);
										name[j] = n;
										break;
									}
								count++;
							}
						}
						// turn the list into an ARRAY and return it
						final Value[] array = new Value[count];
						for (int i = count; --i >= 0; )
							array[i] = new Value().set(name[i]);
						return new Value().set(new ArrayWrapper(array));
					}
				} catch (Throwable t) { t.printStackTrace(); }
			}
		}
		return new Value().set(new ArrayWrapper(new Value[0]));
	}


	/** executes the hard coded script function getStructureStack(year,month,day) */
	private final static Value execute_get_structure_stack (final Value[] args)  {
		Structure stack = new Structure("ARRAY");
		for (int i = 0; i < ScriptNode.stack_size-1; i++) { // -1 because otherwise it would add its own LOCAL data object to the returned array
			stack.add("").set(new Value().set(ScriptNode.stack[i].get("structure_type")));
		}
		return new Value().set(stack);
	}


	/** returns true if args[0] is a structure and has a variable named args[1] */
	private final static Value execute_hasVariable (final Value[] args)  {
		return new Value().set(args.length > 1 && args[0] != null && args[1] != null && args[0].type() == Value.STRUCTURE && args[0].structure().get(args[1].toString()) != null);
	}


	/** searches for a value in an array or a substring in a string */
	private final static Value execute_image_exists (final Value[] args)  {
		return new Value().set(args.length > 0 && args[0] != null && AbstractView.getImage(args[0].toString()) != null);
	}


	/** searches for a value in an array or a substring in a string. args are what, where, direction, from_index */
	private final static Value execute_indexof (final Value[] args)  {
		if (args.length >= 2 && args[0] != null && args[1] != null) {
			final int where_type = args[1].type();
			final boolean direction = !(args.length > 2 && args[2] != null && args[2].equals(false));
			if (where_type == Value.STRING) {
				// we're looking for a substring in a string
				final String s = args[1].string(), what = args[0].toString();
				final int size = s.length();
				if (direction) {
					// search forwards
					final int start = (args.length > 3 && args[3] != null && args[3].type() == Value.INTEGER) ? args[3].integer() : 0;
					int ret = s.indexOf(what, start);
					if (ret >= 0)
						return new Value().set(ret);
				}
				else {
					// search backwards
					final int start = (args.length > 3 && args[3] != null && args[3].type() == Value.INTEGER) ? args[3].integer() : size-1;
					int ret = s.lastIndexOf(what, start);
					if (ret >= 0)
						return new Value().set(ret);
				}
			}
			else if (where_type == Value.ARRAY) {
				// search for a value in an array
				final Value[] array = args[1].array();
				final int size = array.length;
				final Value what = args[0];
				if (direction) {
					// search forwards
					final int start = (args.length > 3 && args[3] != null && args[3].type() == Value.INTEGER && args[3].integer() > 0) ? args[3].integer() : 0;
					final int end = size-1;
					for (int i = start; i <= end; i++) {
						if (array[i].equals(what)) {
							return new Value().set(i);
						}
					}
				}
				else {
					// search backwards
					final int start = (args.length > 3 && args[3] != null && args[3].type() == Value.INTEGER && args[3].integer() < size-1) ? args[3].integer() : size-1;
					for (int i = start; i >= 0; i--) {
						if (array[i].equals(what)) {
							return new Value().set(i);
						}
					}
				}
			}
		}
		return DUMMY_VALUE;
	}


	private final static Value execute_insert (final Value[] args)  {
		if (args.length >= 2 && args[0] != null && args[1] != null && args[0].type() == Value.ARRAY && args[1].type() == Value.INTEGER) {
			final int index = args[1].integer();
			final ArrayWrapper array = args[0].arrayWrapper();
			final int old_length = array.array.length;
			if (index >= 0 && index <= old_length) {
				final Value[] new_array = new Value[old_length+1];
				System.arraycopy(array.array, 0, new_array, 0, index);
				System.arraycopy(array.array, index, new_array, index+1, old_length-index);
				new_array[index] = new Value();
				array.array = new_array;
				return new_array[index];
			}
		}
		return DUMMY_VALUE;
	}


	private final static Value execute_isExpandable (final Value[] args)  {
		if (args.length == 0 || args[0] == null || args[0].type() != Value.STRUCTURE)
			return new Value().set(false);
		Value v = getStructureType(args[0].get("structure_type").string()).get("expandable");
		return new Value().set(v != null && v.equals(true));
	}


	private final static Value execute_pixelheight (final Value[] args)  {
		return new Value().set((args.length == 0 || args[0] == null) ? 0 : Global.getViewWrapper().getView().height(args[0]));
	}


	private final static Value execute_pixelwidth (final Value[] args)  {
		return new Value().set((args.length == 0 || args[0] == null) ? 0 : Global.getViewWrapper().getView().width(args[0]));
	}


	private final static Value execute_random (final Value[] args)  {
		if (args.length >= 2 && args[0] != null && args[1] != null) {
			if (args[0].type() == Value.INTEGER && args[1].type() == Value.INTEGER) {
				int start = args[0].integer();
				int end = args[1].integer();
				// make sure start <= end
				if (start > end) {
					final int k = start;
					start = end;
					end = k;
				}
				return new Value().set(Global.random.nextInt(end-start+1)+start);
			}
			if (args[0].type() == Value.STRING && args[1].type() == Value.STRING) {
				String sstart = args[0].string();
				String send = args[1].string();
				if (sstart.length() == 1 && send.length() == 1) {
					int start = (int) sstart.charAt(0);
					int end = (int) send.charAt(0);
					// make sure start <= end
					if (start > end) {
						final int k = start;
						start = end;
						end = k;
					}
					return new Value().set(((char)(Global.random.nextInt(end-start+1)+start)) + "");
				}
			}
		}
		return DUMMY_VALUE;
	}


	/** executes the hard coded script function size(x) */
	private final static Value execute_size (final Value[] args)  {
		if (args.length > 0 && args[0] != null) {
			if (args[0].type() == Value.ARRAY)
				return new Value().set(args[0].array().length);
			else
				return new Value().set(args[0].toString().length());
		}
		return DUMMY_VALUE;
	}


	/** executes the hard coded script function substring(x) */
	private final static Value execute_substring (final Value[] args)  {
		if (args.length > 0 && args[0] != null) {
			final String s = args[0].toString();
			final int slength = s.length();
			int from = 0, to = slength;
			if (args.length > 1 && args[1] != null && args[1].type() == Value.INTEGER) {
				from = args[1].integer();
				if (from >= to)
					return new Value().set("");
				if (from < 0)
					from = 0;
			}
			if (args.length > 2 && args[2] != null && args[2].type() == Value.INTEGER) {
				to = args[2].integer();
				if (to <= from)
					return new Value().set("");
				if (to > slength)
					from = slength;
			}
			return new Value().set(s.substring(from,to));
		}
		return DUMMY_VALUE;
	}


// ****************************************************************************************************************************************************
// dummy stuff to get the class to compile ************************************************************************************************************
// ****************************************************************************************************************************************************


	private StaticScriptFunctions () {
		super ("", 0, 0);
	}


	ScriptNode copy () { throw new RuntimeException("should never get called"); }


	public Value execute ()  { throw new RuntimeException("should never get called"); }


	boolean print (final PrintStream out, final String indentation, final boolean line_start)  { throw new RuntimeException("should never get called"); }
}