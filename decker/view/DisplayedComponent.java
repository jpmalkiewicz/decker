package decker.view;
import decker.model.*;
import decker.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;


public class DisplayedComponent implements ValueListener
{
	final static String[] EVENT_FUNCTION_NAME = { "on_key_down",   "on_mouse_down",   "on_mouse_dragged",   "on_mouse_entered",   "on_mouse_exited",   "on_mouse_moved",   "on_mouse_up" };
	final static int                               ON_KEY_DOWN = 0, ON_MOUSE_DOWN = 1, ON_MOUSE_DRAGGED = 2, ON_MOUSE_ENTERED = 3, ON_MOUSE_EXITED = 4, ON_MOUSE_MOVED = 5, ON_MOUSE_UP =6;
	// when calling DisplayedComponentt.update(), derived classes use these constants to tell the default update algorithm which settings have custom functions
	final static int CUSTOM_SIZE = 0x1;

	private final static DisplayedComponent[][] eventListener = new DisplayedComponent[EVENT_FUNCTION_NAME.length][5];
	private final static int[] eventListenerCount = new int[EVENT_FUNCTION_NAME.length];
	private static DisplayedComponent[] mouseIsInside = new DisplayedComponent[10]; // this is the list of components where the mouse was inside the last time a mouse event occured. only components which listen to on_mouse_entered or to on_mouse_exited will get added
	private static int mouseIsInsideCount;
	private static DisplayedComponent currentScreen;




	// the displayed component
	Value component;
	// the parent Structure of this component
	DisplayedComponent parent;
	// the bounding rectangle
	int x, y, w = Integer.MIN_VALUE, h = Integer.MIN_VALUE;
	boolean relative_to_parent_width, relative_to_parent_height;
	// the clipped bounding rectangle. if the component is invisible w is <= 0, h possibly too
	int cx, cy, cw, ch;

	// the shape of the component, if it's a STRUCTURE
	private BufferedImage shape;
	// child structures of this component
	DisplayedComponent[] child;
	int child_count;
	int children_relative_to_width, children_relative_to_height;
	// the event functions, if they exist
	final Function[] scriptedEventFunction = new Function[EVENT_FUNCTION_NAME.length];
	final boolean[] hasHardcodedEventFunction = new boolean[EVENT_FUNCTION_NAME.length]; // eventUserInput() gets called for all hard coded event functions, so taht's the function you'll override
	final boolean[] eventFunctionRegistered = new boolean[EVENT_FUNCTION_NAME.length]; // true whenever this DisplayedComponent is a registered listener for an event type
	boolean mouse_is_inside;




// ******************************************************************************************************************************************************
// static methods ***************************************************************************************************************************************
// ******************************************************************************************************************************************************




	final static DisplayedComponent createDisplayedComponent (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		DisplayedComponent ret = null;
		if (_component.type() != Value.STRUCTURE) {
			throw new RuntimeException(_component+" is not a structure");
		}
		else { // it's a structure
			final String t = _component.get("structure_type").string();
			if (t.equals("BUTTON") || t.equals("BORDER_BUTTON"))
				ret = new UIButton(_component, _parent, current_clip_source);
			else if (t.equals("BORDER"))
				ret = new UIBorder(_component, _parent, current_clip_source);
			else if (t.equals("IMAGE"))
				ret = new UIImage(_component, _parent, current_clip_source);
			else if (t.equals("SCROLLBAR"))
				ret = new UIScrollbar(_component, _parent, current_clip_source);
			else if (t.equals("TABLE"))
				ret = new UITable(_component, _parent, current_clip_source);
			else if (t.equals("TEXT"))
				ret = new UIText(_component, _parent, current_clip_source);
			else if (t.equals("TEXTFIELD"))
				ret = new UITextField(_component, _parent, current_clip_source);
		}
		if (ret == null) {
System.out.print("(generic "+_component+") ");
			ret = new DisplayedComponent(_component, _parent, current_clip_source);
		}
		if (ret.child_count == -1) {
			ret.updateChildren(current_clip_source);
			ret.eventSizeChanged(current_clip_source, Integer.MIN_VALUE, Integer.MIN_VALUE, false, false);
		}
		// call the "on_resize" function if there is one
		final Value v;
		if (_component.type() == Value.STRUCTURE && (v=_component.get("on_resize")) != null && v.type() == Value.FUNCTION) {
			FunctionCall.executeFunctionCall(v.function(), new Value[]{ new Value().set(ret.w), new Value().set(ret.h) }, _component.structure());
		}
		return ret;
	}




