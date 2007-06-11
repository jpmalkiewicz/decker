package decker.util;


/**
*	 a balanced tree that maps strings to objects
*   2007-01-27 removed support for storing/retrieving primitive values
*/
public class StringTreeMap
{
	private TreeNode root;
	private int size;
	private boolean ignoreKeyCase;


	public StringTreeMap () {}


	public StringTreeMap (final boolean _ignoreKeyCase) {
		ignoreKeyCase = _ignoreKeyCase;
	}


	public final void clear () {
		if (root != null) {
			root.clear();
			root = null;
			size = 0;
		}
	}


	public boolean containsKey (final String key) {
		if (key == null) {
			throw new NullPointerException("key must not be null");
		}
		final String k = ignoreKeyCase?key.toLowerCase():key;
		TreeNode t = root;
		while (t != null) {
			final int c = k.compareTo(t.key);
			if (c == 0) {
				return true;
			}
			else {
				t = (c<0)?t.left:t.right;
			}
		}
		return false;
	}


	public final Object get (final String key) {
		if (key == null) {
			throw new NullPointerException("key must not be null");
		}
		final String k = ignoreKeyCase?key.toLowerCase():key;
		TreeNode t = root;
		while (t != null) {
			final int c = k.compareTo(t.key);
			if (c == 0) {
				return t.value;
			}
			else {
				t = (c<0)?t.left:t.right;
			}
		}
		return null;
	}


	public final Object get (final String key, final Object defaultValue) { final Object v = get(key); return (v!=null) ? v : defaultValue; }
	public final StringTreeMap.Iterator getIterator () { final Iterator ret = new Iterator(); ret.reset(this); return ret; }


	/** returns the overwritten value, if any */
	public final Object put (final String key, final Object value) {
		if (key == null) {
			throw new NullPointerException("key must not be null");
		}
		final String k = ignoreKeyCase?key.toLowerCase():key;
		if (root == null) {
			// the tree is empty, create a new root
			root = new TreeNode(null, k, value);
			size = 1;
			return null;
		}

		// find the parent of the new node
		// if the key already exists, replace the current value with the new value)
		TreeNode t = root;
		while (true) {
			final int c = k.compareTo(t.key);
			if (c < 0) {
				if (t.left != null) {
					t = t.left;
				}
				else {
					t.left = new TreeNode(t, k, value);
					t.rebalanceTree();
					size++;
					return null;
				}
			}
			else if (c > 0) {
				if (t.right != null) {
					t = t.right;
				}
				else {
					t.right = new TreeNode(t, k, value);
					t.rebalanceTree();
					size++;
					return null;
				}
			}
			else {
				// the key already exists. replace the value
				final Object ret = t.value;
				t.value = value;
				return ret;
			}
		}
	}


	/** returns the removed value, if any */
	public final Object remove (final String key) {
		if (key == null) {
			throw new NullPointerException("key must not be null");
		}
		final String k = ignoreKeyCase?key.toLowerCase():key;
		// find and return the value of the key
		TreeNode t = root;
		while (t != null) {
			final int c = k.compareTo(t.key);
			if (c < 0)
				t = t.left;
			else if (c > 0)
				t = t.right;
			else {
				final Object ret = t.value;
				t.remove();
				size--;
				return ret;
			}
		}
		return null;
	}


	public int size () {
		return size;
	}


/*	public String toString () {
		return "[StringTreeMap,size="+size+"]";
	}
*/

// methods for testing **************************************************************************************************


/*	public static void main (String[] args) {

		final StringTreeMap t = new StringTreeMap();
		// test rebalancing when adding keys
		t.addTest("a");
		t.addTest("b");
		t.addTest("c");
		t.addTest("d");
		t.addTest("e");
		t.removeTest(t.root.key);
		t.removeTest(t.root.key);
		t.removeTest(t.root.key);
		t.clear();
		System.out.println();
		t.addTest("c");
		t.addTest("a");
		t.addTest("b");
		t.clear();
		System.out.println();
		// test enumeration;
		t.addTest("f");
		t.addTest("b");
		t.addTest("h");
		t.addTest("g");
		t.addTest("a");
		t.addTest("d");
		t.addTest("c");
		t.addTest("i");
		t.addTest("j");
		Iterator e = t.getIterator();
		while (e.hasNext())
			System.out.print(((String)e.next().getValue)+" ");
		System.out.println();
	}


	private void addTest (String s) {
		put(s,s);
		printTree();
	}


	// prints the tree in pre order
	private void printTree () {
		if (root != null)
			printTree(root);
		System.out.println();
	}


	private void printTree (final TreeNode t) {
		System.out.print("(");
		if (t.left != null) {
			printTree(t.left);
			System.out.print(" ");
		}
		System.out.print(t.key);
		if (t.right != null) {
			System.out.print(" ");
			printTree(t.right);
		}
		System.out.print(")");
	}


	private void removeTest(String s) {
		remove(s);
		printTree();
	}
*/

// TreeNode inner class *************************************************************************************************


	public final class TreeNode
	{
		private int height;
		private TreeNode parent, left, right;
		private String key;
		private Object value;


