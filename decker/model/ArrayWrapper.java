package decker.model;
import java.io.PrintStream;


public class ArrayWrapper
{
	public Value[] array;


	public ArrayWrapper(final Value[] _array)  {
		array = _array;
	}


	boolean print (final PrintStream out, final String indentation, boolean line_start)  {
		if (array.length == 0) {
			out.print((line_start?indentation:"") + "ARRAY");
			return false;
		}
		out.println((line_start?indentation:"") + "ARRAY");
		final String ind = indentation+Global.BLOCK_INDENT;
		final Value[] a = array;
		final int al = a.length;
		for (int i = 0; i < al; i++) {
			final int ait = a[i].typeDirect();
			if (ait == Value.STRUCTURE) {
				if (!a[i].structure().print(out, ind, true))
					out.println();
			}
			else if (ait == Value.FUNCTION) {
				if (!a[i].function().print(out, ind, true))
					out.println();
			}
			else if (ait == Value.ARRAY) {
				if (!a[i].arrayWrapper().print(out, ind, true))
					out.println();
			}
			else
				out.println(ind+a[i].toStringForPrinting());
		}
		return true;
	}
}