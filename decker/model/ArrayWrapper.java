package decker.model;
import java.io.PrintStream;


public final class ArrayWrapper implements Comparable, ValueListener
{
	public Value[] array;
	private ValueListener[] valueListener;
	private int[] valueListenerAddCount; // if the same ValueListener gets added to this array again, this number counts the times
	private int valueListenerCount;



	public ArrayWrapper (final Value[] _array)  {
		array = _array;
	}



	public void addValueListener (final ValueListener vl) {
		// check whether the listener is already listed
		for (int i = valueListenerCount; --i >= 0; ) {
			if (valueListener[i] == vl) {
				valueListenerAddCount[i]++;
				return;
			}
		}
		// if there have been no value listeners so far, make a new list
		if (valueListener == null) {
			valueListener = new ValueListener[1];
			valueListenerAddCount = new int[1];
		}
		else {
			// if the lists are too small to hold another value listener, make them larger
			if (valueListenerCount == valueListener.length) {
				final int size = valueListenerCount;
				final ValueListener[] vl_new = new ValueListener[size*2];
				System.arraycopy(valueListener, 0, vl_new, 0, size);
				valueListener = vl_new;
				final int[] vlac_new = new int[size*2];
				System.arraycopy(valueListenerAddCount, 0, vlac_new, 0, size);
				valueListenerAddCount = vlac_new;
			}
		}
		// apend the new listener to the lists
		valueListener[valueListenerCount] = vl;
		valueListenerAddCount[valueListenerCount] = 1;
		valueListenerCount++;
	}



	public int compareTo (Object o) {
		if (o == null)
			return 1;
		int ret = o.hashCode() - hashCode();
		if (ret != 0 || o == this)
			return ret;
		if (o instanceof ArrayWrapper) {
			ret = ((ArrayWrapper)o).array.hashCode() - array.hashCode();
			if (ret != 0) {
				return ret;
			}
		}
		else {
			return -1; // the comparator will only be used to compare ArrayWrappers and Structures, so this should work fine
		}
		throw new RuntimeException("identical hash code for different ArrayWrappers");
	}



	public void eventValueChanged (final int index, final ArrayWrapper array, final Value old_value, final Value new_value) {
		for (int i = valueListenerCount; --i >= 0; ) {
			valueListener[i].eventValueChanged(index, array, old_value, new_value);
		}
	}



	public void eventValueChanged (final String variable_name, final Structure structure, final Value old_value, final Value new_value) {
		throw new RuntimeException("this function should never get called");
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
			final int ait = a[i].type();
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



	public void removeValueListener (final ValueListener vl) {
		for (int i = valueListenerCount; --i >= 0; ) {
			if (valueListener[i] == vl) {
				// if the listener has been added more than once, reduce the number of times by one, otherwise remove it
				if (valueListenerAddCount[i] > 1) {
					valueListenerAddCount[i]--;
				}
				else {
					// remove the listener from the list by closing the gap it would leave
					System.arraycopy(valueListener, i+1, valueListener, i, valueListenerCount-i-1);
					System.arraycopy(valueListenerAddCount, i+1, valueListenerAddCount, i, valueListenerCount-i-1);
					valueListenerCount--;
					valueListener[valueListenerCount] = null;
				}
				return;
			}
		}
	}



	/** solely used by test classes */
	public Object[] testGetValueListenerData () {
		return new Object[]{ valueListener, valueListenerAddCount, new Value().set(valueListenerCount) };
	}
}