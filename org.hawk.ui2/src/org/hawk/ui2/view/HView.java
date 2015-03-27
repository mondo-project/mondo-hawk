package org.hawk.ui2.view;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.wizards.IWizardDescriptor;
import org.hawk.ui2.Activator;
import org.hawk.ui2.dialog.HConfigDialog;
import org.hawk.ui2.dialog.HQueryDialog;
import org.hawk.ui2.util.HManager;
import org.hawk.ui2.util.HModel;
import org.osgi.framework.FrameworkUtil;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class HView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "hawk.ui.view.HawkView";

	private TableViewer viewer;
	private Action query;
	private Action start;
	private Action stop;
	private Action delete;
	private Action add;
	private Action config;

	// private Action doubleClickAction;

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		protected Image image = null;

		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {

			if (image == null) {
				image = Activator.getImageDescriptor("icons/hawk.png")
						.createImage();
			}

			return image;
		}
	}

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public HView() {
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		HManager hm = HManager.getInstance();
		// hm.setView(this);
		viewer.setContentProvider(hm);
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(viewer.getControl(), "hawkview.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		viewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection.isEmpty() || selection.size() > 1)
					initButtons();

				if (selection.size() == 1) {
					enableButtons();

					if (((HModel) selection.getFirstElement()).isRunning()) {
						start.setEnabled(false);
						stop.setEnabled(true);
						query.setEnabled(true);
						config.setEnabled(true);
					} else {
						stop.setEnabled(false);
						start.setEnabled(true);
						query.setEnabled(false);
						config.setEnabled(false);
					}
				}

			}

		});
	}

	private void initButtons() {
		query.setEnabled(false);
		start.setEnabled(false);
		stop.setEnabled(false);
		delete.setEnabled(false);

		config.setEnabled(false);
	}

	private void enableButtons() {
		query.setEnabled(true);
		start.setEnabled(true);
		stop.setEnabled(true);
		delete.setEnabled(true);

		config.setEnabled(true);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				HView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(query);
		manager.add(start);
		manager.add(stop);
		manager.add(delete);
		manager.add(add);
		manager.add(config);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(query);
		manager.add(start);
		manager.add(stop);
		manager.add(delete);
		manager.add(add);
		manager.add(config);
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(query);
		manager.add(start);
		manager.add(stop);
		manager.add(delete);
		manager.add(add);
		manager.add(config);
	}

	private void makeActions() {
		query = new Action() {
			public void run() {
				IStructuredSelection selected = (IStructuredSelection) viewer
						.getSelection();

				HQueryDialog dialog = new HQueryDialog(PlatformUI
						.getWorkbench().getActiveWorkbenchWindow().getShell(),
						(HModel) selected.getFirstElement());
				dialog.open();
			}

		};
		query.setText("Query");
		query.setToolTipText("Query");
		query.setImageDescriptor(ImageDescriptor.createFromURL(FileLocator
				.find(FrameworkUtil.getBundle(this.getClass()), new Path(
						"icons/query.gif"), null)));

		start = new Action() {
			public void run() {
				IStructuredSelection selected = (IStructuredSelection) viewer
						.getSelection();
				if (selected.size() == 1) {
					((HModel) selected.getFirstElement()).start();
					viewer.refresh();
					start.setEnabled(false);
					stop.setEnabled(true);
					query.setEnabled(true);
					config.setEnabled(true);
				}
			}
		};
		start.setText("Start");
		start.setToolTipText("Start");
		start.setImageDescriptor(ImageDescriptor.createFromURL(FileLocator
				.find(FrameworkUtil.getBundle(this.getClass()), new Path(
						"icons/nav_go.gif"), null)));

		stop = new Action() {
			public void run() {
				IStructuredSelection selected = (IStructuredSelection) viewer
						.getSelection();
				if (selected.size() == 1) {
					((HModel) selected.getFirstElement()).stop();
					viewer.refresh();
					start.setEnabled(true);
					stop.setEnabled(false);
					query.setEnabled(false);
					config.setEnabled(false);
				}
			}
		};
		stop.setText("Stop");
		stop.setToolTipText("Stop");
		stop.setImageDescriptor(ImageDescriptor.createFromURL(FileLocator.find(
				FrameworkUtil.getBundle(this.getClass()), new Path(
						"icons/stop.gif"), null)));

		delete = new Action() {
			public void run() {
				IStructuredSelection selected = (IStructuredSelection) viewer
						.getSelection();
				if (selected.size() == 1) {
					HManager.delete((HModel) selected.getFirstElement());
					viewer.refresh();
				}
			}
		};
		delete.setText("Delete");
		delete.setToolTipText("Delete");
		delete.setImageDescriptor(ImageDescriptor.createFromURL(FileLocator
				.find(FrameworkUtil.getBundle(this.getClass()), new Path(
						"icons/rem_co.gif"), null)));

		add = new Action() {
			public void run() {
				IWizardDescriptor descriptor = PlatformUI.getWorkbench()
						.getNewWizardRegistry()
						.findWizard("hawk.ui.wizard.HawkNewWizard");
				if (descriptor != null) {
					Shell activeShell = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell();
					IWizard wizard;
					try {
						wizard = descriptor.createWizard();
						WizardDialog wd = new WizardDialog(activeShell, wizard);
						wd.setTitle(wizard.getWindowTitle());
						wd.open();
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		};
		add.setText("Add");
		add.setToolTipText("Add");
		add.setImageDescriptor(ImageDescriptor.createFromURL(FileLocator.find(
				FrameworkUtil.getBundle(this.getClass()), new Path(
						"icons/new-hawk.png"), null)));

		config = new Action() {
			public void run() {
				IStructuredSelection selected = (IStructuredSelection) viewer
						.getSelection();

				HConfigDialog dialog = new HConfigDialog(PlatformUI
						.getWorkbench().getActiveWorkbenchWindow().getShell(),
						((HModel) selected.getFirstElement()));
				dialog.open();
			}
		};
		config.setText("Configure");
		config.setToolTipText("Configure");
		config.setImageDescriptor(ImageDescriptor.createFromURL(FileLocator
				.find(FrameworkUtil.getBundle(this.getClass()), new Path(
						"icons/configure.gif"), null)));

		initButtons();
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (config.isEnabled())
					config.run();
			}
		});
	}

	// private void showMessage(String message) {
	// MessageDialog.openInformation(viewer.getControl().getShell(), "Hawk",
	// message);
	// }

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public void update() {
		viewer.refresh();
	}

}