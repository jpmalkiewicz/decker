package decker.view;
import decker.model.*;
import java.awt.*;




class UIScrollpane extends DisplayedComponent
{
	private UIClip content_clip;
	private DisplayedComponent content_parent;
	private DisplayedComponent content;
	private UIScrollpaneScrollbar vertical_scrollbar, horizontal_scrollbar;
	private DisplayedComponent corner_filler;
	private final Value v_displayed_element = new Value();
	private final Value v_vertical_scrollbar = new Value();
	private final Value v_horizontal_scrollbar = new Value();



	UIScrollpane (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		content_clip = new UIClip(null, this, current_clip_source);
		content_parent = new DisplayedComponent(null, content_clip, current_clip_source);
		content_parent.x = Integer.MIN_VALUE;
		content_parent.y = Integer.MIN_VALUE;
		if (_component != null && _component.type() == Value.STRUCTURE)
			_component.structure().addValueListener(this);
		update(0, current_clip_source);
	}




	void destroy () {
		super.destroy();
		if (content_parent != null) {
			content_parent.destroy();
			content_parent = null;
		}
		if (content_clip != null) {
			content_clip.destroy();
			content_clip = null;
		}
		if (content != null) {
			content.destroy();
			content = null;
		}
		if (vertical_scrollbar != null) {
			vertical_scrollbar.destroy();
			vertical_scrollbar = null;
		}
		if (horizontal_scrollbar != null) {
			horizontal_scrollbar.destroy();
			horizontal_scrollbar = null;
		}
		if (component != null && component.type() == Value.STRUCTURE)
			component.structure().removeValueListener(this);
	}




