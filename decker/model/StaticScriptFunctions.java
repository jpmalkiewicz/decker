package decker.model;
import decker.view.*;
import java.io.*;
import java.text.DateFormat;
import java.util.GregorianCalendar;



final class StaticScriptFunctions extends ScriptNode
{
	final static void execute (final int function_id, final Structure function_data, final Value[] args) {
		Value v;
		switch (function_id) {
			case Global.F_DATE_DAY_OF_MONTH : execute_date_day_of_month(function_data); break;
			case Global.F_DATE_DAYS_IN_MONTH : execute_date_days_in_month(function_data); break;
			case Global.F_DATE_TEXT : execute_date_text(function_data); break;
			case Global.F_DEBUG : execute_debug(function_data); break;
			case Global.F_DELETE : execute_delete(function_data); break;
			case Global.F_EXIT_PROGRAM : System.exit(0); break;
			case Global.F_FILELIST : execute_filelist(function_data); break;
			case Global.F_IMAGE_EXISTS : execute_image_exists(function_data); break;
			case Global.F_INDEXOF : execute_indexof(function_data); break;
			case Global.F_INSERT : execute_insert(function_data); break;
			case Global.F_PIXELHEIGHT : execute_pixelheight(function_data); break;
			case Global.F_PIXELWIDTH : execute_pixelwidth(function_data); break;
			case Global.F_RANDOM : execute_random(function_data); break;
			case Global.F_REPAINT : if ((v=stack[ENGINE_STACK_SLOT].get("frames_per_second")) == null || v.type() != Value.INTEGER || v.integer() <= 0) try { Global.getDisplayedComponent().repaint(); } catch (Throwable t) {}; break;
			case Global.F_SIZE : execute_size(function_data); break;
			case Global.F_SUBSTRING : execute_substring(function_data); break;
			case Global.F_TO_LOWER_CASE : if (args.length>0) function_data.get("return_value").set(args[0].toString().toLowerCase()); break;
			case Global.F_TO_UPPER_CASE : if (args.length>0) function_data.get("return_value").set(args[0].toString().toUpperCase()); break;
			case Global.F_VALUE_TYPE : if (args.length>0) function_data.get("return_value").set(args[0].typeNameDirect()); break;
		}
	}


	/** executes the hard coded script function date_day_of_month(year,month,day) */
	private final static void execute_date_day_of_month (final Structure function_data)  {
		final Structure args = function_data.get("argument").structure();
		if (args.get("size").integer() >= 3 && args.get("0").type() == Value.INTEGER && args.get("1").type() == Value.INTEGER && args.get("2").type() == Value.INTEGER) {
			final int year = args.get("0").integer();
			final int month = args.get("1").integer();
			final int day = args.get("2").integer();
			function_data.get("return_value").set(new GregorianCalendar(year, month, day).get(GregorianCalendar.DAY_OF_MONTH));
		}
	}


	/** executes the hard coded script function date_days_in_month(year,month,day) */
	private final static void execute_date_days_in_month (final Structure function_data)  {
		final Structure args = function_data.get("argument").structure();
		if (args.get("size").integer() >= 3 && args.get("0").type() == Value.INTEGER && args.get("1").type() == Value.INTEGER && args.get("2").type() == Value.INTEGER) {
			final int year = args.get("0").integer();
			final int month = args.get("1").integer();
			final int day = args.get("2").integer();
			function_data.get("return_value").set(new GregorianCalendar(year, month, day).getActualMaximum(GregorianCalendar.DAY_OF_MONTH));
		}
	}


	/** executes the hard coded script function date_text(year,month,day) */
	private final static void execute_date_text (final Structure function_data)  {
		final Structure args = function_data.get("argument").structure();
		if (args.get("size").integer() >= 3 && args.get("0").type() == Value.INTEGER && args.get("1").type() == Value.INTEGER && args.get("2").type() == Value.INTEGER) {
			final int year = args.get("0").integer();
			final int month = args.get("1").integer();
			final int day = args.get("2").integer();
			function_data.get("return_value").set(DateFormat.getDateInstance(DateFormat.LONG).format(new GregorianCalendar(year, month, day).getTime()));
		}
	}


