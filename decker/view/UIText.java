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
		updateText();
		update(current_clip_source);
		child_count = 0; // cannot have children
	}




	void draw (final Graphics g) {
		if (color != null)
			g.setColor(color);
		g.setFont(font);
		g.drawString(text, x, y+y_offset);
	}




	void update (final DisplayedComponent current_clip_source) {
		super.update(current_clip_source);
		updateText();
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
	}
}