	void draw (final Graphics g) {
		if (content != null) {
			final Shape old_clip = g.getClip();
			g.setClip(content_clip.cx, content_clip.cy, content_clip.cw, content_clip.ch);
			content.draw(g);
			g.setClip(old_clip);
		}
		if (vertical_scrollbar != null)
			vertical_scrollbar.draw(g);
		if (horizontal_scrollbar != null)
			horizontal_scrollbar.draw(g);
	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	void sliderMoved (final UIScrollpaneScrollbar source, final int new_position) {
		if (content != null) {
			if (content_parent.y > Integer.MIN_VALUE) {
				if (source == vertical_scrollbar)
					content_parent.y = y - new_position;
				else
					content_parent.x = x - new_position;
			}
			content.update(0, content_clip);
		}
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		if (component.type() != Value.STRUCTURE || !component.get("structure_type").equals("SCROLLPANE"))
			return;
		final Structure d = component.structure();

		super.update(customSettings, current_clip_source);
		// add the displayed content
		Value v;
		if (!(v=d.get("displayed_element")).equals(v_displayed_element)) {
			v_displayed_element.set(v);
			if (content != null)
				content.destroy();
			if (v.equalsConstant("UNDEFINED"))
				content = null;
			else
				content = createDisplayedComponent(v, content_parent, content_clip);
		}
		// update the scrollbars
		final boolean optional_scrollbars = d.get("optional_scrollbars").equals(true);
		// check whether we need a vertical scrollbar
		boolean needs_vertical = (v=d.get("vertical_scrollbar")).type() == Value.STRUCTURE && v.get("structure_type").equals("SCROLLBAR") &&( !optional_scrollbars ||( content != null && content.h > h ));
		if (needs_vertical) {
			v = d.get("vertical_scrollbar");
			// give the scrollbar values which make sense
			v.get("vertical").set(true);
			if (v.get("x").equalsConstant("UNDEFINED"))
				v.get("x").set("RIGHT");
			// make a new vertical scrollbar if neccesary
			if (vertical_scrollbar == null || !v.equals(v_vertical_scrollbar)) {
				if (vertical_scrollbar != null)
					vertical_scrollbar.destroy();
				vertical_scrollbar = new UIScrollpaneScrollbar(v, this, current_clip_source);
				v_vertical_scrollbar.set(v);
			}
		}
		// check whether we need a horizontal scrollbar
		boolean needs_horizontal = (v=d.get("horizontal_scrollbar")).type() == Value.STRUCTURE && v.get("structure_type").equals("SCROLLBAR") &&( !optional_scrollbars ||( content != null && content.w > w-((vertical_scrollbar==null)?0:vertical_scrollbar.w) ));
		if (needs_horizontal) {
			v = d.get("horizontal_scrollbar");
			// give the scrollbar values which make sense
			v.get("vertical").set(false);
			if (v.get("y").equalsConstant("UNDEFINED"))
				v.get("y").set("BOTTOM");
			// make a new horizontal scrollbar if neccesary
			if (horizontal_scrollbar == null || !v.equals(v_horizontal_scrollbar)) {
				if (horizontal_scrollbar != null)
					horizontal_scrollbar.destroy();
				horizontal_scrollbar = new UIScrollpaneScrollbar(v, this, current_clip_source);
				v_horizontal_scrollbar.set(v);
			}
			// check whether we need a horizontal scrollbar now that we have a horizontal scrollbar
			if (!needs_vertical) {
				needs_vertical = (v=d.get("vertical_scrollbar")).type() == Value.STRUCTURE && v.get("structure_type").equals("SCROLLBAR") &&( !optional_scrollbars ||( content != null && content.h > h-horizontal_scrollbar.h ));
				if (needs_vertical) {
					v = d.get("vertical_scrollbar");
					// give the scrollbar values which make sense
					v.get("vertical").set(true);
					if (v.get("x").equalsConstant("UNDEFINED"))
						v.get("x").set("RIGHT");
					// make a new vertical scrollbar if neccesary
					if (vertical_scrollbar == null || !v.equals(v_vertical_scrollbar)) {
						if (vertical_scrollbar != null)
							vertical_scrollbar.destroy();
						vertical_scrollbar = new UIScrollpaneScrollbar(v, this, current_clip_source);
						v_vertical_scrollbar.set(v);
					}
				}
			}
		}
		else if (horizontal_scrollbar != null) {
			horizontal_scrollbar.destroy();
			horizontal_scrollbar = null;
		}
		// remove the vertical scrollbar, if it is no longer used
		if (!needs_vertical && vertical_scrollbar != null) {
			vertical_scrollbar.destroy();
			vertical_scrollbar = null;
		}
		// don't bother with the content clip and parent unless we actually have some kind of content
		if (content != null) {
			// adjust the scrollpane clip
			content_clip.x = x;
			content_clip.y = y;
			content_clip.w = w-(needs_vertical?vertical_scrollbar.w:0);
			content_clip.h = h-(needs_horizontal?horizontal_scrollbar.h:0);
			content_clip.determineClip(current_clip_source);
			// adjust the scrollbar sizes
			if (needs_horizontal)
				v_horizontal_scrollbar.get("width").set(content_clip.w);
			if (needs_vertical)
				v_vertical_scrollbar.get("height").set(content_clip.h);
			// make sure the scrollbars have valid values
System.out.println("UIScrollpane : "+content_parent.x+" "+content_parent.y);
			int dx = (content_parent.x>Integer.MIN_VALUE) ? (x - content_parent.x) : (needs_horizontal?horizontal_scrollbar.getSliderPosition():0);
			if (!needs_horizontal)
				dx = 0;
			else {
				if (dx + content_clip.w > content.w)
					dx = content_clip.w - content.w;
				v_horizontal_scrollbar.get("slider_max").set(content.w - content_clip.w);
				v_horizontal_scrollbar.get("slider_position").set(dx);
			}
System.out.println("UIScrollpane : "+dx);
			int dy = (content_parent.y>Integer.MIN_VALUE) ? (y - content_parent.y) : (needs_vertical?vertical_scrollbar.getSliderPosition():0);
System.out.println("UIScrollpane : "+dy+"   "+(content_parent.y>Integer.MIN_VALUE));
			if (!needs_vertical)
				dy = 0;
			else {
				if (dy + content_clip.h > content.h)
					dy = content_clip.h - content.h;
System.out.println("UIScrollpane : "+dy+"   "+(content_parent.y>Integer.MIN_VALUE));
				v_vertical_scrollbar.get("slider_max").set(content.h - content_clip.h);
				v_vertical_scrollbar.get("slider_position").set(dy);
			}
System.out.println("UIScrollpane : "+dy);
			if (needs_horizontal)
				horizontal_scrollbar.update(0, current_clip_source);
			if (needs_vertical)
				vertical_scrollbar.update(0, current_clip_source);
			// adjust the content parent
			content_parent.x = x-dx;
			content_parent.y = y-dy;
			content_parent.w = w;
			content_parent.h = h;
			// adjust the content
			content.update(0, content_clip);
		}
	}
}