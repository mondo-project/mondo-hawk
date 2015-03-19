package org.hawk.ui2.wizard;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.*;
import org.hawk.ui2.util.HModel;
import org.hawk.ui2.view.HView;

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
		final String index = page.getIndexerName();
		final String dbid = page.getDBID();
		final List<String> plugins = page.getPlugins();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					doFinish(folder, index, dbid, plugins, monitor);
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
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error",
					realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method.
	 */

	private void doFinish(String folderName, String indexerName, String dbid,
			List<String> plugins, IProgressMonitor monitor)
			throws CoreException {

		// set up a new Hawk with the selected plugins

		try {
			// create a new hawk index at containerName folder with name
			// fileName
			HModel.create(indexerName, folderName, dbid, plugins);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// open hawk view, tell the view about the new index

		monitor.beginTask("Creating " + indexerName, 2);

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
		monitor.worked(1);
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {

	}
}