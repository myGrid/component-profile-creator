package org.taverna.component.profile_creator.utils;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
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

	public static void makeRowsDialogEditable(JTable table,
			ShowDialog editCallback) {
		table.setDefaultRenderer(Object.class, new DialogRowRenderer());
		table.setDefaultEditor(Object.class, new DialogRowEditor(editCallback));
	}

	public static void setRowLines(JTable table, int row, int lines) {
		if (lines == 0)
			lines = 1;
		table.setRowHeight(row, lines * table.getRowHeight());
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

	public interface ShowDialog {
		Object[] edit(Object[] row, int rowNumber);
	}

	@SuppressWarnings("serial")
	private static class DialogRowEditor extends AbstractCellEditor implements
			TableCellEditor, ActionListener {
		private Object[] newInput;
		private Object[] oldValue;
		private int row, column;
		private final JButton button;
		private final ShowDialog doShow;
		private JTable table;
		private static final String EDIT = "edit";

		public DialogRowEditor(ShowDialog callback) {
			button = new JButton();
			button.setActionCommand(EDIT);
			button.addActionListener(this);
			button.setBorderPainted(false);
			doShow = callback;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (EDIT.equals(e.getActionCommand())) {
				newInput = doShow.edit(oldValue, row);
				if (newInput == null || newInput.length != oldValue.length)
					newInput = oldValue;
				fireEditingStopped();
				for (int i = 0; i < newInput.length; i++)
					if (i != column)
						table.setValueAt(newInput[i], row, i);
			}
		}

		@Override
		public Object getCellEditorValue() {
			return newInput[column];
		}

		@Override
		public Component getTableCellEditorComponent(JTable table,
				Object value, boolean isSelected, int row, int column) {
			newInput = ((Vector<?>) ((DefaultTableModel) table.getModel())
					.getDataVector().get(row)).toArray();
			oldValue = newInput.clone();
			this.row = row;
			this.column = column;
			this.table = table;
			button.setText(value.toString());
			return button;
		}
	}

	@SuppressWarnings("serial")
	private static class DialogRowRenderer extends JLabel implements
			TableCellRenderer {
		public DialogRowRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			setText(value.toString());
			if (isSelected) {
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			} else {
				setBackground(table.getBackground());
				setForeground(table.getForeground());
			}
			return this;
		}
	}
}
