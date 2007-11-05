package decker.view;
import decker.model.*;
import java.awt.*;


class UIImage extends DisplayedComponent
{
	private Image image;


	UIImage (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(null, _parent, current_clip_source);
		image = AbstractView.getImage(_component.toString());
		x = _parent.x;
		y = _parent.y;
		w = (image==null) ? 0 : image.getWidth(null);
		h = (image==null) ? 0 : image.getWidth(null);
	}


	public void draw (final Graphics g) {
		if (image != null)
			g.drawImage(image, x, y, null);
	}
}