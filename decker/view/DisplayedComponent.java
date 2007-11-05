package decker.view;
import decker.model.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;


public class DisplayedComponent
{
	final static String[] EVENT_FUNCTION_NAME = { "on_key_down",   "on_mouse_down",   "on_mouse_dragged",   "on_mouse_entered",   "on_mouse_exited",   "on_mouse_moved",   "on_mouse_up" };
	final static int                               ON_KEY_DOWN = 0, ON_MOUSE_DOWN = 1, ON_MOUSE_DRAGGED = 2, ON_MOUSE_ENTERED = 3, ON_MOUSE_EXITED = 4, ON_MOUSE_MOVED = 5, ON_MOUSE_UP =6;
	private final static DisplayedComponent[][] eventListener = new DisplayedComponent[EVENT_FUNCTION_NAME.length][5];
	private final static int[] eventListenerCount = new int[EVENT_FUNCTION_NAME.length];



	// the displayed component
	Value component;
	// the parent Structure of this component
	private DisplayedComponent parent;
	// the bounding rectangle
	int x, y, w, h;
	// the clipped bounding rectangle. if the component is invisible w is <= 0, h possibly too
	int cx, cy, cw, ch;
	// true if the last mouse event was inside the component
	private boolean last_mouse_event_inside;
	// true if the component itself has changed in any way
	private boolean has_changed;

	// the shape of the component, if it's a STRUCTURE
	private BufferedImage shape;
	// child structures of this component
	DisplayedComponent[] child;
	int child_count;
	// the event functions, if they exist
	private final Function[] eventFunction = new Function[EVENT_FUNCTION_NAME.length];



// ******************************************************************************************************************************************************
// static methods ***************************************************************************************************************************************
// ******************************************************************************************************************************************************



	final static DisplayedComponent createDisplayedComponent (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		DisplayedComponent ret = null;
		if (_component.type() != Value.STRUCTURE) {
			ret = new UIImage(_component, _parent, current_clip_source);
		}
		else { // it's a structure
			final String t = _component.get("structure_type").string();
			if (t.equals("BUTTON") || t.equals("BORDER_BUTTON"))
				ret = new UIButton(_component, _parent, current_clip_source);
			else if (t.equals("BORDER"))
				ret = new UIBorder(_component, _parent, current_clip_source);
		}
		if (ret == null) {
System.out.print("(generic) ");
			ret = new DisplayedComponent(_component, _parent, current_clip_source);
		}
		if (ret.child_count == -1) {
			ret.updateChildren(current_clip_source);
		}
		return ret;
	}


	private final static void handleKeyDown (final KeyEvent e) {
System.out.print("handleKeyDown() not implemented");
		for (int i = eventListenerCount[ON_KEY_DOWN]; --i >= 0; ) {

		}
//		v.eventKeyPressed(((KeyEvent)e).getKeyChar(), ((KeyEvent)e).getKeyCode(), ((KeyEvent)e).isAltDown());
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
// tell all the listeners from the last event about it, if the mouse has left their area
		final int listenerCount = eventListenerCount[eventID];
		DisplayedComponent[] parent_list = new DisplayedComponent[10];
System.out.println(listenerCount + " listeners");
		if (listenerCount > 0) {
			final DisplayedComponent[] el = eventListener[eventID];
System.out.println("mouse at ("+mouse_x+" "+mouse_y+")");
			for (int i = listenerCount; --i >= 0; ) {
				final DisplayedComponent c = el[i];
System.out.println("("+c.cx+" "+c.cy+"   "+c.cw+" "+c.ch+")");
				if (mouse_x >= c.cx && mouse_x < c.cx+c.cw && mouse_y >= c.cy && mouse_y < c.cy+c.ch &&( c.shape == null || (c.shape.getRGB(mouse_x-c.x, mouse_y-c.y)&0xff000000) != 0 )) {

					if (eventID != ON_MOUSE_MOVED && eventID != ON_MOUSE_DRAGGED)
						FunctionCall.executeFunctionCall(c.eventFunction[eventID], new Value[]{ new Value().set(mouse_x-c.x), new Value().set(mouse_y-c.y), new Value().set(true) }, c.component.structure());
					else
						FunctionCall.executeFunctionCall(c.eventFunction[eventID], new Value[]{ new Value().set(mouse_x-c.x), new Value().set(mouse_y-c.y), new Value().set(mouse_dx), new Value().set(mouse_dy), new Value().set(true) }, c.component.structure());
System.out.println("mouse event inside "+c.getClass().getName());
				}
			}
		}
if (eventID == ON_MOUSE_UP)
System.out.println("mouse up END");
		return false;
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


	DisplayedComponent (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		component = _component;
		parent = _parent;
		child_count = -1; // the -1 will tell createDisplayedComponent() that the children still need to be added
		if (_component != null)
			update(current_clip_source);
	}


	private void addEventListener (final int eventID) {
		if (eventListener[eventID].length == eventListenerCount[eventID]) {
			final DisplayedComponent[] dc = eventListener[eventID];
			eventListener[eventID] = new DisplayedComponent[dc.length*2];
			System.arraycopy(dc, 0, eventListener[eventID], 0, dc.length);
		}
		eventListener[eventID][eventListenerCount[eventID]++] = this;
	}


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
			// adjust the Frame if this is the top level view
/*			if (parent_width == Integer.MIN_VALUE)  {
				// set the window title - it may have changed during code execution
				if ( (v=d.get("title")) != null && !v.equalsConstant("UNDEFINED") && !v.equals(oldScreenTitle))  {
					oldScreenTitle = v.toString();
					setTitle(oldScreenTitle);
				}
			}
*/			// draw the structure
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
			else if (type.equals("TABLE")) {
//				UITable.draw(x, y, d, g, this);
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


	private void removeEventListener (final int eventID) {
		final DisplayedComponent[] dc = eventListener[eventID];
		for (int i = eventListenerCount[eventID]; --i >= 0; ) {
			if (dc[i] == this) {
				System.arraycopy(dc, i+1, dc, i, eventListenerCount[eventID]-i-1);
				eventListenerCount[eventID]--;
				break;
			}
		}
	}


	private void update (final DisplayedComponent current_clip_source) {
		has_changed = false;

		// if the displayed component is an image, fetch it
		if (component.type() != Value.STRUCTURE) {
			x = parent.x;
			y = parent.y;
		}
		// if the displayed component is a Structure, set the structure specific variables
		else { // (component.type() == Value.STRUCTURE)
			final Structure s = component.structure();
			// determine the bounding rectangle
			x = parent.x + DefaultView.x(s, parent.w, -1);
			y = parent.y + DefaultView.y(s, parent.h, -1);
			w = DefaultView.width(s);
			h = DefaultView.height(s);
			// calculate the bounding rectangle of the visible area
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
			// the component may have a shape
			Value v;
			if ((v=s.get("shape")) != null && v.type() == Value.STRING) {
				shape = (BufferedImage) AbstractView.getImage(v.string(), true);
			}
			// the event handler functions
			for (int i = EVENT_FUNCTION_NAME.length; --i >= 0; ) {
				final Value e = s.get(EVENT_FUNCTION_NAME[i]);
				if (e != null && e.type() == Value.FUNCTION) {
					if (eventFunction[i] == null) {
						addEventListener(i);
					}
					eventFunction[i] = e.function();
				}
				else if (eventFunction[i] != null) {
					removeEventListener(i);
					eventFunction[i] = null;
				}
			}
		}
	}


	void updateChildren (final DisplayedComponent current_clip_source) {
		child_count = 0;
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
}