package decker.util;
import java.io.*;



/** doesn't override all methods */
public final class StringPrintStream extends PrintStream
{
	private final StringBuffer buffer = new StringBuffer();


	public StringPrintStream ()  {
		super(new PipedOutputStream());
	}


	public PrintStream append (final char c)  { buffer.append(c); return this; }


//	public PrintStream append (final CharSequence csq)  { buffer.append(csq); return this; }


//	public PrintStream append(final CharSequence csq, final int start, final int end)  { buffer.append(csq, start, end); return this; }


	public boolean checkError ()  { return false; }


//	public void close ()


//	public void flush ()


//	public PrintStream format(String format, Object... args)


//	public PrintStream format (Locale l, String format, Object... args)


	public void print (boolean b)  { buffer.append(b); }


	public void print(char c)  { buffer.append(c); }


	public void print (char[] s)  { buffer.append(s); }


	public void print (double d)  { buffer.append(d); }


	public void print (float f)  { buffer.append(f); }


	public void print (int i)  { buffer.append(i); }


	public void print (long l)  { buffer.append(l); }


	public void print (Object obj)  { buffer.append(obj.toString()); }


	public void print (String s)  { buffer.append(s); }


//	public PrintStream printf(Locale l, Object... args)


//	public PrintStream printf(Locale l, String format, Object... args)


	public void println ()  { buffer.append('\n'); }


	public void println (boolean b)  { buffer.append(b); buffer.append('\n'); }


	public void println (char c)  { buffer.append(c); buffer.append('\n'); }


	public void println (char[] s)  { buffer.append(s); buffer.append('\n'); }


	public void println (double d)  { buffer.append(d); buffer.append('\n'); }


	public void println (float f)  { buffer.append(f); buffer.append('\n'); }


	public void println (int i)  { buffer.append(i); buffer.append('\n'); }


	public void println (long l)  { buffer.append(l); buffer.append('\n'); }


	public void println (Object obj)  { buffer.append(obj.toString()); buffer.append('\n'); }


	public void println (String s)  { buffer.append(s); buffer.append('\n'); }


	public String toString ()  { return buffer.toString(); }


	public void write (byte[] buf, int off, int len)  { buffer.append(new String(buf, off, len)); }


	public void write (final byte[] b)  { buffer.append(new String(b)); }
}