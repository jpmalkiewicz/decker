package decker.tests;
import decker.model.*;


public class ArrayWrapper_ListenerTest implements ValueListener
{
	private String name;




	public static void main (String[] args) {
		final ArrayWrapper_ListenerTest a = new ArrayWrapper_ListenerTest("a"), b = new ArrayWrapper_ListenerTest("b"), c = new ArrayWrapper_ListenerTest("c");
		final ArrayWrapper aw = new ArrayWrapper(new Value[0]);
		System.out.println("adding a");
		aw.addValueListener(a);
		displayListeners(aw);
		System.out.println("adding b");
		aw.addValueListener(b);
		displayListeners(aw);
		System.out.println("adding a");
		aw.addValueListener(a);
		displayListeners(aw);
		System.out.println("adding c");
		aw.addValueListener(c);
		displayListeners(aw);
		System.out.println("removing b");
		aw.removeValueListener(b);
		displayListeners(aw);
		System.out.println("removing non-existant b");
		aw.removeValueListener(b); // shouldn't do anything
		displayListeners(aw);
		System.out.println("removing a");
		aw.removeValueListener(a);
		displayListeners(aw);
		System.out.println("removing a");
		aw.removeValueListener(a);
		displayListeners(aw);
		System.out.println("removing c");
		aw.removeValueListener(c);
		displayListeners(aw);
		System.out.println("removing non-existant c");
		aw.removeValueListener(c);
		displayListeners(aw);
	}


	private static void displayListeners (final ArrayWrapper aw) {
		System.out.println();
		Object[] o = aw.getValueListenerData();
		final ValueListener[] valueListener = (ValueListener[]) o[0];
		final int[] valueListenerAddCount = (int[]) o[1];
		final int valueListenerCount = ((Value)o[2]).integer();
		System.out.println(valueListenerCount + " listener"+(valueListenerCount==1?"":"s"));
		for (int i = 0; i < valueListenerCount; i++) {
			System.out.println(valueListenerAddCount[i]+" "+((ArrayWrapper_ListenerTest)valueListener[i]).name);
		}
		System.out.println();
	}




	ArrayWrapper_ListenerTest (final String _name) {
		name = _name;
	}


	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
	}


	public void eventValueChanged(int index, ArrayWrapper wrapper, Value old_value, Value new_value) {
		// TODO Auto-generated method stub
		
	}
}