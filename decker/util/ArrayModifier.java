package decker.util
;

public abstract class ArrayModifier {
	public final static Object[] resize (final Object[] old_array, final Object[] new_array) {
		System.arraycopy(old_array, 0, new_array, 0, (old_array.length>new_array.length)?new_array.length:old_array.length);
		return new_array;
	}

	static public Object[] addElement (final Object[] old_array, final Object[] new_array, final Object o) {
		System.arraycopy(old_array, 0, new_array, 0, old_array.length);
		new_array[old_array.length] = o;
		return new_array;
	}


	/** removes the an entry from the old array */
	static public Object[] removeElement (final Object[] old_array, final Object[] new_array, final int index) {
		if (index >= 0 && index < old_array.length) {
			System.arraycopy(old_array, 0, new_array, 0, index);
			System.arraycopy(old_array, index+1, new_array, index, old_array.length-index-1);
			return new_array;
		}
		return old_array;
	}


	/** removes the last occurence of o in the old array. returns the old array if o doesn't occur in it */
	static public Object[] removeElement (final Object[] old_array, final Object[] new_array, final Object o) {
		for (int i = old_array.length; --i >= 0; ) {
			if (old_array[i] == o) {
				System.arraycopy(old_array, 0, new_array, 0, i);
				System.arraycopy(old_array, i+1, new_array, i, old_array.length-i-1);
				return new_array;
			}
		}
		return old_array;
	}
}