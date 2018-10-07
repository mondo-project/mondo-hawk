package org.hawk.osgiserver;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Job scheduling rule that prevents two jobs from running at once on the same
 * Hawk HModel.
 */
public class HModelSchedulingRule implements ISchedulingRule {
	private final HModel hawkModel;

	public HModelSchedulingRule(HModel hawkModel) {
		this.hawkModel = hawkModel;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		if (rule instanceof HModelSchedulingRule) {
			return ((HModelSchedulingRule)rule).hawkModel == this.hawkModel;
		}
		return false;
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		return rule == this;
	}
}