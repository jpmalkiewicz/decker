package decker.view;
import decker.model.*;
import java.awt.*;


final class UIBorder extends DisplayedComponent
{
	public static boolean PRINT_ILLEGAL_BORDER_SIZE_WARNING = false;

	private Color left, top, right, bottom, background;
	int thickness;
	boolean inverted;
	private UIInnerArea inner_area;




	UIBorder (final DisplayedComponent _parent, final DisplayedComponent current_clip_source, final boolean use_default_background_color) {
		super(null, _parent, current_clip_source);
		left = AbstractView.getColor(ScriptNode.getVariable("BORDER_COLOR1").string());
		top = left;
		right = AbstractView.getColor(ScriptNode.getVariable("BORDER_COLOR2").string());
		bottom = right;
		inverted = false;
		thickness = 2;
		Value v;
		if ((v=ScriptNode.getVariable("DEFAULT_BORDER_THICKNESS")).type() == Value.INTEGER)
			thickness = v.integer();
		if (use_default_background_color)
			background = AbstractView.getColor(ScriptNode.getVariable("BACKGROUND_COLOR").string());
		child_count = 0;
		inner_area = new UIInnerArea(this);
		inner_area.update(0, current_clip_source);
	}




	UIBorder (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		if (_component != null && _component.type() == Value.STRUCTURE){
			_component.structure().addValueListener(this);
		}
		updateBorder();
		child_count = 0;
		inner_area = new UIInnerArea(this);
		update(0, current_clip_source);
	}




	void applyInnerBounds (UIInnerArea _inner_area) {
		_inner_area.x = x + thickness;
		_inner_area.y = y + thickness;
		_inner_area.w = w - 2*thickness;
		_inner_area.h = h - 2*thickness;
		// no need to set the clip of the _inner_area, since UIInnerArea never listens to input events anyway
		// this would be a lazy implementation that doesn't take the border into account
//		_inner_area.cx = cx;
//		_inner_area.cy = cy;
//		_inner_area.cw = cw;
//		_inner_area.ch = ch;
	}




	void draw (final Graphics g) {
		final int t = thickness;
		// don't draw the border if it has a negative size
		if (w < 2*t || h < 2*t) {
			if (PRINT_ILLEGAL_BORDER_SIZE_WARNING) {
				PRINT_ILLEGAL_BORDER_SIZE_WARNING = false; // print the warning only once
				new RuntimeException("Warning in UIBorder.draw() : negative border size. x="+x+" y="+y+" w="+w+" h="+h).printStackTrace();
			}
			return;
		}
		// draw the background
		if (background != null && w-2*t > 0 && h-2*t > 0) {
			g.setColor(background);
			g.fillRect(x+t, y+t, w-2*t, h-2*t);
		}
		// draw the border
		final int w1 = w-1, h1 = h-1;
		if (!inverted) {
			// draw the top border
			g.setColor(top);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i, x+w1-i, y+i);
			// draw the left border
			g.setColor(left);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i+1, x+i, y+h1-i);
			// draw the right border
			g.setColor(right);
			for (int i = t; --i >= 0; )
				g.drawLine(x+w1-i, y+i+1, x+w1-i, y+h1-i);
			// draw the bottom border
			g.setColor(bottom);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i+1, y+h1-i, x+w1-i-1, y+h1-i);
		}
		else {
			// draw the bottom border
			g.setColor(top);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+h1-i, x+w1-i, y+h1-i);
			// draw the right border
			g.setColor(left);
			for (int i = t; --i >= 0; )
				g.drawLine(x+w1-i, y+i, x+w1-i, y+h1-i-1);
			// draw the left border
			g.setColor(right);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i+1, x+i, y+h1-i-1);
			// draw the top border
			g.setColor(bottom);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i, x+w1-i-1, y+i);
		}
		inner_area.draw(g);
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		super.update(customSettings, current_clip_source);
		updateBorder();
		if (inner_area != null);
		inner_area.update(0, current_clip_source);
	}




	private void updateBorder () {
		if (component != null && component.type() == Value.STRUCTURE && component.get("structure_type").equals("BORDER")) {
			Value v;
			inverted = component.get("inverted").equals(true);
			background = AbstractView.getColor(component.get("background_color").toString());
			Value vtc = component.get("top_color"), vlc = component.get("left_color"), vrc = component.get("right_color"), vbc = component.get("bottom_color");
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
			left = AbstractView.getColor(vlc.toString());
			top = AbstractView.getColor(vtc.toString());
			right = AbstractView.getColor(vrc.toString());
			bottom = AbstractView.getColor(vbc.toString());
			// determine the line thickness
			thickness = 2;
			if ((v=component.get("thickness")).type() == Value.INTEGER)
				thickness = v.integer();
			else if ((v=ScriptNode.getVariable("DEFAULT_BORDER_THICKNESS")).type() == Value.INTEGER)
				thickness = v.integer();
		}
	}




	void updateChildren (final DisplayedComponent current_clip_source) {
		inner_area.updateChildren(current_clip_source);
	}
}