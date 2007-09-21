package decker.view;
import decker.model.*;
import decker.util.Queue;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;



public class DefaultView extends AbstractView
{
	private String old_title = "";
	private int dx, dy;
	private Graphics g;
	private final static Shape NO_CLIP_CHANGE = new java.awt.geom.Line2D.Double();


boolean dummy;
	public void drawContent (final Graphics gr)  {
		final Value v = Global.getDisplayedScreen();
		if (v == null)
			return;
if (!dummy)
{
dummy = true;
//v.structure().print(System.err,"", true);
}
		g = gr;
		drawContent(v, null);

	}


	public void drawContent (final Value display_this, final Structure parent)  {
		if (display_this.type() != Value.STRUCTURE) {
			if (!display_this.equalsConstant("UNDEFINED")) {
				// treat it as a string and fetch the corresponding image
				final String image_name = display_this.toString();
				Image image = getImage(image_name);
				if (image != null)
					g.drawImage(image, dx, dy, null);
			}
		}
		else {
			final Structure d = display_this.structure();
			ScriptNode.addStackItem(d);
			Value v = null, v2 = null;
			Shape clip = NO_CLIP_CHANGE;
			// if the structure has an on_draw function, execute it
			if ((v=d.get("on_draw")) != null && v.typeDirect() == Value.FUNCTION) {
				FunctionCall.executeFunctionCall(v.function(), null, ScriptNode.KEEP_STACK);
			}
			final int x = x(d,parent), y = y(d,parent);
			final int w = width(d), h = height(d);
			// adjust the Frame if this is the top level view
			if (parent == null)  {
				// set the window title - it may have changed during code execution
				if ( (v=d.get("title")) != null && !v.equalsConstant("UNDEFINED") && !v.equals(old_title))  {
					old_title = v.toString();
					setTitle(old_title);
				}
			}
			else { // only change the coordinate offset if this is not the top level screen element
				dx += x;
				dy += y;
			}
			// draw the structure
			final String type = d.get("structure_type").toString();
			if (type.equals("TEXT")) {
				// set the font
				if ((v2=d.get("font")) != null && v2.type() == Value.STRING)
					g.setFont(getFont(d.get("font").string(), true));
				// set the text color
				if ((v2=d.get("color").evaluate()) != null && v2.type() == Value.STRING) {
					final Color c = getColor(v2.string());
					if (c != null)
						g.setColor(c);
				}
				// determine the x coordinate of the string
				String s = d.get("text").toString();
				final FontMetrics fm = getFontMetrics(g.getFont());
				// draw the string
				g.drawString(s, dx, dy+fm.getAscent());
			}
			else if (type.equals("DRAWING_BOUNDARY")) {
				clip = g.getClip();
				g.setClip(dx, dy, w, h);
			}
			else if (type.equals("LINE") && d.get("x2").type() == Value.INTEGER && d.get("y2").type() == Value.INTEGER) {
				if ((v=d.get("color").evaluate()) != null)
					g.setColor(getColor(v.toString()));
				g.drawLine(dx, dy, dx+d.get("x2").integer()-x, dy+d.get("y2").integer()-y);
			}
			else if (type.equals("RECTANGLE")) {
				if ((v=d.get("color").evaluate()) != null)
					g.setColor(getColor(v.toString()));
				g.fillRect(dx, dy, w, h);
			}
			else if (type.equals("TABLE")) {
				UITable.draw(d, g, this);
			}
			// draw the child components of this view component
			v = d.get("component");
			if (v != null) {
				if (v.type() == Value.ARRAY) {
					final Value[] comp = v.array();
					final int ccount = comp.length;
					for (int i = 0; i < ccount; i++)
						if (comp[i].type() == Value.STRUCTURE)
							drawContent(comp[i], d);
				}
				else if (v.type() == Value.STRUCTURE) {
					drawContent(v, d);
				}
			}
			ScriptNode.removeStackItem(d);
			// restore the dx and dy values
			if (parent != null) {
				dx -= x;
				dy -= y;
			}
			// restore the clipping area if the currently displayed element has changed it, e.g. a DRAWING_BOUNDARY structure
			if (clip != NO_CLIP_CHANGE)
				g.setClip(clip);
		}
	}



//*******************************************************************************************************************************************************
// override the empty event methods. events will be handled in connection with view objects *************************************************************
//*******************************************************************************************************************************************************