	/** executes the hard coded script function debug(object,to_console) */
	private final static void execute_debug (final Structure function_data)  {
		final Value vwhat = function_data.get("argument").get("0");
		final Value vwhere = function_data.get("argument").get("1");
		if (vwhat != null) {
			PrintStream where = (vwhere != null && vwhere.equals(false)) ? System.err : System.out;
			if (vwhat.typeDirect() == Value.FUNCTION)
				vwhat.function().print(where, "", true);
			else if (vwhat.type() == Value.STRUCTURE)
				vwhat.structure().print(where, "", true);
			else
				where.println(vwhat.toString());
		}
	}


	private final static void execute_delete (final Structure function_data)  {
		final Value varray = function_data.get("argument").get("0");
		final Value vindex = function_data.get("argument").get("1");
		if (varray.type() == Value.STRUCTURE && vindex.type() == Value.INTEGER) {
			final Value v = varray.structure().deleteFromArray(vindex.integer());
			if (v != null)
				function_data.putDirectlyIntoStringTreeMap("return_value", v);
		}
	}


	/** executes the hard coded script function filelist(x) */
	private final static void execute_filelist (final Structure function_data)  {
		final Value v = function_data.get("argument").get("0");
		if (v != null && v.toString().indexOf("..") == -1 && v.toString().indexOf(":") == -1 && v.toString().indexOf("~") == -1) {
			try {
				File d = new File(("rulesets/"+Global.getCurrentRuleset().getName()+"/"+v.toString()).replace('/', File.separatorChar));
				if (d.exists() && d.isDirectory()) {
					// create an alphabetical list of file names for the files in this folder
					final File[] f = d.listFiles();
					int count = 0;
					final String[] name = new String[f.length];
					for (int i = 0; i < f.length; i++) {
						if (f[i].isFile()) {
							// insert it in the alphabetical list of file names
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
					final Structure a = new Structure("ARRAY");
					for (int i = 0; i < count; i++)
						a.add("").set(name[i]);
					function_data.get("return_value").set(a);
					return;
				}
			} catch (Throwable t) { t.printStackTrace(); }
		}
		function_data.get("return_value").set(new Structure("ARRAY"));
	}


	/** searches for a value in an array or a substring in a string */
	private final static void execute_image_exists (final Structure function_data)  {
		final String name = (function_data.get("argument").get("0")==null) ? "UNDEFINED" : function_data.get("argument").get("0").toString();
		function_data.get("return_value").set(AbstractView.getImage(name) != null);
	}


	/** searches for a value in an array or a substring in a string */
	private final static void execute_indexof (final Structure function_data)  {
		final Value vwhat = function_data.get("argument").get("0");
		final Value vwhere = function_data.get("argument").get("1");
		final Value vdirection = function_data.get("argument").get("2");
		final Value vstart = function_data.get("argument").get("3");
		final boolean direction = !(vdirection != null && vdirection.equals(false));
		if (vwhat != null && vwhere != null) {
			if (vwhere.type() == Value.STRING) {
				// we're looking for a substring in a string
				final String s = vwhere.string(), what = vwhat.toString();
				final int size = s.length();
				if (direction) {
					// search forwards
					final int start = (vstart != null && vstart.type() == Value.INTEGER) ? vstart.integer() : 0;
					int ret = s.indexOf(what, start);
					if (ret >= 0)
						function_data.get("return_value").set(ret);
				}
				else {
					// search backwards
					final int start = (vstart != null && vstart.type() == Value.INTEGER) ? vstart.integer() : size-1;
					int ret = s.lastIndexOf(what, start);
					if (ret >= 0)
						function_data.get("return_value").set(ret);
				}
			}
			else if (vwhere.type() == Value.STRUCTURE && vwhere.get("structure_type").equals("ARRAY")) {
				// search for a value in an array
				final Structure array = vwhere.structure();
				final int size = array.get("size").integer();
				if (direction) {
					// search forwards
					final int start = (vstart != null && vstart.type() == Value.INTEGER && vstart.integer() >= 0) ? vstart.integer() : 0;
					final int end = size-1;
					for (int i = start; i <= end; i++)
						if (array.get(i+"").equals(vwhat)) {
							function_data.get("return_value").set(i);
							break;
						}
				}
				else {
					// search backwards
					final int start = (vstart != null && vstart.type() == Value.INTEGER && vstart.integer() < size) ? vstart.integer() : (size-1);
					for (int i = start; i >= 0; i--)
						if (array.get(i+"").equals(vwhat)) {
							function_data.get("return_value").set(i);
							break;
						}
				}
			}
		}
	}


	private final static void execute_insert (final Structure function_data)  {
		final Value varray = function_data.get("argument").get("0");
		final Value vindex = function_data.get("argument").get("1");
		if (varray.type() == Value.STRUCTURE && vindex.type() == Value.INTEGER) {
			final Value v = varray.structure().insertIntoArray(vindex.integer());
			if (v != null)
				function_data.putDirectlyIntoStringTreeMap("return_value", v);
		}
	}


	private final static void execute_pixelheight (final Structure function_data)  {
		final Value v = function_data.get("argument").get("0");
		final AbstractView view = Global.getViewWrapper().getView();
		if (v != null)
			function_data.get("return_value").set(view.height(v));
	}


	private final static void execute_pixelwidth (final Structure function_data)  {
		final Value v = function_data.get("argument").get("0");
		final AbstractView view = Global.getViewWrapper().getView();
		if (v != null)
			function_data.get("return_value").set(view.width(v));
	}


	private final static void execute_random (final Structure function_data)  {
		final Value v1 = function_data.get("argument").get("0");
		final Value v2 = function_data.get("argument").get("1");
		if (v1 != null && v2 != null) {
			if (v1.type() == Value.INTEGER && v2.type() == Value.INTEGER) {
				int start = v1.integer();
				int end = v2.integer();
				// make sure start <= end
				if (start > end) {
					final int k = start;
					start = end;
					end = k;
				}
				function_data.get("return_value").set(Global.random.nextInt(end-start+1)+start);
			}
			if (v1.type() == Value.STRING && v2.type() == Value.STRING) {
				String sstart = v1.string();
				String send = v2.string();
				if (sstart.length() == 1 && send.length() == 1) {
					int start = (int) sstart.charAt(0);
					int end = (int) send.charAt(0);
					// make sure start <= end
					if (start > end) {
						final int k = start;
						start = end;
						end = k;
					}
					function_data.get("return_value").set(((char)(Global.random.nextInt(end-start+1)+start)) + "");
				}
			}
		}
	}


	/** executes the hard coded script function size(x) */
	private final static void execute_size (final Structure function_data)  {
		final Value v = function_data.get("argument").get("0");
		if (v != null) {
			if (v.type() != Value.STRUCTURE)
				function_data.get("return_value").set(v.toString().length());
			else if (v.get("structure_type").equals("ARRAY"))
				function_data.get("return_value").set(v.get("size"));
		}
	}


	/** executes the hard coded script function substring(x) */
	private final static void execute_substring (final Structure function_data)  {
		final Value arg = function_data.get("argument");
		final Value v = arg.get("0"), from = arg.get("1"), to = arg.get("2");
		if (v != null) {
			final String s = v.toString();
			int ifrom = 0, ito = s.length();
			try {
				ifrom = from.integer();
			} catch (Throwable t) {}
			if (ifrom > s.length())
				ifrom = s.length();
			if (ifrom < 0)
				ifrom = 0;
			try {
				ito = to.integer();
			} catch (Throwable t) {}
			if (ito > s.length())
				ito = s.length();
			if (ito < ifrom)
				ito = ifrom;
			function_data.get("return_value").set(s.substring(ifrom,ito));
		}
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