	final static int determineX (final Value component, final int parent_x, final int parent_width, final int width)  {
		int ret = parent_x;
		// if it's the top level view (parent_width==Integer.MIN_VALUE) it automatically sits at (0,0)
		if (parent_width > Integer.MIN_VALUE && component.type() == Value.STRUCTURE) {
			final Value p = component.get("x");
			final Value a = component.get("h_align");
			final int ptype = (p==null) ? (-1) : p.type();
			final int atype = (a==null) ? (-1) : a.type();
			if (p != null) {
				switch (ptype) {
					case Value.INTEGER :
						ret += p.integer();
					break;
					case Value.REAL :
						if (a == null || atype != Value.REAL)
							ret += (int) (p.real() + 0.500000001);
						else
							ret += (int) (p.real() + a.real() + 0.500000001); // to avoid rounding errors adding up
					break;
					case Value.CONSTANT :
						if (p.equalsConstant("RIGHT")) {
							// if the visible_object has no explicit horizontal alignment, treat it as LEFT aligned, i.e. sitting left of (x,y)
							if (a == null ||( atype != Value.INTEGER && atype != Value.REAL && atype != Value.CONSTANT )||( atype == Value.CONSTANT && !a.equalsConstant("LEFT") && !a.equalsConstant("CENTER") && !a.equalsConstant("RIGHT") ))
								ret += parent_width-width;
							else
								ret += parent_width;
						}
						else if (p.equalsConstant("CENTER")) {
							// if the visible_object has no explicit horizontal alignment, treat it as CENTER aligned, i.e. sitting centered on (x,y)
							if (a == null ||( atype != Value.INTEGER && atype != Value.REAL && atype != Value.CONSTANT )||( atype == Value.CONSTANT && !a.equalsConstant("LEFT") && !a.equalsConstant("CENTER") && !a.equalsConstant("RIGHT") ))
								ret += (parent_width-width)/2;
							else
								ret += parent_width/2;
						}
						// else if (p.equalsConstant("LEFT"))  // this case has no effect as x is already assumed to be (parent_x+0)
						//		ret += 0;
					break;
				}
			}
			if (a != null) {
				switch (atype) {
					case Value.INTEGER :
						ret += a.integer();
					break;
					case Value.REAL :
						if (ptype != Value.REAL) // otherwise a has already been added
							ret += (int) (a.real()+0.500000001);
					break;
					case Value.CONSTANT :
						if (a.equalsConstant("CENTER"))
							ret -= width/2;
						else if (a.equalsConstant("LEFT"))
							ret -= width;
						//	else if (a.equalsConstant("RIGHT"))  // this has no effect apart from keeping x=CENTER and x=RIGHT from assuming a h_align value that differs from RIGHT
						//		ret -= 0;
					break;
				}
			}
		}
		return ret;
	}




	final static int determineY (final Value component, final int parent_y, final int parent_height, final int height)  {
		int ret = parent_y;
		// if it's the top level vieh (parent_height==Integer.MIN_VALUE) it automatically sits at (0,0)
		if (parent_height > Integer.MIN_VALUE && component.type() == Value.STRUCTURE) {
			final Value p = component.get("y");
			final Value a = component.get("v_align");
			final int ptype = (p==null) ? (-1) : p.type();
			final int atype = (a==null) ? (-1) : a.type();
			if (p != null) {
				switch (ptype) {
					case Value.INTEGER :
						ret += p.integer();
					break;
					case Value.REAL :
						if (a == null || atype != Value.REAL)
							ret += (int) (p.real() + 0.500000001);
						else
							ret += (int) (p.real() + a.real() + 0.500000001); // to avoid rounding errors adding up
					break;
					case Value.CONSTANT :
						if (p.equalsConstant("BOTTOM")) {
							// if the visible_object has no eyplicit horizontal alignment, treat it as LEFT aligned, i.e. sitting left of (x,y)
							if (a == null ||( atype != Value.INTEGER && atype != Value.REAL && atype != Value.CONSTANT )||( atype == Value.CONSTANT && !a.equalsConstant("TOP") && !a.equalsConstant("CENTER") && !a.equalsConstant("BOTTOM") ))
								ret += parent_height-height;
							else
								ret += parent_height;
						}
						else if (p.equalsConstant("CENTER")) {
							// if the visible_object has no eyplicit horizontal alignment, treat it as CENTER aligned, i.e. sitting centered on (x,y)
							if (a == null ||( atype != Value.INTEGER && atype != Value.REAL && atype != Value.CONSTANT )||( atype == Value.CONSTANT && !a.equalsConstant("TOP") && !a.equalsConstant("CENTER") && !a.equalsConstant("BOTTOM") ))
								ret += (parent_height-height)/2;
							else
								ret += parent_height/2;
						}
						//	else if (p.equalsConstant("TOP"))  // this case has no effect as y is already assumed to be (parent_y+0)
						//		ret += 0;
					break;
				}
			}
			if (a != null) {
				switch (atype) {
					case Value.INTEGER :
						ret += a.integer();
					break;
					case Value.REAL :
						if (ptype != Value.REAL) // otherhise a has already been added
							ret += (int) (a.real()+0.500000001);
					break;
					case Value.CONSTANT :
						if (a.equalsConstant("CENTER"))
							ret -= height/2;
						else if (a.equalsConstant("TOP"))
							ret -= height;
						//	else if (a.equalsConstant("BOTTOM"))  // this has no effect apart from keeping the cases y=CENTER and y=BOTTOM from assuming a h_align value that differs from BOTTOM
						//		ret -= 0;
					break;
				}
			}
		}
		return ret;
	}




