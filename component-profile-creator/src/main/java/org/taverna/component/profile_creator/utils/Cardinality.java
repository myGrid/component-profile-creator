package org.taverna.component.profile_creator.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.math.BigInteger;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public enum Cardinality {
	FORBIDDEN("forbidden", 0, 0), OPTIONAL("optional", 0, 1), MANDATORY(
			"required", 1, 1), ANY_NUMBER("any number", 0, null), AT_LEAST_ONE(
			"at least one", 1, null);
	private final String name;
	private final int min;
	private final Integer max;

	private Cardinality(String descriptiveName, int lowerBound,
			Integer upperBound) {
		name = descriptiveName;
		min = lowerBound;
		max = upperBound;
	}

	public static final List<Cardinality> CARDINALITIES = unmodifiableList(asList(new Cardinality[] {
			FORBIDDEN, OPTIONAL, MANDATORY, ANY_NUMBER, AT_LEAST_ONE }));

	@Override
	public String toString() {
		return name;
	}

	public BigInteger getLowerBound() {
		return BigInteger.valueOf(min);
	}

	public String getUpperBound() {
		return max == null ? "unbounded" : max.toString();
	}

	public static Cardinality get(BigInteger lowerBound, String upperBound) {
		boolean lowNonZero = lowerBound.intValue() > 0;
		if (upperBound.equals("unbounded"))
			return (lowNonZero ? AT_LEAST_ONE : ANY_NUMBER);
		if (lowNonZero)
			return MANDATORY;
		return (Integer.parseInt(upperBound) > 0 ? OPTIONAL : FORBIDDEN);
	}

	@SuppressWarnings("serial")
	public static TableCellRenderer tableRenderer() {
		return new DefaultTableCellRenderer() {
			@Override
			public void setValue(Object value) {
				super.setValue(((Cardinality) value).toString());
			}
		};
	}

	@SuppressWarnings({ "serial" })
	public static ListCellRenderer<Object> listRenderer() {
		return (ListCellRenderer<Object>) new DefaultListCellRenderer() {
			@Override
			public JComponent getListCellRendererComponent(JList<?> list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				if (isSelected) {
					setBackground(list.getSelectionBackground());
					setForeground(list.getSelectionForeground());
				} else {
					setBackground(list.getBackground());
					setForeground(list.getForeground());
				}
				setText(((Cardinality) value).toString());
				return this;
			}
		};
	}

	public static TableCellEditor tableEditor() {
		JComboBox<Cardinality> statements = new JComboBox<>();
		statements.setRenderer(listRenderer());
		for (Cardinality c : CARDINALITIES)
			statements.addItem(c);
		return new DefaultCellEditor(statements);
	}
}