	public void eventKeyPressed (final char c, final int code, final boolean isAltDown)  {
		final Value v = Global.getDisplayedScreen();
		if (v != null && v.type() == Value.STRUCTURE) {
			final KeyEvent k = (KeyEvent)Global.getViewWrapper().getLastEvent();
			Value key = new Value().set(c+"");
			if (c == KeyEvent.CHAR_UNDEFINED)
				key.set("");
			switch (code) {
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
			final int modifiers = k.getModifiers();
			final Queue q = new Queue();
			q.add(key);
			if (isAltDown) q.add(new Value().set("ALT"));
			if ((modifiers & InputEvent.ALT_GRAPH_MASK) == InputEvent.ALT_GRAPH_MASK) q.add(new Value().set("ALT_GRAPH"));
			if ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) q.add(new Value().set("CTRL"));
			if ((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) q.add(new Value().set("SHIFT"));

			final Value[] args = new Value[q.size()];
			for (int i = 0; i < args.length; i++)
				args[i] = (Value) q.remove();
			eventKeyPressed(args, v.structure());
		}
	}


	private void eventKeyPressed (final Value[] args, final Structure d)  {
		Value v, v2;
		ScriptNode.addStackItem(d);
		// execute the on_key_down function if this structure has one
		if ((v=d.get("on_key_down")) != null && v.typeDirect() == Value.FUNCTION) {
			FunctionCall.executeFunctionCall(v.function(), args, ScriptNode.KEEP_STACK);
		}
		// hand the event on to all sub-components
		v = d.get("component");
		if (v != null) {
			if (v.type() == Value.ARRAY) {
				final Value[] comp = v.array();
				final int ccount = comp.length;
				for (int i = 0; i < ccount; i++)
					if (comp[i].type() == Value.STRUCTURE)
						eventKeyPressed(args, comp[i].structure());
			}
			else if (v.type() == Value.STRUCTURE) {
				eventKeyPressed(args, v.structure());
			}
		}
		// clean up
		ScriptNode.removeStackItem(d);
	}


	public void eventMouseDragged (final int x, final int y, final int dx, final int dy)  { final Value v = Global.getDisplayedScreen(); if (v != null && v.type() == Value.STRUCTURE) eventMouseDragged (x, y, dx, dy, v.structure(), null); }
	public void eventMouseMoved (final int x, final int y)  { final Value v = Global.getDisplayedScreen(); if (v != null && v.type() == Value.STRUCTURE) eventMouseMoved (x, y, v.structure(), null); }
	public void eventMousePressed (final int x, final int y)  { final Value v = Global.getDisplayedScreen(); if (v != null && v.type() == Value.STRUCTURE) eventMousePressed (x, y, v.structure(), null); }
	public void eventMouseReleased (final int x, final int y)  { final Value v = Global.getDisplayedScreen(); if (v != null && v.type() == Value.STRUCTURE) eventMouseReleased (x, y, v.structure(), null); }


	/** returns true iff the event has been consumed */
	private boolean eventMouseDragged (int x, int y, final int dx, final int dy, final Structure d, final Structure parent)  {
		Value v;
		// put the current data object on the stack, in case there are function calls for nested objects
		ScriptNode.addStackItem(d);
		// we have to adjust the event coordinates if d is not the top level view object
		x -= x(d,parent);
		y -= y(d,parent);
		// if the current structure is a button that is not disabled, it may have changed its state
		if (( d.get("structure_type").equals("BUTTON") || d.get("structure_type").equals("BORDER_BUTTON") )&& !d.get("state").equalsConstant("DISABLED")) {
			if (inside(x, y, d)) {
				if (!d.get("state").equalsConstant("PRESSED")) {
					d.get("state").setConstant("PRESSED");
					repaint();
				}
			}
			else {
				if (!d.get("state").equalsConstant("IDLE")) {
					d.get("state").setConstant("IDLE");
					repaint();
				}
			}
		}
		// if the component has an on_mouse_dragged function, call it
		if ((v=d.get("on_mouse_dragged")) != null && v.typeDirect() == Value.FUNCTION && inside(x, y, d)) {
			FunctionCall.executeFunctionCall(v.function(), new Value[]{ new Value().set(x), new Value().set(y), new Value().set(dx), new Value().set(dy) }, ScriptNode.KEEP_STACK);
		}
		// hand the event on to all sub-components
		v = d.get("component");
		if (v != null) {
			if (v.type() == Value.ARRAY) {
				final Value[] comp = v.array();
				final int ccount = comp.length;
				for (int i = 0; i < ccount; i++)
					if (comp[i].type() == Value.STRUCTURE && eventMouseDragged(x, y, dx, dy, comp[i].structure(), d))
						return true;
			}
			else if (v.type() == Value.STRUCTURE) {
				if (eventMouseDragged(x, y, dx, dy, v.structure(), d))
					return true;
			}
		}
		// clean up
		ScriptNode.removeStackItem(d);
		return false;
	}


	/** returns true iff the event has been consumed */
	private boolean eventMouseMoved (int x, int y, final Structure d, final Structure parent)  {
		Value v;
		// put the current data object on the stack, in case there are function calls for nested objects
		ScriptNode.addStackItem(d);
		// we have to adjust the event coordinates if d is not the top level view object
		x -= x(d,parent);
		y -= y(d,parent);
		// process the event
		if (( d.get("structure_type").equals("BUTTON") || d.get("structure_type").equals("BORDER_BUTTON") )&& !d.get("state").equalsConstant("DISABLED")) {
			if (inside(x, y, d)) {
				if (!d.get("state").equalsConstant("HOVER")) {
					d.get("state").setConstant("HOVER");
					repaint();
				}
			}
			else if (!d.get("state").equalsConstant("IDLE")) {
				d.get("state").setConstant("IDLE");
				repaint();
			}
		}
		// hand the event on to all sub-components
		v = d.get("component");
		if (v != null) {
			if (v.type() == Value.ARRAY) {
				final Value[] comp = v.array();
				final int ccount = comp.length;
				for (int i = 0; i < ccount; i++)
					if (comp[i].type() == Value.STRUCTURE && eventMouseMoved(x, y,  comp[i].structure(), d))
						return true;
			}
			else if (v.type() == Value.STRUCTURE) {
				if (eventMouseMoved(x, y, v.structure(), d))
					return true;
			}
		}
		// clean up
		ScriptNode.removeStackItem(d);
		return false;
	}


	/** returns true iff the event has been consumed */
	private boolean eventMousePressed (int x, int y, final Structure d, final Structure parent)  {
		Value v;
		// put the current data object on the stack, in case there are function calls for nested objects
		ScriptNode.addStackItem(d);
		// we have to adjust the event coordinates if d is not the top level view object
		x -= x(d,parent);
		y -= y(d,parent);
		// if the current structure is a button that is not disabled, it may have changed its state
		if (( d.get("structure_type").equals("BUTTON") || d.get("structure_type").equals("BORDER_BUTTON") )&& !d.get("state").equalsConstant("DISABLED")) {
			if (inside(x, y, d)) {
				if (!d.get("state").equalsConstant("PRESSED")) {
					d.get("state").setConstant("PRESSED");
					repaint();
				}
			}
			else {
				if (!d.get("state").equalsConstant("IDLE")) {
					d.get("state").setConstant("IDLE");
					repaint();
				}
			}
		}
		// if the component has an on_mouse_down function, call it
		if ((v=d.get("on_mouse_down")) != null && v.typeDirect() == Value.FUNCTION && inside(x, y, d)) {
			FunctionCall.executeFunctionCall(v.function(), new Value[]{ new Value().set(x), new Value().set(y) }, ScriptNode.KEEP_STACK);
		}
		// hand the event on to all sub-components
		v = d.get("component");
		if (v != null) {
			if (v.type() == Value.ARRAY) {
				final Value[] comp = v.array();
				final int ccount = comp.length;
				for (int i = 0; i < ccount; i++)
					if (comp[i].type() == Value.STRUCTURE && eventMousePressed(x, y, comp[i].structure(), d))
						return true;
			}
			else if (v.type() == Value.STRUCTURE) {
				if (eventMousePressed(x, y, v.structure(), d))
					return true;
			}
		}
		// clean up
		ScriptNode.removeStackItem(d);
		return false;
	}


	/** returns true iff the event has been consumed */
	private boolean eventMouseReleased (int x, int y, final Structure d, final Structure parent)  {
		Value v;
		// put the current data object on the stack, in case there are function calls for nested objects
		ScriptNode.addStackItem(d);
		// we have to adjust the event coordinates if d is not the top level view object
		x -= x(d,parent);
		y -= y(d,parent);

		// if the current structure is a button that is not disabled, it may have changed its state
		if (d.get("structure_type").equals("BUTTON") || d.get("structure_type").equals("BORDER_BUTTON")) {
			if (!d.get("state").equalsConstant("DISABLED")) {
				if (inside(x, y, d)) {
					if ((v=d.get("on_mouse_up")) != null && v.typeDirect() == Value.FUNCTION) {
						FunctionCall.executeFunctionCall(v.function(), new Value[]{ new Value().set(x), new Value().set(y) }, ScriptNode.KEEP_STACK);
						repaint();
					}
					if (!d.get("state").equalsConstant("HOVER")) {
						d.get("state").setConstant("HOVER");
						repaint();
					}
				}
				else if (!d.get("state").equalsConstant("IDLE")) {
					d.get("state").setConstant("IDLE");
					repaint();
				}
			}
		}
		else if ((v=d.get("on_mouse_up")) != null && v.typeDirect() == Value.FUNCTION && inside(x, y, d)) {
			FunctionCall.executeFunctionCall(v.function(), new Value[]{ new Value().set(x), new Value().set(y) }, ScriptNode.KEEP_STACK);
		}
		// hand the event on to all sub-components
		v = d.get("component");
		if (v != null) {
			if (v.type() == Value.ARRAY) {
				final Value[] comp = v.array();
				final int ccount = comp.length;
				for (int i = 0; i < ccount; i++)
					if (comp[i].type() == Value.STRUCTURE && eventMouseReleased(x, y, comp[i].structure(), d))
						return true;
			}
			else if (v.type() == Value.STRUCTURE) {
				if (eventMouseReleased(x, y, v.structure(), d))
					return true;
			}
		}
		// clean up
		ScriptNode.removeStackItem(d);
		return false;
	}


	public int getDrawOffsetX ()  { return dx; }


	public int getDrawOffsetY ()  { return dy; }


// private methods **************************************************************************************************************************************


	/** returns true iff the point (x,y) is inside the view object */
	private boolean inside (final int x, final int y, final Structure d)  {
		// (x,y) can only be inside the image if both x and y are >= 0
		if (x >= 0 && y >= 0) {
			Value v;
			// if the view object has a shape, use that to determine whether the mouse is inside its area. pixels which are part of the button's active surface are white in the shape image
			if ((v=d.get("shape")) != null && !v.equalsConstant("UNDEFINED")) {
				BufferedImage i = (BufferedImage) getImage(v.toString(), true);
				if (i != null)
					return x < i.getWidth() && y < i.getHeight() && i.getRGB(x,y) == 0xffffff;
			}
			// otherwise determine the width and height of the view element and just check whether (x,y) is inside the resulting rectangle
			return x < width(d) && y < height(d);
		}
		return false;
	}


	private int x (Object visible_object, final Structure parent)  {
		int ret = 0;
		// if it's the top level view (parent==null) it automatically sits at (0,0)
		if (parent != null &&( visible_object instanceof Structure ||( visible_object instanceof Value && ((Value)visible_object).type() == Value.STRUCTURE ))) {
			if (visible_object instanceof Value)
				visible_object = ((Value)visible_object).structure();
			final Value v = ((Structure)visible_object).get("x");
			final Value a = ((Structure)visible_object).get("h_align");
			if (v != null) {
				if (v.type() == Value.INTEGER)
					ret = v.integer();
				else if (v.type() == Value.REAL) {
					if (a == null || a.type() != Value.REAL)
						ret = (int) (v.real() + 0.5);
					else
						ret = (int) (v.real() + a.real() + 0.5); // to avoid rounding errors adding up
				}
				else if (v.type() == Value.CONSTANT) {
					if (v.equalsConstant("RIGHT")) {
						if (a == null || a.equalsConstant("UNDEFINED")) // if the visible_object has no explicit horizontal alignment, treat it as LEFT aligned, i.e. sitting left of (x,y)
							ret = width(parent)-width(visible_object);
						else
							ret = width(parent);
					}
					else if (v.equalsConstant("CENTER")) {
						if (a == null || a.equalsConstant("UNDEFINED")) // if the visible_object has no explicit horizontal alignment, treat it as CENTER aligned, i.e. sitting centered on (x,y)
							ret = (width(parent)-width(visible_object))/2;
						else
							ret = width(parent)/2;
					}
//					else if (v.equalsConstant("LEFT"))  // this case has no effect as x is already assumed to be 0
//						ret = 0;
				}
			}
			if (a != null) {
				final int atype = a.type();
				if (atype == Value.INTEGER)
					ret += a.integer();
				else if (atype == Value.REAL && v.type() != Value.REAL)
					ret += (int) (a.real()+0.5);
				else if (atype == Value.CONSTANT) {
					if (a.equalsConstant("CENTER"))
						ret -= width(visible_object)/2;
					else if (a.equalsConstant("LEFT"))
						ret -= width(visible_object);
//					else if (a.equalsConstant("RIGHT"))  // this has no effect apart from keeping x=CENTER and x=RIGHT from assuming a h_align value that differs from RIGHT
//						ret -= 0;
				}
			}
		}
		return ret;
	}


	private int y (Object visible_object, final Structure parent)  {
		int ret = 0;
		// if it's the top level view (parent==null) it automatically sits at (0,0)
		if (parent != null &&( visible_object instanceof Structure ||( visible_object instanceof Value && ((Value)visible_object).type() == Value.STRUCTURE ))) {
			if (visible_object instanceof Value)
				visible_object = ((Value)visible_object).structure();
			final Value v = ((Structure)visible_object).get("y");
			final Value a = ((Structure)visible_object).get("v_align");
			if (v != null) {
				if (v.type() == Value.INTEGER)
					ret = v.integer();
				else if (v.type() == Value.REAL) {
					if (a == null || a.type() != Value.REAL)
						ret = (int) (v.real() + 0.5);
					else
						ret = (int) (v.real() + a.real() + 0.5); // to avoid rounding errors adding up
				}
				else if (v.type() == Value.CONSTANT) {
					if (v.equalsConstant("BOTTOM")) {
						if (a == null || a.equalsConstant("UNDEFINED")) // if the visible_object has no explicit vertical alignment, treat it as TOP aligned, i.e. sitting above (x,y)
							ret = height(parent)-height(visible_object);
						else
							ret = height(parent);
					}
					else if (v.equalsConstant("CENTER")) {
						if (a == null || a.equalsConstant("UNDEFINED")) // if the visible_object has no explicit vertical alignment, treat it as CENTER aligned, i.e. sitting centered on (x,y)
							ret = (height(parent)-height(visible_object))/2;
						else
							ret = height(parent)/2;
					}
//					else if (v.equalsConstant("TOP"))  // this case has no effect as y is already assumed to be 0
//						ret = 0;
				}
			}
			if (a != null) {
				final int atype = a.type();
				if (atype == Value.INTEGER)
					ret += a.integer();
				else if (atype == Value.REAL && v.type() != Value.REAL)
					ret += (int) (a.real()+0.5);
				else if (atype == Value.CONSTANT) {
					if (a.equalsConstant("CENTER"))
						ret -= height(visible_object)/2;
					else if (a.equalsConstant("TOP"))
						ret -= height(visible_object);
//					else if (a.equalsConstant("BOTTOM"))  // this has no effect apart from keeping y=CENTER and y=BOTTOM from assuming a v_align value that differs from BOTTOM
//						ret -= 0;
				}
			}
		}
		return ret;
	}
}
