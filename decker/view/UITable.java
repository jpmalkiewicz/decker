package decker.view;
import decker.model.*;
import java.awt.*;




class UITable extends DisplayedComponent
{
	private int[] column_width, column_x;
	private int row_height, total_width;
	private TableCellWrapper[][] cell; // cell[row][column]


	UITable (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		update(0, current_clip_source);
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		if (!width_already_determined)
			w = total_width;
		if (!height_already_determined)
			h = row_height * cell.length;
	}




	public void draw (final Graphics g) {

	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		final Structure d = component.structure();
		Value v;
		// check whether the size or formatting has changed
		final int rows = d.get("rows").integer();
		final int columns = d.get("columns").integer();
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
		else { // v must be an ARRAY
			final Value[] a = v.array();
			for (int i = 0; i < columns; i++) {
				colx[i] = total_width;
				colw[i] = a[i].integer();
			}
			totalw = colx[columns-1] + colw[columns-1];
		}
		// if the number of columns has changed, rebuild the whole table
//		if (cell == null || column_width.length != columns) {
if (true) {
			// destroy the old DisplayComponents
			if (cell != null) {
				for (int i = cell.length; --i >= 0; ) {
					for (int j = cell[i].length; --j >= 0; ) {
						if (cell[i][j] != null) {
							cell[i][j].destroy();
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
			h = rows*rowh;
// trigger a size_changed event if necessary
			cell = new TableCellWrapper[rows][columns];
			x = determineX(component, parent.x, parent.w, w);
			y = determineY(component, parent.y, parent.h, h);
			final Value[] row_data = d.get("cell").array();
			for (int i = (row_data.length<rows)?row_data.length:rows; --i >= 0; ) {
				final Value[] column_data = row_data[i].array();
				for (int j = (column_data.length<columns)?column_data.length:columns; --j >= 0; ) {
					// we need a wrapper for every cell, so the component that sits in that cell can poll the cell boundary when positioning itself
					cell[i][j] = new TableCellWrapper(this, x + colx[j], y + i*rowh, colw[j], rowh);
					if ((v=column_data[j]) != null) {
						cell[i][j].cell_content = createDisplayedComponent(v, cell[i][j], current_clip_source);
					}
				}
			}
		}
super.update(customSettings|CUSTOM_SIZE, current_clip_source);
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
