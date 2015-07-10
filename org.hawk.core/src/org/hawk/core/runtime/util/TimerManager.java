/*******************************************************************************
 * Copyright (c) 2011-2015 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.core.runtime.util;

import java.util.HashSet;
import java.util.Timer;

public class TimerManager {

	public static HashSet<Timer> timers = new HashSet<Timer>();
	
	public static Timer createNewTimer(String string, boolean b) {

		Timer timer = new Timer(string,b);
		//
		timers.add(timer);

		return timer;

	}

}
