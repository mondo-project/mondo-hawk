package org.hawk.ui2.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.*;
import org.hawk.core.util.HawkConfig;
import org.hawk.ui2.util.HModel;
import org.hawk.ui2.view.HView;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * This is a sample new wizard.
 */

public class HWizard extends Wizard implements INewWizard {
	private HWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for HawkNewWizard.
	 */
	public HWizard() {
		super();
		setNeedsProgressMonitor(false);
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new HWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We
	 * will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		final String folder = page.getContainerName();
		final String dbType = page.getDBID();
		final List<String> plugins = page.getPlugins();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish(folder, dbType, plugins, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method.
	 * 
	 * @param dbType
	 */

	private void doFinish(String folder, String dbType, List<String> plugins,
			IProgressMonitor monitor) throws CoreException {

		// set up a new Hawk with the selected plugins

		try {
			// create a new hawk index at containerName folder with name
			// fileName
			HModel.create(new File(folder), dbType, plugins);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// open hawk view, tell the view about the new index

		monitor.beginTask("Creating ", 2);

		monitor.worked(1);
		monitor.setTaskName("Opening Hawk interface...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				try {
					HView view = (HView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.showView("hawk.ui.view.HawkView");
					view.update();
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}
		});

		// add this new hawk to metadata
		IEclipsePreferences preferences = InstanceScope.INSTANCE
				.getNode("org.hawk.ui2");

		String xml = preferences.get("config", "error");

		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawkConfig.class);
		stream.setClassLoader(HawkConfig.class.getClassLoader());

		HawkConfig hc = null;

		try {

			if (!xml.equals("error")) {
				hc = (HawkConfig) stream.fromXML(xml);
			}

			HashSet<String> locs = new HashSet<String>();

			if (hc != null)
				locs.addAll(hc.getLocs());

			locs.add(folder);

			xml = stream.toXML(new HawkConfig(locs));

			preferences.put("config", xml);

		} catch (Exception e) {
			e.printStackTrace();
		}

		monitor.worked(1);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}
}