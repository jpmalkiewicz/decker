package decker.view;
import decker.model.*;
import java.awt.image.BufferedImage;
import java.awt.*;


public class DisplayedComponent
{
	// the displayed component
	Value component;
	// the parent Structure of this component
	private DisplayedComponent parent;
	// the bounding rectangle
	int x, y, w, h;
	// the clipped bounding rectangle. if the component is invisible w is <= 0, h possibly too
	private int cx, cy, cw, ch;
	// true if the last mouse event was inside the component
	private boolean last_mouse_event_inside;
	// true if the component itself has changed in any way
	private boolean has_changed;

	// if the component is an image, this is it
	private Image image;

	// the shape of the component, if it's a STRUCTURE
	private BufferedImage shape;
	// child structures of this component
	DisplayedComponent[] child;
	int child_count;
	// the event functions, if they exist
	private Function keyDown;
	private Function mouseDragged;
	private Function mouseEntered;
	private Function mouseExited;
	private Function mouseMoved;
	private Function mouseDown;
	private Function mouseUp;



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
/*			else if (type.equals("BORDER")) {
				final boolean inverted = d.get("inverted").equals(true);
				Value vtc = d.get("top_color"), vlc = d.get("left_color"), vrc = d.get("right_color"), vbc = d.get("bottom_color");
				if (vtc.equalsConstant("UNDEFINED"))
					vtc = vlc;
				else if (vlc.equalsConstant("UNDEFINED"))
					vlc = vtc;
				if (vrc.equalsConstant("UNDEFINED")) {
					if (!vbc.equalsConstant("UNDEFINED"))
						vrc = vbc;
					else
						vrc = vlc;
				}
				if (vbc.equalsConstant("UNDEFINED"))
					vbc = vrc;
				// determine the line thickness
				int thickness = 2;
				if ((v=d.get("thickness")).type() == Value.INTEGER)
					thickness = v.integer();
				else if ((v=ScriptNode.getValue("DEFAULT_BORDER_THICKNESS")).type() == Value.INTEGER)
					thickness = v.integer();

				// if we have enough colors, draw the border
				if (!vlc.equalsConstant("UNDEFINED")) {
					final int w1 = w-1;
					final int h1 = h-1;
					if (!inverted) {
						// draw the top border
						g.setColor(AbstractView.getColor(vtc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+i, y+i, x+w1-i, y+i);
						// draw the left border
						g.setColor(AbstractView.getColor(vlc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+i, y+i+1, x+i, y+h1-i);
						// draw the right border
						g.setColor(AbstractView.getColor(vrc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+w1-i, y+i+1, x+w1-i, y+h1-i);
						// draw the bottom border
						g.setColor(AbstractView.getColor(vbc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+i+1, y+h1-i, x+w1-i-1, y+h1-i);
					}
					else {
						// draw the bottom border
						g.setColor(AbstractView.getColor(vtc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+i, y+h1-i, x+w1-i, y+h1-i);
						// draw the right border
						g.setColor(AbstractView.getColor(vlc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+w1-i, y+i, x+w1-i, y+h1-i-1);
						// draw the left border
						g.setColor(AbstractView.getColor(vrc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+i, y+i+1, x+i, y+h1-i-1);
						// draw the top border
						g.setColor(AbstractView.getColor(vbc.toString()));
						for (int i = thickness; --i >= 0; )
							g.drawLine(x+i, y+i, x+w1-i-1, y+i);
					}
				}
				// draw the background
				if (!(v=d.get("background_color")).equalsConstant("UNDEFINED")) {
					g.setColor(AbstractView.getColor(v.toString()));
					g.fillRect(x+thickness, y+thickness, w-2*thickness, h-2*thickness);
				}
			}
*/			else if (type.equals("LINE") && d.get("x2").type() == Value.INTEGER && d.get("y2").type() == Value.INTEGER) {
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


	private void update (final DisplayedComponent current_clip_source) {
		has_changed = false;

		// if the displayed component is an image, fetch it
		if (component.type() != Value.STRUCTURE) {
			x = parent.x;
			y = parent.y;
			image = AbstractView.getImage(component.toString());
		}
		// if the displayed component is a Structure, set the structure specific variables
		else { // (component.type() == Value.STRUCTURE)
image = null;
			final Structure s = component.structure();
			// determine the bounding rectangle
			x = parent.x + DefaultView.x(s, parent.w);
			y = parent.y + DefaultView.y(s, parent.h);
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
			}
			// the component may have a shape
			Value v;
			if ((v=s.get("shape")) != null && v.type() == Value.STRING) {
				shape = (BufferedImage) AbstractView.getImage(v.string(), true);
			}
			// the event handler functions
			if ((v=s.get("on_key_down")) != null && v.type() == Value.FUNCTION) {
				if (keyDown == null) {
					DisplayedScreen.addKeyDownListener(this);
				}
				keyDown = v.function();
			}
			else if (keyDown != null) {
				DisplayedScreen.removeKeyDownListener(this);
				keyDown = null;
			}
			if ((v=s.get("on_mouse_down")) != null && v.type() == Value.FUNCTION) {
				if (mouseDown == null) {
					DisplayedScreen.addMouseDownListener(this);
				}
				mouseDown = v.function();
			}
			else if (mouseDown != null) {
				DisplayedScreen.removeMouseDownListener(this);
				mouseDown = null;
			}
			if ((v=s.get("on_mouse_dragged")) != null && v.type() == Value.FUNCTION) {
				if (mouseDragged == null) {
					DisplayedScreen.addMouseDraggedListener(this);
				}
				mouseDragged = v.function();
			}
			else if (mouseDragged != null) {
				DisplayedScreen.removeMouseDraggedListener(this);
				mouseDragged = null;
			}
			if ((v=s.get("on_mouse_entered")) != null && v.type() == Value.FUNCTION) {
				if (mouseEntered == null) {
					DisplayedScreen.addMouseEnteredListener(this);
				}
				mouseEntered = v.function();
			}
			else if (mouseEntered != null) {
				DisplayedScreen.removeMouseEnteredListener(this);
				mouseEntered = null;
			}
			if ((v=s.get("on_mouse_exited")) != null && v.type() == Value.FUNCTION) {
				if (mouseExited == null) {
					DisplayedScreen.addMouseExitedListener(this);
				}
				mouseExited = v.function();
			}
			else if (mouseExited != null) {
				DisplayedScreen.removeMouseExitedListener(this);
				mouseExited = null;
			}
			if ((v=s.get("on_mouse_moved")) != null && v.type() == Value.FUNCTION) {
				if (mouseMoved == null) {
					DisplayedScreen.addMouseMovedListener(this);
				}
				mouseMoved = v.function();
			}
			else if (mouseMoved != null) {
				DisplayedScreen.removeMouseMovedListener(this);
				mouseMoved = null;
			}
			if ((v=s.get("on_mouse_up")) != null && v.type() == Value.FUNCTION) {
				if (mouseUp == null) {
					DisplayedScreen.addMouseUpListener(this);
				}
				mouseUp = v.function();
			}
			else if (mouseUp != null) {
				DisplayedScreen.removeMouseUpListener(this);
				mouseUp = null;
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