	public final static void drawScreen (final Graphics g) {
		if (currentScreen != null)
			currentScreen.child[0].draw(g);
	}




	private final static void handleKeyDown (final KeyEvent e) {
		// if there is no key listener, don't waste time generating the event info
		if (eventListenerCount[ON_KEY_DOWN] == 0)
			return;
		// turn the pressed key into a single letter, if it is one, or a description string
		final char c = e.getKeyChar();
		Value key = new Value().set((c==KeyEvent.CHAR_UNDEFINED)?"":(c+""));
		switch (e.getKeyCode()) {
			case KeyEvent.VK_BACK_SPACE : key.set("BACKSPACE"); break;
			case KeyEvent.VK_ESCAPE : key.set("ESCAPE"); break;
			case KeyEvent.VK_LEFT : key.set("LEFT"); break;
			case KeyEvent.VK_RIGHT : key.set("RIGHT"); break;
			case KeyEvent.VK_UP : key.set("UP"); break;
			case KeyEvent.VK_DOWN : key.set("DOWN"); break;
			case KeyEvent.VK_F1 : key.set("F1"); break;
			case KeyEvent.VK_F2 : key.set("F2"); break;
			case KeyEvent.VK_F3 : key.set("F3"); break;
			case KeyEvent.VK_F4 : key.set("F4"); break;
			case KeyEvent.VK_F5 : key.set("F5"); break;
			case KeyEvent.VK_F6 : key.set("F6"); break;
			case KeyEvent.VK_F7 : key.set("F7"); break;
			case KeyEvent.VK_F8 : key.set("F8"); break;
			case KeyEvent.VK_F9 : key.set("F9"); break;
			case KeyEvent.VK_F10 : key.set("F10"); break;
			case KeyEvent.VK_F11 : key.set("F11"); break;
			case KeyEvent.VK_F12 : key.set("F12"); break;
		}
		// add the modifier keys to the event info
		final int modifiers = e.getModifiers();
		final Queue q = new Queue();
		q.add(key);
		if ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) q.add(new Value().set("ALT"));
		if ((modifiers & InputEvent.ALT_GRAPH_MASK) == InputEvent.ALT_GRAPH_MASK) q.add(new Value().set("ALT_GRAPH"));
		if ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) q.add(new Value().set("CTRL"));
		if ((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) q.add(new Value().set("SHIFT"));
		final Value[] args = new Value[q.size()];
		for (int i = 0; i < args.length; i++) {
			args[i] = (Value) q.remove();
		}
		// send the key event to all key listeners
		for (int i = eventListenerCount[ON_KEY_DOWN]; --i >= 0; ) {
			final DisplayedComponent k = eventListener[ON_KEY_DOWN][i];
			if (( !k.hasHardcodedEventFunction[ON_KEY_DOWN] || k.eventUserInput(ON_KEY_DOWN, e, -1, -1, -1, -1) )&& k.scriptedEventFunction[ON_KEY_DOWN] != null) {
				FunctionCall.executeFunctionCall(k.scriptedEventFunction[ON_KEY_DOWN], args, k.component.structure());
			}
		}
	}




	final static boolean hasExplicitX (final Value component)  {
		if (component.type() == Value.STRUCTURE) {
			final Value p = component.get("x");
			final Value a = component.get("h_align");
			final int ptype = (p==null) ? (-1) : p.type();
			final int atype = (a==null) ? (-1) : a.type();
			String s;
			if (p != null &&( ptype == Value.INTEGER || ptype == Value.REAL ||( ptype == Value.CONSTANT &&( (s=p.constant()).equals("LEFT") || s.equals("CENTER") || s.equals("RIGHT") ))))
				return true;
			if (a != null &&( atype == Value.INTEGER || atype == Value.REAL ||( atype == Value.CONSTANT &&( (s=a.constant()).equals("LEFT") || s.equals("CENTER") || s.equals("RIGHT") ))))
				return true;
		}
		return false;
	}




	final static boolean hasExplicitY (final Value component)  {
		if (component.type() == Value.STRUCTURE) {
			final Value p = component.get("y");
			final Value a = component.get("v_align");
			final int ptype = (p==null) ? (-1) : p.type();
			final int atype = (a==null) ? (-1) : a.type();
			String s;
			if (p != null &&( ptype == Value.INTEGER || ptype == Value.REAL ||( ptype == Value.CONSTANT &&( (s=p.constant()).equals("TOP") || s.equals("CENTER") || s.equals("BOTTOM") ))))
				return true;
			if (a != null &&( atype == Value.INTEGER || atype == Value.REAL ||( atype == Value.CONSTANT &&( (s=a.constant()).equals("TOP") || s.equals("CENTER") || s.equals("BOTTOM") ))))
				return true;
		}
		return false;
	}




	/** returns the percentage value, or Integer.MIN_VALUE if it is not a percentage value */
	public final static int getPercentageValue (final String s) {
		if (s != null && s.endsWith("%")) {
			try {
				return Integer.parseInt(s.substring(0, s.length()-1));
			} catch (NumberFormatException ex) {}
		}
		return Integer.MIN_VALUE;
	}




	final static int getScreenHeight () {
		if (currentScreen != null)
			return currentScreen.child[0].h;
		return 0;
	}




	final static int getScreenWidth () {
		if (currentScreen != null)
			return currentScreen.child[0].w;
		return 0;
	}




	final static boolean handleUserInput (final AWTEvent e, final int mouse_x, final int mouse_y, final int mouse_dx, final int mouse_dy) {
		int eventID = -1;
		switch (e.getID()) {
			case MouseEvent.MOUSE_DRAGGED :
				eventID = ON_MOUSE_DRAGGED;
				break;
			case MouseEvent.MOUSE_ENTERED :
			case MouseEvent.MOUSE_MOVED :
			case MouseEvent.MOUSE_EXITED :
				eventID = ON_MOUSE_MOVED;
//				mouse_x = -100000;
//				mouse_y = -100000;
				break;
			case MouseEvent.MOUSE_PRESSED :
				eventID = ON_MOUSE_DOWN;
				break;
			case MouseEvent.MOUSE_RELEASED :
				eventID = ON_MOUSE_UP;
				break;
			case KeyEvent.KEY_PRESSED :
				handleKeyDown((KeyEvent)e);
				return false;
			case KeyEvent.KEY_RELEASED :
				return false;
			default:
				return true;
		}
		// it's a mouse event
		// tell the components about it, if the mouse has left their area, and remove them from the mouseIsInside list
		for (int i = mouseIsInsideCount; --i >= 0; ) {
			final DisplayedComponent c = mouseIsInside[i];
			if (! (mouse_x >= c.cx && mouse_x < c.cx+c.cw && mouse_y >= c.cy && mouse_y < c.cy+c.ch &&( c.shape == null || (c.shape.getRGB(mouse_x-c.x, mouse_y-c.y)&0xff000000) != 0 )) ) {
				c.mouse_is_inside = false;
				if (( !c.hasHardcodedEventFunction[ON_MOUSE_EXITED] || c.eventUserInput(ON_MOUSE_EXITED, e, mouse_x, mouse_y, mouse_dx, mouse_dy) )&& c.scriptedEventFunction[ON_MOUSE_EXITED] != null) {
					FunctionCall.executeFunctionCall(c.scriptedEventFunction[ON_MOUSE_EXITED], new Value[]{ new Value().set(mouse_x-c.x), new Value().set(mouse_y-c.y), new Value().set(false) }, c.component.structure());
				}
				mouseIsInside[i] = mouseIsInside[--mouseIsInsideCount];
				mouseIsInside[mouseIsInsideCount] = null;
			}
		}
		// find the components where the mouse is inside now, and tell them about it
		for (int i = eventListenerCount[ON_MOUSE_ENTERED]; --i >= 0; ) {
			final DisplayedComponent c = eventListener[ON_MOUSE_ENTERED][i];
			if (!c.mouse_is_inside && mouse_x >= c.cx && mouse_x < c.cx+c.cw && mouse_y >= c.cy && mouse_y < c.cy+c.ch &&( c.shape == null || (c.shape.getRGB(mouse_x-c.x, mouse_y-c.y)&0xff000000) != 0 )) {
				// the mouse is inside this component, add it to the mouseIsInside list
				if (mouseIsInsideCount == mouseIsInside.length) {
					final DisplayedComponent[] newMII = new DisplayedComponent[mouseIsInsideCount*2];
					System.arraycopy(mouseIsInside, 0, newMII, 0, mouseIsInsideCount);
					mouseIsInside = newMII;
				}
				mouseIsInside[mouseIsInsideCount++] = c;
				// tell the component that the mouse has entered it
				c.mouse_is_inside = true;
				if (( !c.hasHardcodedEventFunction[ON_MOUSE_ENTERED] || c.eventUserInput(ON_MOUSE_ENTERED, e, mouse_x, mouse_y, mouse_dx, mouse_dy) )&& c.scriptedEventFunction[ON_MOUSE_ENTERED] != null) {
					FunctionCall.executeFunctionCall(c.scriptedEventFunction[ON_MOUSE_ENTERED], new Value[]{ new Value().set(mouse_x-c.x), new Value().set(mouse_y-c.y), new Value().set(true) }, c.component.structure());
				}
			}
		}
		// we also need to check whether the mouse has entered them for those components which are listening for it to exit but not for it to enter
		for (int i = eventListenerCount[ON_MOUSE_EXITED]; --i >= 0; ) {
			final DisplayedComponent c = eventListener[ON_MOUSE_EXITED][i];
			// if the component hasn't been added to the list yet, mouse_is_inside will be false
			if (!c.mouse_is_inside && mouse_x >= c.cx && mouse_x < c.cx+c.cw && mouse_y >= c.cy && mouse_y < c.cy+c.ch &&( c.shape == null || (c.shape.getRGB(mouse_x-c.x, mouse_y-c.y)&0xff000000) != 0 )) {
				// the mouse is inside this component, add it to the mouseIsInside list
				if (mouseIsInsideCount == mouseIsInside.length) {
					final DisplayedComponent[] newMII = new DisplayedComponent[mouseIsInsideCount*2];
					System.arraycopy(mouseIsInside, 0, newMII, 0, mouseIsInsideCount);
					mouseIsInside = newMII;
				}
				mouseIsInside[mouseIsInsideCount++] = c;
				c.mouse_is_inside = true;
			}
		}
		// tell the components about the event itself
		final int listenerCount = eventListenerCount[eventID];
		if (listenerCount > 0) {
System.out.println();
			final DisplayedComponent[] el = eventListener[eventID];
System.out.println(listenerCount + " listeners          mouse at ("+mouse_x+" "+mouse_y+")");
			for (int i = listenerCount; --i >= 0; ) {
				final DisplayedComponent c = el[i];
				if (mouse_x >= c.cx && mouse_x < c.cx+c.cw && mouse_y >= c.cy && mouse_y < c.cy+c.ch &&( c.shape == null || (c.shape.getRGB(mouse_x-c.x, mouse_y-c.y)&0xff000000) != 0 )) {
System.out.println("mouse event inside "+c.getClass().getName());
					// if there is no hardcoded function or the hardcoded function doesn't block the scripted one, call the scripted function
					if (( !c.hasHardcodedEventFunction[eventID] || c.eventUserInput(eventID, e, mouse_x, mouse_y, mouse_dx, mouse_dy) )&& c.scriptedEventFunction[eventID] != null) {
System.out.println("mouse event : "+c.hashCode()+"  "+i+"    "+e);
						if (eventID != ON_MOUSE_MOVED && eventID != ON_MOUSE_DRAGGED)
							FunctionCall.executeFunctionCall(c.scriptedEventFunction[eventID], new Value[]{ new Value().set(mouse_x-c.x), new Value().set(mouse_y-c.y), new Value().set(true) }, c.component.structure());
						else
							FunctionCall.executeFunctionCall(c.scriptedEventFunction[eventID], new Value[]{ new Value().set(mouse_x-c.x), new Value().set(mouse_y-c.y), new Value().set(mouse_dx), new Value().set(mouse_dy), new Value().set(true) }, c.component.structure());
					}
				}
			}
		}
if (eventID == ON_MOUSE_UP)
System.out.println("mouse up END");
		return false;
	}




	public static void setDisplayedScreen (final Value screen) {
		// remove the old event listeners
		for (int i = eventListener.length; --i >= 0; ) {
			final DisplayedComponent[] el = eventListener[i];
			for (int j = eventListenerCount[i]; --j >= 0; ) {
				el[j] = null;
			}
			eventListenerCount[i] = 0;
		}
		// create the data for the new screen
		currentScreen = new DisplayedComponent(screen);
	}




