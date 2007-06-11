package decker.util;


public final class Queue
{
	private int first, size;
	private Object[] entries;

	public Queue () {
		entries = new Object[16]; // entries.length has to be a power of 2
	}


	public synchronized void add (final Object o) {
		if (entries.length == size) {
			final Object[] e = new Object[entries.length*2];
			System.arraycopy(entries, first, e, 0, size-first);
			System.arraycopy(entries, 0, e, size-first, first);
			entries = e;
			first = 0;
		}
		entries[(first+size)%entries.length] = o;
		size++;
	}


	public synchronized Object remove () {
		if (size == 0) {
			return null;
		}
		final Object ret = entries[first];
		entries[first] = null;
		first = ((first+1)&(entries.length-1));
		size--;
		return ret;
	}


	public int size () {
		return size;
	}
}