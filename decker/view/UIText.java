package decker.view;
import decker.model.*;
import java.awt.*;



/** displays a TEXT structure */
final class UIText extends DisplayedComponent
{
	private String text;
	private Color color;
	private Font font;
	private int y_offset;




	UIText (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		component.structure().addValueListener(this);
		updateText();
		super.update(CUSTOM_SIZE, current_clip_source);
		child_count = 0; // cannot have children
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		final FontMetrics fm = AbstractView.getFontMetrics(font);
		if (!width_already_determined)
			w = fm.stringWidth(text);
		if (!height_already_determined)
			h = fm.getHeight();
	}




	void draw (final Graphics g) {
		if (color != null)
			g.setColor(color);
		g.setFont(font);
		g.drawString(text, x, y+y_offset);
	}




	public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {
System.out.println(index+"  changed");
		updateText();
		super.eventValueChanged(index, wrapper, old_value, new_value);
	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
System.out.println(variable_name+"  changed");
		updateText();
		super.eventValueChanged(variable_name, container, old_value, new_value);
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		super.update(customSettings|CUSTOM_SIZE, current_clip_source);
		updateText();
	}




	private void updateText () {
		final Structure t = component.structure();
		Value v;
		// fetch the text and its style settings
final String old_text = text;
		text = t.get("text").toString();
System.out.println("updating text    "+old_text+" -> "+text);
		v = t.get("font");
		font = AbstractView.getFont((v.type() == Value.STRING)?v.string():"", null, false);
		color = ((v=t.get("color")).type() == Value.STRING) ? AbstractView.getColor(v.string()) : null;
		y_offset = AbstractView.getFontMetrics(font).getAscent();
	}
}