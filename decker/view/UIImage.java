package decker.view;
import decker.model.*;
import java.awt.*;


class UIImage extends DisplayedComponent
{
	private Image image;




	UIImage (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		fetchImage();
		if (_component.type() == Value.STRUCTURE)
			_component.structure().addValueListener(this);
		update(0, current_clip_source);
	}




	void destroy () {
		super.destroy();
		if (component.type() == Value.STRUCTURE)
			component.structure().removeValueListener(this);
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		if (image != null) {
			if (!width_already_determined)
				w = image.getWidth(null);
			if (!height_already_determined)
				h = image.getHeight(null);
		}
	}




	public void draw (final Graphics g) {
		if (image != null)
			g.drawImage(image, x, y, null);
	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
		if (variable_name.equals("image") || variable_name.equals("angle")) {
			Value v;
			int k;
			final int old_width = w, old_height = h;
			final boolean old_rtpw = relative_to_parent_width, old_rtph = relative_to_parent_height;
			final Image old_image = image;
			fetchImage();
			if (w != old_width || h != old_height || old_rtpw != relative_to_parent_width || old_rtph != relative_to_parent_height) {
				eventSizeChanged(getCurrentClipSource(), old_width, old_height, old_rtpw, old_rtph);
			}
		}
		else {
			super.eventValueChanged(variable_name, container, old_value, new_value);
		}
	}




	private void fetchImage () {
		// fetch the image
		Value v;
		String image_name;
		int angle = 0;
		if (component.type() != Value.STRUCTURE || component.get("image") == null) {
			image_name = component.toString();
		}
		else {
			image_name = component.get("image").toString();
			angle = ((v=component.get("angle")) != null &&( v.type() == Value.INTEGER || v.type() == Value.REAL )) ? (int) v.real() : 0;
		}
		image = (angle!=0) ? AbstractView.getTurnedImage(image_name, angle) : AbstractView.getImage(image_name);
		// print an error if the image is missing
		if (image == null && !image_name.equals("UNDEFINED")) {
			System.out.println("UIImage : undefined image "+image_name+" ("+((component.type() != Value.STRUCTURE || component.get("image") == null)?component:component.get("image")).typeName()+")");
		}
		// adjust the component bounds
		int k;
		w = (image != null) ? image.getWidth(null) : 0;
		if (component.type() == Value.STRUCTURE &&( (v=component.get("width")) == null || v.equalsConstant("UNDEFINED") )) {
			relative_to_parent_width = (v=component.get("x")) != null &&( v.equalsConstant("CENTER") || v.equalsConstant("RIGHT") ||( (k=getPercentageValue(v.toString())) != Integer.MIN_VALUE && k != 0 ));
		}
		h = (image != null) ? image.getHeight(null) : 0;
		if (component.type() == Value.STRUCTURE &&( (v=component.get("height")) == null || v.equalsConstant("UNDEFINED") )) {
			relative_to_parent_height = (v=component.get("y")) != null &&( v.equalsConstant("CENTER") || v.equalsConstant("BOTTOM") ||( (k=getPercentageValue(v.toString())) != Integer.MIN_VALUE && k != 0 ));
		}
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		if (component.type() == Value.STRUCTURE)
			super.update(customSettings|CUSTOM_SIZE, current_clip_source);
		else {
			// it's just a name
			x = parent.x;
			y = parent.y;
			image = AbstractView.getImage(component.toString());
			if (image == null) {
				w = 0;
				h = 0;
			}
			else {
				w = image.getWidth(null);
				h = image.getHeight(null);
			}
		}
	}
}
