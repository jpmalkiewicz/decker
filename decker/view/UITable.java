package decker.view;
import decker.model.*;
import java.awt.*;




class UITable extends DisplayedComponent
{
	private int[] column_width, column_x;
	private int row_height, total_width, columns;
	private TableCellWrapper[][] cell; // cell[row][column]


	UITable (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		update(0, current_clip_source);
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		w = total_width;
		h = (cell != null) ? (row_height * cell.length) : 0;
	}




	public void draw (final Graphics g) {
		if (cell != null) {
			for (int i = cell.length; --i >= 0; ) {
				if (i < cell.length) { // to avoid errors when a cell has an on_draw function that changed the table
					final TableCellWrapper[] row = cell[i];
					for (int j = row.length; --j >= 0; ) {
						if (row[j].cell_content != null) {
System.out.print("("+j+","+i+")");
							row[j].cell_content.draw(g);
						}
					}
				}
			}
		}
	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
		if (component.type() == Value.STRUCTURE && component.get("structure_type").equals("TABLE")) {
			if (variable_name.equals("width") && component.get("width").equals(w))
				return;
			if (variable_name.equals("height") && component.get("height").equals(h))
				return;
			if (variable_name.equals("columns") && !component.get("columns").equals(columns)) {
				// if the table is defined via column count and a single width value for all columns, update it
				if (component.get("cell_width").type() == Value.INTEGER)
					update(CUSTOM_SIZE, getCurrentClipSource());
// otherwise ignore it
else
System.out.println("UITable : ignoring a changs to TABLE.columns");
				return;
			}
			if (variable_name.equals("rows")) {
				final Value v = component.get("rows");
				if (cell != null && !v.equals(cell.length))
					v.set(cell.length);
				else if (cell == null && !v.equals(0))
					v.set(0);
				return;
			}
			if (variable_name.equals("x") || variable_name.equals("y")) {
				// check whether the position really did change, and whether the changed variable has a legal value
				final int old_x = x, old_y = y;
				x = determineX(component, parent.x, parent.w, w);
				y = determineY(component, parent.y, parent.h, h);
				if (( x != old_x || y != old_y )&& cell != null) {
					// x or y has changed. move the table contents
					DisplayedComponent k;
					final int dx = x-old_x;
					final int dy = y-old_y;
					for (int i = cell.length; --i >= 0; ) {
						for (int j = cell[i].length; --j >= 0; ) {
							cell[i][j].x += dx;
							cell[i][j].y += dy;
							if ((k=cell[i][j].cell_content) != null) {
								k.x += dx;
								k.y += dy;
							}
						}
					}
				}
				return;
			}
			update(CUSTOM_SIZE, getCurrentClipSource());
		}
	}




	public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		final Structure d = component.structure();
		Value v;
		// check whether the size or formatting has changed
		int rows = d.get("rows").integer();
		columns = d.get("columns").integer();
		final int rowh = d.get("cell_height").integer();
		// calculate the x and width of each column
		final int[] colx = new int[columns];
		final int[] colw = new int[columns];
		int totalw;
		total_width = 0;
		if ((v=d.get("cell_width")).type() == Value.INTEGER) {
			final int cellw = v.integer();
			for (int i = 0; i < columns; i++) {
				colx[i] = i*cellw;
				colw[i] = cellw;
			}
			totalw = cellw*columns;
		}
		else if (v.type() != Value.ARRAY) {
			columns = -1;
			System.err.println("unable to render table : cell_width must be an INTEGER or ARRAY but is a "+v.typeName());
			return;
		}
		else { // cell_width is an ARRAY
			final Value[] a = v.array();
			columns = a.length;
			totalw = 0;
			for (int i = 0; i < a.length; i++) {
				colx[i] = totalw;
				colw[i] = a[i].integer();
				totalw += colw[i];
			}
			// if the columns value is wrong, fix it
			if (!(v=d.get("columns")).equals(columns)) {
				v.set(columns);
			}
		}
		// if the number of columns has changed, rebuild the whole table
//		if (cell == null || column_width.length != columns) {
if (true) {
			// destroy the old DisplayComponents
			if (cell != null) {
				for (int i = cell.length; --i >= 0; ) {
					for (int j = cell[i].length; --j >= 0; ) {
						if (cell[i][j] != null) {
							cell[i][j].cell_content.destroy();
							cell[i][j].cell_content = null;
						}
					}
				}
			}
			// create the new table
			column_width = colw;
			column_x = colx;
			row_height = rowh;
			total_width = totalw;
			w = totalw;
			if (!(v=d.get("width")).equals(w))
				v.set(w);
			h = rows*rowh;
			if (!(v=d.get("height")).equals(h))
				v.set(h);
// trigger a size_changed event if necessary
			x = determineX(component, parent.x, parent.w, w);
			y = determineY(component, parent.y, parent.h, h);
			if ((v=d.get("cell")).type() == Value.ARRAY) {
				final Value[] row_data = v.array();
System.out.println("UITable : table has "+row_data.length+" rows");
				if (rows != row_data.length) {
					rows = row_data.length;
					d.get("rows").set(rows);
				}
				// create the new array if the number of rows has changed
				if (cell == null || rows != cell.length ||( rows > 0 && cell[0].length != columns )) {
					cell = new TableCellWrapper[rows][columns];
				}
				for (int i = (row_data.length<rows)?row_data.length:rows; --i >= 0; ) {
					if (row_data[i].type() == Value.ARRAY) {
						final Value[] column_data = row_data[i].array();
						for (int j = (column_data.length<columns)?column_data.length:columns; --j >= 0; ) {
							// we need a wrapper for every cell, so the component that sits in that cell can poll the cell boundary when positioning itself
							if (cell[i][j] == null) {
								cell[i][j] = new TableCellWrapper(this, x + colx[j], y + i*rowh, colw[j], rowh);
							}
							else {
								cell[i][j].x = x + colx[j];
								cell[i][j].y = y + i*rowh;
								cell[i][j].w = colw[j];
								cell[i][j].h = rowh;
							}
							if ((v=column_data[j]) != null && !v.equalsConstant("UNDEFINED")) {
								cell[i][j].cell_content = createDisplayedComponent(v, cell[i][j], current_clip_source);
							}
						}
					}
				}
			}
			else {
				cell = null;
				if (rows != 0)
					d.get("rows").set(0);
			}
System.out.println("UITable   : "+x+" "+y+"  "+w+" "+h+"    "+columns+" "+rows+"   "+cell[0][1].cell_content.x+" "+cell[0][2].cell_content.x+" "+cell[0][3].cell_content.x);
		}
System.out.println("UITable   : "+x+" "+y+"  "+w+" "+h+"    "+columns+" "+rows);
super.update(customSettings|CUSTOM_SIZE, current_clip_source);
System.out.println("UITable * : "+x+" "+y+"  "+w+" "+h+"    "+columns+" "+rows);
//System.exit(0);
	}




	static class TableCellWrapper extends DisplayedComponent
	{
		DisplayedComponent cell_content;



		TableCellWrapper (final UITable parent, final int _x, final int _y, final int _w, final int _h) {
			super(null, parent);
			child_count = 0;
			x = _x;
			y = _y;
			w = _w;
			h = _h;
		}



		void destroy () { if (cell_content != null) { cell_content.destroy(); cell_content = null; } }
		void draw (final Graphics g) {}
		public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {}
		public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {}
		void update (final int customSettings, final DisplayedComponent current_clip_source) {}
	}
}
