package org.taverna.component.profile_creator.utils;

import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class PositiveUnboundedModel extends SpinnerListModel implements
		Comparable<PositiveUnboundedModel> {
	private boolean unbounded, unboundable;
	private SpinnerNumberModel in;

	public PositiveUnboundedModel(boolean mayBeUnbounded) {
		in = new SpinnerNumberModel(0, 0, null, 1);
		unbounded = unboundable = mayBeUnbounded;
	}

	@Override
	public Object getNextValue() {
		if (unbounded)
			return 0;
		return in.getNextValue();
	}

	@Override
	public Object getPreviousValue() {
		if (unbounded)
			return null;
		Object prev = in.getPreviousValue();
		if (prev == null && unboundable)
			prev = "unbounded";
		return prev;
	}

	@Override
	public Object getValue() {
		if (unbounded)
			return "unbounded";
		return in.getValue();
	}

	public Integer getBoundValue() {
		if (unbounded)
			return null;
		return (Integer) in.getNumber();
	}

	@Override
	public void setValue(Object value) {
		boolean old = unbounded;
		unbounded = value.equals("unbounded") && unboundable;
		if (!unbounded) {
			in.setValue(value);
			fireStateChanged();
		} else if (old != unbounded)
			fireStateChanged();
	}

	@Override
	public int compareTo(PositiveUnboundedModel other) {
		Integer me = getBoundValue(), them = other.getBoundValue();
		if (me == null)
			return (them == null ? 0 : 1);
		if (them == null)
			return -1;
		return me.compareTo(them);
	}

	public static void couple(final PositiveUnboundedModel min,
			final PositiveUnboundedModel max) {
		min.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (min.compareTo(max) > 0)
					max.setValue(min.getValue());
			}
		});
		max.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (max.compareTo(min) < 0)
					min.setValue(max.getValue());
			}
		});
	}
}