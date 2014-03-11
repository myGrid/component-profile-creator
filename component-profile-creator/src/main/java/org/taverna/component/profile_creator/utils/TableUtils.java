package org.taverna.component.profile_creator.utils;

import java.awt.event.ActionEvent;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class TableUtils {
	public static void installDelegatingColumn(TableColumn column, String label) {
		column.setMaxWidth(new JButton(label).getPreferredSize().width);
		column.setCellRenderer(new TableCellRenderer() {
			@Override
			public JComponent getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				return (JComponent) value;
			}
		});
		column.setCellEditor(new TableCellEditor() {
			@Override
			public Object getCellEditorValue() {
				return null;
			}

			@Override
			public boolean isCellEditable(EventObject anEvent) {
				return true;
			}

			@Override
			public boolean shouldSelectCell(EventObject anEvent) {
				return false;
			}

			@Override
			public boolean stopCellEditing() {
				return true;
			}

			@Override
			public void cancelCellEditing() {
			}

			@Override
			public void addCellEditorListener(CellEditorListener l) {
			}

			@Override
			public void removeCellEditorListener(CellEditorListener l) {
			}

			@Override
			public JComponent getTableCellEditorComponent(JTable table,
					Object value, boolean isSelected, int row, int column) {
				return (JComponent) value;
			}
		});
	}

	public static void configureColumn(JTable table, int columnNumber,
			Integer width, TableCellRenderer renderer, TableCellEditor editor) {
		TableColumn column = table.getColumnModel().getColumn(columnNumber);
		if (width != null)
			column.setMaxWidth(width);
		if (renderer != null)
			column.setCellRenderer(renderer);
		if (editor != null)
			column.setCellEditor(editor);
	}

	@SuppressWarnings("serial")
	public static class RowDeletionAction extends AbstractAction {
		private final JTable table;

		public RowDeletionAction(JTable table) {
			super("Del");
			this.table = table;
		}

		@Override
		public final void actionPerformed(ActionEvent e) {
			DefaultTableModel dtm = (DefaultTableModel) table.getModel();
			Vector<?> data = (Vector<?>) dtm.getDataVector().get(
					table.getSelectedRow());
			rowDeleted(data.toArray());
			dtm.removeRow(table.getSelectedRow());
		}

		protected void rowDeleted(Object[] rowData) {
			// Do nothing by default...
		}
	}
}
