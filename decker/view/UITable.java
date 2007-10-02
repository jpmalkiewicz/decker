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
		int total_width = 0;
		if ((q=d.get("cell_width")).type() == Value.INTEGER) {
			final int w = q.integer();
			for (int i = 0; i < columns; i++) {
				cx[i] = i*w;
				cw[i] = w;
			}
			total_width = w*columns;
		}
		else { // q must be an ARRAY
			final Value[] a = q.array();
			for (int i = 0; i < columns; i++) {
				cx[i] = total_width;
				final int w = a[i].integer();
				cw[i] = w;
				total_width += w;
			}
		}
		// mark the selected row
		int r;
		if ((q=d.get("selected_row")).type() == Value.INTEGER && ((r=q.integer()) >= 0) && r < rows) {
			q = d.get("selected_row_background");
			boolean its_a_color = false;
			if (q.type() == Value.STRING) {
				Color c = AbstractView.getColor(q.string());
				if (c != null) {
					its_a_color = true;
					g.setColor(c);
					g.fillRect(dx, dy+ch*r, total_width, ch);
				}
			}
		}
		// draw the cells
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


	static boolean eventMouseDragged (int x, int y, final int dx, final int dy, final Structure d, final int parent_width, final int parent_height, final DefaultView caller)  {
		Value q;
		final int rows = d.get("rows").integer();
		final int columns = d.get("columns").integer();
		final int ch = d.get("cell_height").integer();
		// calculate the x and width of each column
		final int[] cx = new int[columns];
		final int[] cw = new int[columns];
		int total_width = 0;
		if ((q=d.get("cell_width")).type() == Value.INTEGER) {
			final int w = q.integer();
			for (int i = 0; i < columns; i++) {
				cx[i] = i*w;
				cw[i] = w;
			}
			total_width = w*columns;
		}
		else { // q must be an ARRAY
			final Value[] a = q.array();
			for (int i = 0; i < columns; i++) {
				cx[i] = total_width;
				final int w = a[i].integer();
				cw[i] = w;
				total_width += w;
			}
		}
		// spread the event
		final Value[] cells = d.get("cell").array();
		for (int j = 0; j < rows; j++) {
			final Value[] row = cells[j].array();
			final int eventy = y-j*ch;
			for (int i = 0; i < columns; i++)
				if (row[i].type() == Value.STRUCTURE)
					if (caller.eventMouseDragged(x-cx[i], eventy, dx, dy, row[i].structure(), cw[i], ch))
						return true;
		}
		// if we're currently dragging a row, check whether it has changed its position
		if ((q=d.get("can_drag_rows")).type() == Value.INTEGER) {
			final int dragged_row_index = q.integer()/ch;
			if (y >= 0 && y/ch < rows && y/ch != dragged_row_index) {
				// the row has been dragged to a new position
				final Value dragged_row = cells[dragged_row_index];
				if (y/ch > dragged_row_index)
					System.arraycopy(cells, dragged_row_index+1, cells, dragged_row_index, y/ch-dragged_row_index);
				else
					System.arraycopy(cells, y/ch, cells, y/ch+1, dragged_row_index-y/ch);
				cells[y/ch] = dragged_row;
				q.set(q.integer()%ch+(y/ch)*ch);
				// call the row drag listener function if there is one
				if ((q=d.get("on_row_drag")) != null && q.typeDirect() == Value.FUNCTION)
					FunctionCall.executeFunctionCall(q.function(), new Value[]{ new Value().set(d), new Value().set(dragged_row_index), new Value().set(y/ch) }, ScriptNode.KEEP_STACK);
			}
		}
		// move the row selection to the current row
		if (!d.get("selected_row_background").equalsConstant("UNDEFINED") && DefaultView.inside(x, y, total_width, ch*rows, d))
			d.get("selected_row").set(y/ch);
		return false;
	}


	static boolean eventMouseMoved (int x, int y, final Structure d, final int parent_width, final int parent_height, final DefaultView caller)  {
		Value q;
		final int rows = d.get("rows").integer();
		final int columns = d.get("columns").integer();
		final int ch = d.get("cell_height").integer();
		// calculate the x and width of each column
		final int[] cx = new int[columns];
		final int[] cw = new int[columns];
		int total_width = 0;
		if ((q=d.get("cell_width")).type() == Value.INTEGER) {
			final int w = q.integer();
			for (int i = 0; i < columns; i++) {
				cx[i] = i*w;
				cw[i] = w;
			}
			total_width = w*columns;
		}
		else { // q must be an ARRAY
			final Value[] a = q.array();
			for (int i = 0; i < columns; i++) {
				cx[i] = total_width;
				final int w = a[i].integer();
				cw[i] = w;
				total_width += w;
			}
		}
		// spread the event
		final Value[] cells = d.get("cell").array();
		for (int j = 0; j < rows; j++) {
			final Value[] row = cells[j].array();
			final int eventy = y-j*ch;
			for (int i = 0; i < columns; i++)
				if (row[i].type() == Value.STRUCTURE)
					if (caller.eventMouseMoved(x-cx[i], eventy, row[i].structure(), cw[i], ch))
						return true;
		}
		return false;
	}


	static boolean eventMousePressed (int x, int y, final Structure d, final int parent_width, final int parent_height, final DefaultView caller)  {
		Value q;
		final int rows = d.get("rows").integer();
		final int columns = d.get("columns").integer();
		final int ch = d.get("cell_height").integer();
		// calculate the x and width of each column
		final int[] cx = new int[columns];
		final int[] cw = new int[columns];
		int total_width = 0;
		if ((q=d.get("cell_width")).type() == Value.INTEGER) {
			final int w = q.integer();
			for (int i = 0; i < columns; i++) {
				cx[i] = i*w;
				cw[i] = w;
			}
			total_width = w*columns;
		}
		else { // q must be an ARRAY
			final Value[] a = q.array();
			for (int i = 0; i < columns; i++) {
				cx[i] = total_width;
				final int w = a[i].integer();
				cw[i] = w;
				total_width += w;
			}
		}
		// change the selected row
		if (!d.get("selected_row_background").equalsConstant("UNDEFINED") && DefaultView.inside(x, y, total_width, ch*rows, d))
			d.get("selected_row").set(y/d.get("cell_height").integer());
		// spread the event
		final Value[] cells = d.get("cell").array();
		for (int j = 0; j < rows; j++) {
			final Value[] row = cells[j].array();
			final int eventy = y-j*ch;
			for (int i = 0; i < columns; i++)
				if (row[i].type() == Value.STRUCTURE)
					if (caller.eventMousePressed(x-cx[i], eventy, row[i].structure(), cw[i], ch))
						return true;
		}
		// if it is possible to drag rows and the event hasn't been consumed, remember the place where the row was grabbed
		if (( (q=d.get("can_drag_rows")).equals(true) || q.type() == Value.INTEGER )&& DefaultView.inside(x, y, total_width, ch*rows, d))
			q.set(y);
		return false;
	}


	static boolean eventMouseReleased (int x, int y, final Structure d, final int parent_width, final int parent_height, final DefaultView caller)  {
		Value q;
		final int rows = d.get("rows").integer();
		final int columns = d.get("columns").integer();
		final int ch = d.get("cell_height").integer();
		// calculate the x and width of each column
		final int[] cx = new int[columns];
		final int[] cw = new int[columns];
		int total_width = 0;
		if ((q=d.get("cell_width")).type() == Value.INTEGER) {
			final int w = q.integer();
			for (int i = 0; i < columns; i++) {
				cx[i] = i*w;
				cw[i] = w;
			}
			total_width = w*columns;
		}
		else { // q must be an ARRAY
			final Value[] a = q.array();
			for (int i = 0; i < columns; i++) {
				cx[i] = total_width;
				final int w = a[i].integer();
				cw[i] = w;
				total_width += w;
			}
		}
		// stop the row drag mechanism
		if ((q=d.get("can_drag_rows")).type() == Value.INTEGER) {
			q.set(true);
		}
		// spread the event
		final Value[] cells = d.get("cell").array();
		for (int j = 0; j < rows; j++) {
			final Value[] row = cells[j].array();
			final int eventy = y-j*ch;
			for (int i = 0; i < columns; i++)
				if (row[i].type() == Value.STRUCTURE)
					if (caller.eventMouseReleased(x-cx[i], eventy, row[i].structure(), cw[i], ch))
						return true;
		}
		return false;
	}
}