package decker.view;
import decker.model.*;
import java.awt.*;

class UITable extends DisplayedComponent
{
//	private int[] column_width, column_x;
	private int row_height, total_width, columns;
	private TableCellWrapper[][] cell; // cell[row][column]
	private boolean can_drag_rows, can_select_rows;
	private boolean dragging_row;
	private int current_row = -1; // the selected or dragged row
	private final Value selected_row_background = new Value();

	UITable (Value _component, DisplayedComponent _parent, DisplayedComponent current_clip_source) {
		super(_component, _parent);
		update(0, current_clip_source);
		_component.structure().addValueListener(this);
	}

	void destroy () {
		if (cell != null) {
			for (int i = cell.length; --i >= 0; ) {
				final TableCellWrapper[] row = cell[i];
				for (int j = row.length; --j >= 0; ) {
					row[j].destroy();
					row[j] = null;
				}
				cell[i] = null;
			}
		}
		cell = null;
		if (component.type() == Value.STRUCTURE) {
			component.structure().removeValueListener(this);
		}
		super.destroy();
	}


	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		w = total_width;
		h = (cell != null) ? (row_height * cell.length) : 0;
	}

	public void draw (final Graphics g) {
		if (cell != null) {
			// mark the selected row
			if (can_select_rows && current_row > -1) {
				if (selected_row_background.type() == Value.STRING) {
					final Color c = AbstractView.getColor(selected_row_background.string());
					if (c != null) {
						g.setColor(c);
						g.fillRect(x, y+current_row*row_height, w, row_height);
					}
				}
			}
			// draw the cell contents
			for (int i = cell.length; --i >= 0; ) {
				if (i < cell.length) { // to avoid errors when a cell has an on_draw function that changed the table
					final TableCellWrapper[] row = cell[i];
					for (int j = row.length; --j >= 0; ) {
						if (row[j].cell_content != null) {
							row[j].cell_content.draw(g);
						}
					}
				}
			}
		}
	}




	void eventPositionChanged (final DisplayedComponent current_clip_source, final int dx, final int dy) {
		super.eventPositionChanged(current_clip_source, dx, dy);
		for (int i = cell.length; --i >= 0; ) {
			for (int j = cell[i].length; --j >= 0; ) {
				cell[i][j].x += dx;
				cell[i][j].y += dy;
				final DisplayedComponent k = cell[i][j].cell_content;
				if (k != null) {
					k.eventPositionChanged(current_clip_source, dx, dy);
				}
			}
		}
	}




	boolean eventUserInput (final int event_id, final AWTEvent e, final int mouse_x, final int mouse_y, final int mouse_dx, final int mouse_dy) {
		final int my = mouse_y-y;
		if (row_height <= 0 || my < 0 || my >= h || mouse_x < x || mouse_x >= x+w || component == null || component.type() != Value.STRUCTURE || !component.get("structure_type").equals("TABLE")) {
			dragging_row = false;
			return true;
		}
		Value v;
		final int old_current_row = current_row;
		switch (event_id) {
			case ON_MOUSE_DOWN :
					if (can_select_rows || can_drag_rows) {
						current_row = my / row_height;
					}
				break;
			case ON_MOUSE_DRAGGED :
					if (can_drag_rows && dragging_row) {
						int target_row = my / row_height;
//						int target_row = (my+row_height/2) / row_height;
//						if (target_row > current_row)
//							target_row--;
						if (target_row != current_row) {
							// move the row in the displayed table and its script representation
							v = component.get("cell");
							if (v.type() == Value.ARRAY) {
								final Value[] rows = v.array();
								if (current_row > -1 && current_row < rows.length && target_row < rows.length) {
									// all the data is valid, move the row
									final TableCellWrapper[] d_row = cell[current_row];
									final Value s_row = rows[current_row];
									if (current_row < target_row) {
										System.arraycopy(cell, current_row+1, cell, current_row, target_row-current_row);
										System.arraycopy(rows, current_row+1, rows, current_row, target_row-current_row);
									}
									else {
										System.arraycopy(cell, target_row, cell, target_row+1, current_row-target_row);
										System.arraycopy(rows, target_row, rows, target_row+1, current_row-target_row);
									}
									cell[target_row] = d_row;
									rows[target_row] = s_row;
System.out.println("moving UITable row : update the cell positions and cell content positions");
// update the position of all moved displayed components
update(0, getCurrentClipSource());
									// adjust the current row index
									current_row = target_row;
									// call the row drag listener function, if there is one
									if ((v=component.get("on_row_dragged")) != null && v.type() == Value.FUNCTION) {
										Value[] args = new Value[]{ new Value().set(component), new Value(), new Value() };
										if (old_current_row > -1)
											args[1].set(old_current_row);
										if (current_row > -1)
											args[2].set(current_row);
										FunctionCall.executeFunctionCall(v.function(), args, null);
									}
								}
							}
						}
					}
					else {
						current_row = my / row_height;
					}
				break;
			case ON_MOUSE_EXITED :
					dragging_row = false;
				break;
			case ON_MOUSE_UP :
					dragging_row = false;
				break;
		}
		if (can_select_rows && current_row != old_current_row) {
			component.get("selected_row").set(current_row);
			if (!dragging_row && (v=component.get("on_selection_change")) != null && v.type() == Value.FUNCTION) {
				Value[] args = new Value[]{ new Value().set(component), new Value(), new Value() };
				if (old_current_row > -1)
					args[1].set(old_current_row);
				if (current_row > -1)
					args[2].set(current_row);
				FunctionCall.executeFunctionCall(v.function(), args, null);
			}
		}
		if (event_id == ON_MOUSE_DOWN && can_drag_rows) {
			dragging_row = true;
		}
		return true;
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
System.out.println("UITable : ignoring a change to TABLE.columns");
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
					final int dx = x-old_x;
					final int dy = y-old_y;
					eventPositionChanged(getCurrentClipSource(), dx, dy);
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
		if (component == null || component.type() != Value.STRUCTURE || !component.get("structure_type").equals("TABLE")) {
			super.update(0, current_clip_source);
		}
		final Structure d = component.structure();
		Value v;
		// update whether a row can be selected / dragged
		hasHardcodedEventFunction[ON_MOUSE_DOWN] = false;
		hasHardcodedEventFunction[ON_MOUSE_DRAGGED] = false;
		hasHardcodedEventFunction[ON_MOUSE_EXITED] = false;
		hasHardcodedEventFunction[ON_MOUSE_UP] = false;
		selected_row_background.set(d.get("selected_row_background"));
		can_select_rows = !selected_row_background.equalsConstant("UNDEFINED");
		can_drag_rows = d.get("can_drag_rows").equals(true);
		if (can_select_rows) {
			hasHardcodedEventFunction[ON_MOUSE_DOWN] = true;
			hasHardcodedEventFunction[ON_MOUSE_DRAGGED] = true;
		}
		if (can_drag_rows) {
			hasHardcodedEventFunction[ON_MOUSE_DOWN] = true;
			hasHardcodedEventFunction[ON_MOUSE_DRAGGED] = true;
			hasHardcodedEventFunction[ON_MOUSE_EXITED] = true;
			hasHardcodedEventFunction[ON_MOUSE_UP] = true;
		}
		else {
			dragging_row = false;
		}
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
						if (cell[i][j].cell_content != null) {
							cell[i][j].cell_content.destroy();
							cell[i][j].cell_content = null;
						}
					}
				}
			}
			// create the new table
//			column_width = colw;
//			column_x = colx;
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
		}
		// fetch the selected row
		if (can_select_rows && (v=d.get("selected_row")).type() == Value.INTEGER && v.integer() > -1 && v.integer() < rows)
			current_row =v.integer();
		// finally call the super.update function to update anything that has been omitted so far
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
