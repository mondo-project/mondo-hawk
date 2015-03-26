package org.hawk.ui2.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.hawk.core.IAbstractConsole;
import org.hawk.ui2.Activator;

public class EclipseLogConsole implements IAbstractConsole {

	@Override
	public void println(String s) {
		log(s, IStatus.INFO);		
	}

	@Override
	public void printerrln(String s) {
		log(s, IStatus.ERROR);		
	}

	@Override
	public void print(String s) {
		println(s);
	}
	
	protected void log(String s, int severity) {
		Activator.getDefault().getLog().log(new Status(severity, "org.hawk.ui2", 0, s, null));				
	}

}
