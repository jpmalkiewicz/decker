package decker.view;
import decker.model.*;
import java.awt.*;
import java.awt.event.*;




final class UITextField extends DisplayedComponent
{
	private String text;
	private Color color;
	private Font font;
	private FontMetrics font_metrics;
	private int y_offset;
	private int char_limit;
	private int cursor_x;
	private Object cursor;



	UITextField (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		updateText();
		update(current_clip_source);
		child_count = 0; // cannot have children
		// register it as a hard coded key listener
		hasHardcodedEventFunction[ON_KEY_DOWN] = true;
		if (!eventFunctionRegistered[ON_KEY_DOWN])
			addEventListener(ON_KEY_DOWN);
	}




	void draw (final Graphics g) {
		if (color != null)
			g.setColor(color);
		g.setFont(font);
		g.drawString(text, x, y+y_offset);
		if (cursor instanceof DisplayedComponent) {
			((DisplayedComponent)cursor).draw(g);
		}
		else {
			g.drawString((String)cursor, x+cursor_x, y+y_offset);
		}
	}




	boolean eventUserInput (final int event_id, final AWTEvent e, final int mouse_x, final int mouse_y, final int mouse_dx, final int mouse_dy) {
		if (event_id == ON_KEY_DOWN) {
			final KeyEvent k = (KeyEvent) e;
			final int old_cursor_x = cursor_x;
			if (k.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
				if (text.length() > 0) {
					text = text.substring(0, text.length()-1);
					cursor_x = font_metrics.stringWidth(text);
				}
			}
			else if (k.getKeyChar() != KeyEvent.CHAR_UNDEFINED && text.length() < char_limit) {
				// also make sure that the text fits into the field
				Value v = component.get("width");
				if ( v.type() == Value.INTEGER && v.integer() < font_metrics.stringWidth(text+k.getKeyChar()) + ((cursor instanceof String)?font_metrics.stringWidth((String)cursor):((DisplayedComponent)cursor).w) ) {
					return true;
				}
				text = text + k.getKeyChar();
				cursor_x = font_metrics.stringWidth(text);
			}
			if (old_cursor_x != cursor_x && cursor instanceof DisplayedComponent) {
				((DisplayedComponent)cursor).x = cursor_x + x;
			}
		}
		return true;
	}




	void update (final DisplayedComponent current_clip_source) {
		super.update(current_clip_source);
		updateText();
		updateCursor(current_clip_source);
	}




	private void updateCursor (final DisplayedComponent current_clip_source) {
		final Value v = component.get("cursor");
		if (v.type() == Value.STRUCTURE) {
			if (!(cursor instanceof DisplayedComponent) || !((DisplayedComponent)cursor).component.equals(v)) {
				cursor = createDisplayedComponent(v, this, current_clip_source);
			}
			((DisplayedComponent)cursor).x = x + cursor_x;
		}
		else {
			cursor = v.toString();
		}
	}




	private void updateText () {
		final Structure t = component.structure();
		Value v;
		// fetch the text and its style settings
		text = t.get("text").toString();
		v = t.get("font");
		font = AbstractView.getFont((v.type() == Value.STRING)?v.string():"", null, false);
		color = ((v=t.get("color")).type() == Value.STRING) ? AbstractView.getColor(v.string()) : null;
		y_offset = AbstractView.getFontMetrics(font).getAscent();
		char_limit = ((v=t.get("char_limit")).type() == Value.INTEGER) ? v.integer() : Integer.MAX_VALUE;
		font_metrics = AbstractView.getFontMetrics(font);
		cursor_x = font_metrics.stringWidth(text);
	}
}