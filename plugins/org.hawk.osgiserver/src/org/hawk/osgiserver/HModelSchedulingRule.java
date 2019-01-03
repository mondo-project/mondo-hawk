/*******************************************************************************
 * Copyright (c) 2018 Aston University.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 3.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-3.0
 *
 * Contributors:
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
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