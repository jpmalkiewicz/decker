package decker.view;
import decker.model.*;
import java.awt.*;


/** contains all interface related functions for the table object */
class UITable
{
	private final static int ILEFT = Integer.MIN_VALUE, IRIGHT = Integer.MIN_VALUE+1, ICENTER = Integer.MIN_VALUE+2, ITOP = Integer.MIN_VALUE+3, IBOTTOM = Integer.MIN_VALUE+4;


	static void draw (final int dx, final int dy, final Structure d, final Graphics g, final DefaultView caller) {
		Value q;
		final int rows = d.get("rows").integer();
		final int columns = d.get("columns").integer();
		final int ch = d.get("cell_height").integer();
		// calculate the x and width of each column
		final int[] cx = new int[columns];
		final int[] cw = new int[columns];
		if ((q=d.get("cell_width")).type() == Value.INTEGER) {
			final int w = q.integer();
			for (int i = 0; i < columns; i++) {
				cx[i] = i*w;
				cw[i] = w;
			}
		}
		else { // q must be an ARRAY
			final Value[] a = q.array();
			int wsum = 0;
			for (int i = 0; i < columns; i++) {
				cx[i] = wsum;
				final int w = a[i].integer();
				cw[i] = w;
				wsum += w;
			}
		}
/*		// calculate the v_align for each column
		final int[] v_align = new int[columns];
		if ((q=d.get("cell_v_align")) == null) {
			for (int i = columns; --i >= 0; )
				v_align[i] = ICENTER;
		}
		else if (q.type() == Value.ARRAY) {
			final Value[] a = q.array();
			for (int i = columns; --i >= 0; ) {
				if (a[i].type() == Value.INTEGER)
					v_align[i] = a[i].integer();
				else {
					final String s = a[i].toString();
					if (s.equals("LEFT"))
						v_align[i] = ILEFT;
					else if (s.equals("RIGHT"))
						v_align[i] = IRIGHT;
					else
						v_align[i] = ICENTER;
				}
			}
		}
		else {
			int va = ICENTER;
			if (q.type() == Value.INTEGER)
				va = q.integer();
			else {
				final String s = q.toString();
				if (s.equals("LEFT"))
					va = ILEFT;
				else if (s.equals("RIGHT"))
					va = IRIGHT;
			}
			for (int i = columns; --i >= 0; )
				v_align[i] = va;
		}
		// calculate the h_align for each column
		final int[] h_align = new int[columns];
		if ((q=d.get("cell_h_align")) == null) {
			for (int i = columns; --i >= 0; )
				h_align[i] = ICENTER;
		}
		else if (q.type() == Value.ARRAY) {
			final Value[] a = q.array();
			for (int i = columns; --i >= 0; ) {
				if (a[i].type() == Value.INTEGER)
					h_align[i] = a[i].integer();
				else {
					final String s = a[i].toString();
					if (s.equals("TOP"))
						h_align[i] = ITOP;
					else if (s.equals("BOTTOm"))
						h_align[i] = IRIGHT;
					else
						h_align[i] = IBOTTOM;
				}
			}
		}
		else {
			int ha = ICENTER;
			if (q.type() == Value.INTEGER)
				ha = q.integer();
			else {
				final String s = q.toString();
				if (s.equals("TOP"))
					ha = ITOP;
				else if (s.equals("BOTTOM"))
					ha = IBOTTOM;
			}
			for (int i = columns; --i >= 0; )
				h_align[i] = ha;
		}
*/		// draw the cells
		final Value[] cells = d.get("cell").array();
		for (int j = rows; --j >= 0; ) {
			final Value[] row = cells[j].array();
			// determine the base y
			final int row_y = j*ch+dy;
			for (int i = columns; --i >= 0; ) {
				caller.drawContent(row[i], dx+cx[i], row_y, cw[i], ch);
			}
		}
	}
}