		TreeNode (final TreeNode _parent, final String _key, final Object _value) {
			parent = _parent;
			key = _key;
			value = _value;
		}


		private void clear () {
			if (left != null) {
				left.clear();
				left = null;
			}
			if (right != null) {
				right.clear();
				right = null;
			}
			parent = null;
			key = null;
			value = null;
		}


		public String getKey () {
			return key;
		}


		public Object getValue () {
			return value;
		}


		// needed for rebalancing
		private void moveLeftChildUp () {
			final TreeNode r = right;
			right = left;
			left = right.left;
			if (left != null)
				left.parent = this;
			right.left = right.right;
			right.right = r;
			if (r != null)
				r.parent = right;
			final String k = key;
			key = right.key;
			right.key = k;
			final Object v = value;
			value = right.value;
			right.value = v;
		}


		// needed for rebalancing
		private void moveRightChildUp () {
			final TreeNode l = left;
			left = right;
			right = left.right;
			if (right != null)
				right.parent = this;
			left.right = left.left;
			left.left = l;
			if (l != null)
				l.parent = left;
			final String k = key;
			key = left.key;
			left.key = k;
			final Object v = value;
			value = left.value;
			left.value = v;
			// height may have changed for two of the nodes involved
			left.recalculateHeight();
			recalculateHeight();
		}


		private void rebalanceTree () {
			// determine the new tree height
			final int oldHeight = height;
			final int leftHeight = (left == null)?0:(left.height+1);
			final int rightHeight = (right == null)?0:(right.height+1);
			final int newHeight = (leftHeight>rightHeight)?leftHeight:rightHeight;
			// rebalance the tree if necessary
			if (leftHeight > rightHeight+1) {
				// left child tree is too high, move it up
				if (left.right != null &&( left.left == null || left.right.height > left.left.height )) {
					// left.right has grown, an extra move is necessary
					left.moveRightChildUp();
				}
				moveLeftChildUp();
			}
			else if (rightHeight > leftHeight+1) {
				// right child tree is too high, move it up
				if (right.left != null &&( right.right == null || right.left.height > right.right.height )) {
					// right.left has grown, an extra move is necessary
					right.moveLeftChildUp();
				}
				moveRightChildUp();
			}
			else if (height != newHeight) {
				height = newHeight;
				// if the tree height has changed, the parent node may need to be rebalanced
				if (parent != null)
					parent.rebalanceTree();
			}
		}


		private void recalculateHeight () {
			final int leftHeight = (left == null)?0:(left.height+1);
			final int rightHeight = (right == null)?0:(right.height+1);
			height = (leftHeight>rightHeight)?leftHeight:rightHeight;
		}


		/** returns the new root node */
		private void remove () {
			// found the key, now remove the node
			if (left == null) {
				if (right == null) {
					// it was a leaf
					if (parent == null)
						root = null;
					else {
						if (parent.left == this)
							parent.left = null;
						else
							parent.right = null;
					}
				}
				else {
					// move the node's right child up to replace it
					right.parent = parent;
					if (parent == null)
						root = right;
					else {
						if (parent.left == this)
							parent.left = right;
						else
							parent.right = right;
					}
				}
			}
			else { // left != null
				if (right == null) {
					// move the node's left child up to replace it
					left.parent = parent;
					if (parent == null)
						root = left;
					else {
						if (parent.left == this)
							parent.left = left;
						else
							parent.right = left;
					}
				}
				else {
					// both children exist, get one node from the larger of the two child trees, and replace this node with it
					TreeNode t;
					if (left.height > right.height) {
						t = left;
						while (t.right != null)
							t = t.right;
					}
					else {
						t = right;
						while (t.left != null)
							t = t.left;
					}
					key = t.key;
					value = t.value;
					t.remove();
					// no need to rebalance this node's parent or wipe the node, so return
					return;
				}
			}
			// the node has been removed from the structure, now rebalance and wipe it clean
			if (parent != null)
				parent.rebalanceTree();
			parent = null;
			left = null;
			right = null;
			key = null;
			value = null;
		}


		public Object setValue (final Object o) {
			final Object ret = value;
			value = o;
			return ret;
		}
	}


// Iterator  inner class ************************************************************************************************


	public final static class Iterator
	{
		private TreeNode nextElement;


		public boolean hasNext () {
			return nextElement != null;
		}


		public Object next () {
			return nextNode().value;
		}


		public String nextKey () {
			return nextNode().key;
		}


		public TreeNode nextNode () {
			final TreeNode ret = nextElement;
			if (ret != null) {
				if (nextElement.right != null) {
					nextElement = nextElement.right;
					while (nextElement.left != null) {
						nextElement = nextElement.left;
					}
				}
				else {
					boolean right;
					do {
						right = nextElement.parent != null && nextElement.parent.right == nextElement;
						nextElement = nextElement.parent;
					} while (right);
				}
			}
			return ret;
		}


		public Object nextString () {
			return nextNode().value.toString();
		}


		public void reset (final StringTreeMap stm) {
			nextElement = stm.root;
			if (nextElement != null) {
				while (nextElement.left != null) {
					nextElement = nextElement.left;
				}
			}
		}
	}
}