// ******************************************************************************************************************************************************
// object methods ***************************************************************************************************************************************
// ******************************************************************************************************************************************************




	/** this constructor is solely used for the dummy parent component of the current screen */
	DisplayedComponent (final Value _component) {
		component = new Value();
		cx = -100000;
		cy = cx;
		cw = -2*cx;
		ch = -2*cx;
		child = new DisplayedComponent[5];  // for the current screen and its overlays
		child_count = 1;
		child[0] = createDisplayedComponent(_component, this, this);
	}




	DisplayedComponent (final Value _component, final DisplayedComponent _parent) {
		component = _component;
		parent = _parent;
		child_count = -1; // the -1 will tell createDisplayedComponent() that the children still need to be added
	}




	DisplayedComponent (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		component = _component;
		parent = _parent;
		child_count = -1; // the -1 will tell createDisplayedComponent() that the children still need to be added
		if (_component != null) {
			update(0, current_clip_source);
			if (_component.type() == Value.STRUCTURE) {
				_component.structure().addValueListener(this);
			}
		}
	}




	void addEventListener (final int eventID) {
		if (eventListener[eventID].length == eventListenerCount[eventID]) {
			final DisplayedComponent[] dc = eventListener[eventID];
			eventListener[eventID] = new DisplayedComponent[dc.length*2];
			System.arraycopy(dc, 0, eventListener[eventID], 0, dc.length);
		}
		eventListener[eventID][eventListenerCount[eventID]++] = this;
		eventFunctionRegistered[eventID] = true;
	}




	void destroy () {
		// stop listening to the scripted component
		if (component != null && component.type() == Value.STRUCTURE) {
			component.structure().removeValueListener(this);
		}
		// destroy the child components
		for (int i = child_count; --i >= 0; ) {
			child[i].destroy();
		}
		child_count = 0;
		children_relative_to_width = 0;
		children_relative_to_height = 0;
		// remove this component from all event listener lists
		for (int i = eventFunctionRegistered.length; --i >= 0; ) {
			if (eventFunctionRegistered[i]) {
				removeEventListener(i);
			}
		}
	}




	void determineClip (final DisplayedComponent current_clip_source) {
		if (current_clip_source.cw <= 0) {
			cx = 0;
			cy = 0;
			cw = 0;
			ch = 0;
		}
		else {
			if (current_clip_source.cx <= x) {
				cx = x;
				cw = cx + current_clip_source.cw - x;
				if (cw > w) {
					cw = w;
				}
			}
			else {
				cx = current_clip_source.cx;
				cw = current_clip_source.cw;
				if (x+w < cx+cw) {
					cw = x+w-cx;
				}
			}
			if (current_clip_source.cy <= y) {
				cy = y;
				ch = cy + current_clip_source.ch - y;
				if (ch > h) {
					ch = h;
				}
			}
			else {
				cy = current_clip_source.cy;
				ch = current_clip_source.ch;
				if (y+h < cy+ch) {
					ch = y+h-cy;
				}
			}
		}
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {}




	void draw (final Graphics g) {
		final Value display_this = component;
		if (display_this.type() == Value.STRUCTURE) {
			final Structure d = display_this.structure();
			ScriptNode.addStackItem(d);
			Value v = null, v2 = null;
			Shape clip = null;
			// if the structure has an on_draw function, execute it
			if ((v=d.get("on_draw")) != null && v.type() == Value.FUNCTION) {
// gotta store the current draw coordinates, in case they get polled by the on_draw function
				FunctionCall.executeFunctionCall(v.function(), null, ScriptNode.KEEP_STACK);
			}
			// draw the structure
			final String type = d.get("structure_type").toString();
			if (type.equals("TEXT")) {
				// set the font
				if ((v2=d.get("font")) != null && v2.type() == Value.STRING)
					g.setFont(AbstractView.getFont(d.get("font").string(), true));
				// set the text color
				if ((v2=d.get("color")) != null && v2.type() == Value.STRING) {
					final Color c = AbstractView.getColor(v2.string());
					if (c != null)
						g.setColor(c);
				}
				// determine the x coordinate of the string
				String s = d.get("text").toString();
				final FontMetrics fm = AbstractView.getFontMetrics(g.getFont());
				// draw the string
				g.drawString(s, x, y+fm.getAscent());
			}
			else if (type.equals("DRAWING_BOUNDARY")) {
				clip = g.getClip();
				g.clipRect(x, y, w, h);
			}
			else if (type.equals("LINE") && d.get("x2").type() == Value.INTEGER && d.get("y2").type() == Value.INTEGER) {
				if ((v=d.get("color")) != null)
					g.setColor(AbstractView.getColor(v.toString()));
				g.drawLine(x, y, x+d.get("x2").integer()-(x-parent.x), y+d.get("y2").integer()-(y-parent.y));
			}
			else if (type.equals("RECTANGLE")) {
				if ((v=d.get("color")) != null)
					g.setColor(AbstractView.getColor(v.toString()));
				g.fillRect(x, y, w, h);
			}
			// draw the child components of this view component
			final DisplayedComponent[] c = child;
			final int cc = child_count;
			for (int i = 0; i < cc; i++)
				c[i].draw(g);
			// clean up the structure stack
			ScriptNode.removeStackItem(d);
			// restore the clipping area if the currently displayed element has changed it, e.g. a DRAWING_BOUNDARY structure
			if (clip != null)
				g.setClip(clip);
		}
	}




	void eventPositionChanged (final DisplayedComponent current_clip_source, final int dx, final int dy) {
		x += dx;
		y += dy;
		determineClip(current_clip_source);
	}




	void eventSizeChanged (final DisplayedComponent current_clip_source, final int old_width, final int old_height, final boolean old_relative_to_parent_width, final boolean old_relative_to_parent_height) {
		// if it's the dummy parent node, stop here
		if (component == null)
			return;
		// adjust the number of relativ children at the parent component
		if (old_relative_to_parent_width) {
			parent.children_relative_to_width--;
		}
		if (old_relative_to_parent_height) {
			parent.children_relative_to_height--;
		}
		relative_to_parent_width = false;
		relative_to_parent_height = false;
		if (component.type() == Value.STRUCTURE) {
			final Structure c = component.structure();
			// determine whether this component's size or position is relative to its parent's size
			Value v;
			String s;
			relative_to_parent_width  = ( (v=c.get("x")) != null &&( v.equalsConstant("CENTER") || v.equalsConstant("RIGHT")  ))||( (v=c.get("width"))  != null && v.type() == Value.STRING && (s=v.string()).endsWith("%") && Global.isInteger(s.substring(0, s.length()-1)) );
			relative_to_parent_height = ( (v=c.get("y")) != null &&( v.equalsConstant("CENTER") || v.equalsConstant("BOTTOM") ))||( (v=c.get("height")) != null && v.type() == Value.STRING && (s=v.string()).endsWith("%") && Global.isInteger(s.substring(0, s.length()-1)) );
			// if the component's size has changed, update the child sizes and call on_resize
			// new components are marked with an initial width of Integer.MIN_VALUE. for them, the size change is ignored, because it may be called by
			final boolean width_has_changed = w != old_width;
			final boolean height_has_changed = h != old_height;
			if (width_has_changed || height_has_changed) {
				final Value k = c.get("on_resize");
				if (k != null) {
					FunctionCall.executeFunctionCall(k.function(), new Value[]{ new Value().set(w), new Value().set(h) }, component.structure());
				}
				if (( children_relative_to_width > 0 && width_has_changed )||( children_relative_to_height > 0 && height_has_changed )) {
					for (int i = child_count; --i >= 0; ) {
						if (( child[i].relative_to_parent_width && width_has_changed )||( child[i].relative_to_parent_height && height_has_changed )) {
							child[i].update(0, current_clip_source);
						}
					}
				}
			}
			// notify the parent component if this component's size is relative
			if (relative_to_parent_width) {
				parent.children_relative_to_width++;
			}
			if (relative_to_parent_height) {
				parent.children_relative_to_height++;
			}
		}
	}




	/** returns true if the event handler should also call the scripted function for the event (if there is one), false otherwise */
	boolean eventUserInput (final int event_id, final AWTEvent e, final int mouse_x, final int mouse_y, final int mouse_dx, final int mouse_dy) {
		return true;
	}




	public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {
		final DisplayedComponent clip_source = getCurrentClipSource();
		update(0, clip_source);
		updateChildren(clip_source);
	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
//System.out.println("DC.eventValueChanged() "+getClass().getName()+" "+variable_name+" "+old_value+" -> "+new_value);
//System.out.println(x+","+y+" "+w+","+h);
		final DisplayedComponent clip_source = getCurrentClipSource();
		update(0, clip_source);
		updateChildren(clip_source);
//System.out.println(x+","+y+" "+w+","+h);
	}




	DisplayedComponent getCurrentClipSource () {
		DisplayedComponent source = parent;
		while ( source.parent != null )
//		while ( source.component != null && !(source instanceof UIDrawingBoundary) )
			source = source.parent;
		return source;
	}




	void removeEventListener (final int eventID) {
		final DisplayedComponent[] dc = eventListener[eventID];
		for (int i = eventListenerCount[eventID]; --i >= 0; ) {
			if (dc[i] == this) {
				System.arraycopy(dc, i+1, dc, i, eventListenerCount[eventID]-i-1);
				eventListenerCount[eventID]--;
				eventFunctionRegistered[eventID] = false;
				break;
			}
		}
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		relative_to_parent_width = false;
		relative_to_parent_height = false;
		// if the displayed component is not a structure, use the parent's x and y
		if (component.type() != Value.STRUCTURE) {
			x = parent.x;
			y = parent.y;
		}
		// if the displayed component is a Structure, set the structure specific variables
		else { // (component.type() == Value.STRUCTURE)
			final Structure s = component.structure();
			// determine the width
			w = 0;
			boolean width_determined = false;
			Value v = s.get("width");
			if (v != null) {
				switch (v.type()) {
					case Value.INTEGER :
						w = v.integer();
						width_determined = true;
					break;
					case Value.REAL :
						w = (int) (v.real()+0.500000001);
						width_determined = true;
					break;
					case Value.STRING :
						// check whether it's a percentage value
						final String k = v.string();
						if (k.endsWith("%")) {
							try {
								w = (Integer.parseInt(k.substring(0, k.length()-1)) * parent.w + 50)/100;
								width_determined = true;
							} catch (NumberFormatException ex) {}
						}
					break;
				}
			}
			// determine the height
			h = 0;
			boolean height_determined = false;
			v = s.get("height");
			if (v != null) {
				switch (v.type()) {
					case Value.INTEGER :
						h = v.integer();
						height_determined = true;
					break;
					case Value.REAL :
						h = (int) (v.real()+0.500000001);
						height_determined = true;
					break;
					case Value.STRING :
						// check whether it's a percentage value
						final String k = v.string();
						if (k.endsWith("%")) {
							try {
								h = (Integer.parseInt(k.substring(0, k.length()-1)) * parent.h + 50)/100;
								height_determined = true;
							} catch (NumberFormatException ex) {}
						}
					break;
				}
			}
			// if we don't have a valid width or height value yet, try to call the custom size function some derived classes have
			if (( !width_determined || !height_determined )&& (customSettings&CUSTOM_SIZE) != 0) {
				determineSize(width_determined, height_determined, current_clip_source);
			}
			// determine the position
			x = determineX(component, parent.x, parent.w, w);
			y = determineY(component, parent.y, parent.h, h);
			// calculate the bounding rectangle of the visible area
			determineClip(current_clip_source);
			// the component may have a shape
			if ((v=s.get("shape")) != null && v.type() == Value.STRING) {
				shape = (BufferedImage) AbstractView.getImage(v.string(), true);
			}
			// finally update the event listener functions
			updateEventListeners();
		}
	}




	void updateChildren (final DisplayedComponent current_clip_source) {
		// destroy the old children
		for (int i = child_count; --i >= 0; ) {
			child[i].destroy();
		}
		// remove all children from this component
		child_count = 0;
		children_relative_to_width = 0;
		children_relative_to_height = 0;
		// add the child components
		if (component != null && component.type() == Value.STRUCTURE) {
			final Value v = component.get("component");
			if (v != null && !v.equalsConstant("UNDEFINED")) {
				if (v.type() != Value.ARRAY) {
					child = new DisplayedComponent[]{ createDisplayedComponent(v, this, current_clip_source) };
					child_count = 1;
				}
				else {
					final Value[] c = v.array();
					final int clength = c.length;
					child = new DisplayedComponent[clength];
					for (int i = 0; i < clength; i++) {
						if (!c[i].equalsConstant("UNDEFINED")) {
							child[child_count] = createDisplayedComponent(c[i], this, current_clip_source);
							child_count++;
						}
					}
				}
			}
		}
	}




	void updateEventListeners () {
		if (component.type() == Value.STRUCTURE) {
			final Structure s = component.structure();
			for (int i = EVENT_FUNCTION_NAME.length; --i >= 0; ) {
				// if there is a scripted function, store it in scriptedEventFunction[] for later use
				final Value e = s.get(EVENT_FUNCTION_NAME[i]);
				scriptedEventFunction[i] = (e != null && e.type() == Value.FUNCTION) ? e.function() : null;
				// start or stop listening to this event, if neccessary
				if (hasHardcodedEventFunction[i] || scriptedEventFunction[i] != null) {
					if (!eventFunctionRegistered[i]) {
						addEventListener(i);
					}
				}
				else {
					if (eventFunctionRegistered[i]) {
						removeEventListener(i);
					}
				}
			}
		}
	}
}