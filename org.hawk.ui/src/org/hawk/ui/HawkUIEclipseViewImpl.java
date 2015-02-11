/*******************************************************************************
 * Copyright (c) 2011-2014 The University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Konstantinos Barmpis - initial API and implementation
 ******************************************************************************/
package org.hawk.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.hawk.core.IHawkUI;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.runtime.util.SecurityManager;
import org.hawk.ui.util.AbstractConsoleImpl;
import org.hawk.ui.util.AddIndexerDialog;
import org.hawk.ui.util.StartDialog;

public class HawkUIEclipseViewImpl extends ViewPart implements IHawkUI {

	private HashMap<String, IModelResourceFactory> modelparsers = new HashMap<>();
	private HashMap<String, IMetaModelResourceFactory> metamodelparsers = new HashMap<>();
	private HashMap<String, IQueryEngine> knownQueryLanguages = new HashMap<>();
	private LinkedList<IModelUpdater> updaters = new LinkedList<>();
	// HashMap<String, HawkMetaModelResource> registeredMetamodels = new
	// HashMap<>();

	public static final String ID = "org.hawk.ui.HawkServerEclipseViewImpl";

	private HawkUIEclipseViewImpl view = this;

	static AbstractConsoleImpl myConsole = null;

	// private TableViewer viewer;
	private Action action2;
	private Action action1;
	private Action action4;
	private Action action5;

	private ArrayList<String> vcsUn = new ArrayList<>();
	private ArrayList<String> vcsPw = new ArrayList<>();
	private ArrayList<String> vcsLoc = new ArrayList<>();
	private ArrayList<String> vcsType = new ArrayList<>();
	private String indexerName = "";
	private String indexName = "";
	private String indexType = "";

	private boolean addNew = false;

	// private boolean removed = false;

	private static Button addIndexer;
	private static Button start;
	private static Combo selectIndexer;
	private static StyledText text;
	private static StyledText text2;

	public static Set<String> indexTypes = null;
	public static Set<String> vcsTypes = null;
	// public static Set<String> modelTypes = null;

	private IModelIndexer selectedIndexer = null;

	private char[] adminPw = null;

	static Composite window;

	private File parentfolder = new File(Activator.getDefault()
			.getStateLocation().toString().replaceAll("\\\\", "/"));
	private File metadata = new File(parentfolder.getAbsolutePath().replaceAll(
			"\\\\", "/")
			+ "/.metadata");

	private static HashMap<String, IModelIndexer> indexers = new HashMap<>();

	private HawkOSGIConfigManager configmanager;

	private IMetaModelUpdater metaModelUpdater;

	public static Set<String> getIndexerNames() {
		return indexers.keySet();
	}

	public static IModelIndexer getIndexerByName(String name) {
		return indexers.get(name);
	}

	// static ModelIndexer indexer;
	public void setVCSUn(String un2) {
		vcsUn.add(un2);
	}

	public void setVCSPw(String pw2) {
		vcsPw.add(pw2);
	}

	@Override
	public void createPartControl(final Composite parent) {

		Activator.getDefault().setView(this);

		//
		myConsole = new AbstractConsoleImpl("NoSQL/VCS Index Server Console");

		Composite container = new Composite(parent, SWT.NONE);
		FillLayout f = new FillLayout();
		f.spacing = 20;
		container.setLayout(f);

		final ScrolledComposite sc = new ScrolledComposite(container,
				SWT.H_SCROLL | SWT.V_SCROLL);
		sc.setLayout(new FillLayout());
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				sc.setMinSize(window.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		window = new Composite(sc, SWT.NONE);
		sc.setContent(window);

		GridLayout layout = new GridLayout(1, false);
		window.setLayout(layout);

		addIndexer = new Button(window, SWT.NONE);
		addIndexer.setVisible(false);
		addIndexer.setText("schedule index addition");

		// populate combo
		indexTypes = new HashSet<>();
		vcsTypes = new HashSet<>();
		// indexTypes = selectedIndexer.getKnownBackends();
		// vcsTypes = selectedIndexer.getKnownVCSMonitorTypes();
		// modelTypes = hawk.getKnownModelParserTypes();

		MouseListener listener = new MouseListener() {

			@Override
			public void mouseUp(MouseEvent e) {

				if (e.getSource().equals(addIndexer)) {
					sysout("scheduling indexer addition");
					addIndexer(true);
					if (addNew) {
						sysout("indexer addition scheduled");
						addNew = false;
					} else
						sysout("adding of indexer aborted");

				} else if (e.getSource().equals(start)) {
					StartDialog s = new StartDialog(window.getShell(), view);
					s.setBlockOnOpen(true);
					s.create();
					s.open();
					if (adminPw != null) {
						start();
						start.setVisible(false);
					}
				}
			}

			@Override
			public void mouseDown(MouseEvent e) {
			}

			@Override
			public void mouseDoubleClick(MouseEvent e) {
			}
		};

		addIndexer.addMouseListener(listener);

		SelectionListener l = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedIndexer = getIndexerWithName(selectIndexer
						.getItem(selectIndexer.getSelectionIndex()));

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				selectedIndexer = getIndexerWithName(selectIndexer
						.getItem(selectIndexer.getSelectionIndex()));
			}
		};

		GridData data = new GridData(GridData.FILL_HORIZONTAL);

		text = new StyledText(window, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
		text.setLayoutData(data);
		text.setText("Chose an indexer:");
		text.setEnabled(false);
		text.setVisible(false);

		selectIndexer = new Combo(window, SWT.READ_ONLY);
		selectIndexer.addSelectionListener(l);
		selectIndexer.setLayoutData(data);
		selectIndexer.setEnabled(false);

		makeActions();
		hookContextMenu();
		// hookDoubleClickAction();
		contributeToActionBars();

		text2 = new StyledText(window, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
		text2.setLayoutData(data);
		text2.setText("");
		text2.setEnabled(true);
		text2.setVisible(true);

		start = new Button(window, SWT.NONE);
		start.setText("START");
		start.addMouseListener(listener);

		configmanager = new HawkOSGIConfigManager(this);

	}

	private void addIndexer(boolean interactive) {

		if (interactive) {

			AddIndexerDialog unpw = new AddIndexerDialog(window.getShell(),
					view);
			// WizardDialog unpw = new WizardDialog(window.getShell(),
			// new VCSNoSQLWizardClass());

			unpw.setBlockOnOpen(true);

			unpw.create();

			int result = unpw.open();

			if (result == Window.OK && addNew) {
				add();
			}
		} else {
			add();
		}

	}

	private void add() {

		try {

			IModelIndexer m = new ModelIndexerImpl(indexerName, parentfolder,
					myConsole);

			// create the indexer with relevant database
			IGraphDatabase db = configmanager.createGraph(indexType);
			db.run(indexName, parentfolder, myConsole);
			m.setDB(db);

			m.setMetaModelUpdater(metaModelUpdater);

			for (int i = 0; i < vcsType.size(); i++) {

				IVcsManager mo = configmanager.createVCSManager(vcsType.get(i));
				mo.run(vcsLoc.get(i), vcsUn.get(i), vcsPw.get(i), myConsole);
				m.addVCSManager(mo);
			}

			for (String mmr : metamodelparsers.keySet())
				m.addMetaModelResourceFactory(metamodelparsers.get(mmr));
			for (String mr : modelparsers.keySet())
				m.addModelResourceFactory(modelparsers.get(mr));
			for (String q : knownQueryLanguages.keySet())
				m.addQueryEngine(knownQueryLanguages.get(q));
			for (IModelUpdater u : updaters)
				m.addModelUpdater(u);

			m.init(adminPw);
			indexers.put(m.getName(), m);

			selectIndexer.add(indexerName);
			// selectedIndexer = getIndexerWithName(indexerName);

			sysout("indexer added");

		} catch (Exception e) {
			syserr("Exception in trying to add new Indexer:");
			syserr(e.getMessage());
			e.printStackTrace();
			syserr("Adding of indexer aborted, please try again");
			// e.printStackTrace();
		}

		vcsUn = new ArrayList<>();
		vcsPw = new ArrayList<>();
		vcsLoc = new ArrayList<>();
		vcsType = new ArrayList<>();
		indexerName = "";
		indexName = "";
		indexType = "";

	}

	protected IModelIndexer getIndexerWithName(String item) {
		IModelIndexer ret = indexers.get(item);
		if (ret == null) {
			System.err.println("null getindexerwithname: " + item);
			System.err.println("indexer names: ");
			System.err.println(indexers.keySet());
		}
		return ret;
	}

	private void start() {

		text2.setVisible(false);
		addIndexer.setVisible(true);
		selectIndexer.setEnabled(true);

		text.setVisible(true);

		init();

	}

	private void init() {

		// load any saved indexers
		//

		try {

			if (!metadata.exists())
				metadata.createNewFile();

			BufferedReader r = new BufferedReader(new FileReader(metadata));
			int add = 0;
			String indexer;
			while ((indexer = r.readLine()) != null) {
				String[] parse = indexer.split("\t");
				if (parse.length == 4) {
					// for(String s:parse)
					// System.err.println(s);
					try {
						add++;

						indexerName = parse[0];
						indexName = parse[1];
						indexType = parse[2];
						String monitormetadata = parse[3];
						String[] vcss = monitormetadata.split(":;:");
						for (int i = 0; i < vcss.length; i++) {
							String[] vcs = vcss[i].split(";:;");
							vcsLoc.add(vcs[0]);
							vcsType.add(vcs[1]);
							vcsUn.add(vcs[2]);
							vcsPw.add(vcs[3]);
						}

						addIndexer(false);

						myConsole.println("indexer '" + parse[0] + "' loaded");
					} catch (Exception e) {
						e.printStackTrace();
						myConsole
								.printerrln("model index failed to load, did you type the incorrect admin password?");
						myConsole.printerrln("vcs decrypted username: "
								+ SecurityManager.decrypt(parse[4], adminPw)
								+ "\nvcs decrypted password: "
								+ SecurityManager.decrypt(parse[5], adminPw));
					}
				} else
					myConsole
							.printerrln("non well-formed line in metadata file, ignoring it");
			}

			r.close();

			myConsole.println(add + " indexers loaded successfully");

		} catch (Exception e) {
			myConsole
					.printerrln("error in loading metadata file for saved indexers, none initialised");
		}

	}

	public static void sysout(final String s) {

		if (Display.getDefault() != null && s != null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					// if (text != null && !text.isDisposed()
					// && text.getText() != null) {
					// text.append(s + "\n");
					// text.setSelection(text.getCharCount());
					if (myConsole != null) {
						myConsole.println(s);
					} else
						System.err.println(s);
				}
			});
		} else
			System.err.println(s);
	}

	public static void sysoutp(final String s) {

		if (Display.getDefault() != null && s != null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					// if (text != null && !text.isDisposed()
					// && text.getText() != null) {
					// text.append(s);
					// text.setSelection(text.getCharCount());
					if (myConsole != null) {
						myConsole.print(s);
					} else
						System.err.print(s);
				}
			});
		} else
			System.err.println(s);
	}

	public static void syserr(final String s) {

		if (Display.getDefault() != null && s != null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					// if (text != null && !text.isDisposed()
					// && text.getText() != null) {
					// // text.setForeground(parent.getShell().getDisplay()
					// // .getSystemColor(SWT.COLOR_RED));
					// StyleRange styleRange = new StyleRange(text.getText()
					// .length(), s.length(), window.getShell()
					// .getDisplay().getSystemColor(SWT.COLOR_RED),
					// window.getShell().getDisplay()
					// .getSystemColor(SWT.COLOR_BLACK));
					// text.append(s + "\n");
					// text.setStyleRange(styleRange);
					// text.setSelection(text.getCharCount());//
					// // text.setForeground(parent.getShell().getDisplay()
					// // .getSystemColor(SWT.COLOR_WHITE));
					if (myConsole != null) {
						myConsole.printerrln(s);
					} else
						System.err.println(s);

				}
			});
		} else
			System.err.println(s);

	}

	@Override
	public void setFocus() {
		// viewer.getControl().setFocus();
	}

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return new Object[] { window };
		}
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		public String getColumnText(Object obj, int index) {
			if (obj instanceof StyledText)
				return ((StyledText) obj).getText();
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return null;
			// return getImage(obj);
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	public HawkUIEclipseViewImpl() {

	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		// manager.add(action1);
		// manager.add(new Separator());
		// manager.add(action2);
		// manager.add(new Separator());
		// manager.add(action3);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(action4);
		manager.add(action5);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(action1);
		manager.add(action2);
		manager.add(action4);
		manager.add(action5);
	}

	private void makeActions() {

		action1 = new Action() {
			public void run() {

				for (String s : indexers.keySet()) {
					IModelIndexer m = indexers.get(s);
					m.resetScheduler();
				}

				sysout("timers restarted");

			}
		};
		action1.setText("RST");
		action1.setToolTipText("Restarts the synchronisation timer (to 1 second)");
		action1.setImageDescriptor(Activator.getDefault().getImageDescriptor(
				"icons/time.png"));

		action2 = new Action() {
			public void run() {

				if (selectedIndexer != null) {
					try {
						selectedIndexer.registerMetamodel((File[]) null);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else
					syserr("no indexer selected!");
				// showMessage("Action 2 executed");
			}
		};
		action2.setText("Register-MM");
		action2.setToolTipText("Registers a set of metamodel files");
		action2.setImageDescriptor(Activator.getDefault().getImageDescriptor(
				"icons/mm.png"));

		// action3 = new Action() {
		// public void run() {
		//
		// try {
		// selectedIndexer.runGrabatsAdvanced();
		// } catch (Exception e) {
		// // System.err.println(e);
		// // syserr(e.toString());
		// if (e instanceof java.lang.NullPointerException)
		// syserr("no indexer selected!");
		// else
		// syserr(e.toString());
		// }
		//
		// // showMessage("Action 3 executed");
		// }
		// };
		// action3.setText("G-Advanced");
		// action3.setToolTipText("Executes the grabats query using advanced (context-specific & native) querying");
		// action3.setImageDescriptor(Activator.getDefault().getImageDescriptor(
		// "icons/g-a.gif"));

		action4 = new Action() {
			public void run() {

				try {
					syserr("runeol deprecated -- sorry!");
					myConsole.println("runeol deprecated -- sorry!");
					// selectedIndexer.runEOL();
				} catch (Exception e) {
					// System.err.println(e);
					// syserr(e.toString());
					if (e instanceof java.lang.NullPointerException)
						syserr("no indexer selected!");
					else
						syserr(e.toString());
				}

				// showMessage("Action 3 executed");
			}
		};
		action4.setText("EOL");
		action4.setToolTipText("Executes an EOL query");
		action4.setImageDescriptor(Activator.getDefault().getImageDescriptor(
				"icons/eol.png"));

		action5 = new Action() {
			public void run() {

				try {
					selectedIndexer.logFullStore();
				} catch (Exception e) {
					// System.err.println(e);
					// syserr(e.toString());
					if (e instanceof java.lang.NullPointerException)
						syserr("no indexer selected!");
					else
						syserr(e.toString());
				}

				// showMessage("Action 5 executed");
			}
		};
		action5.setText("Log");
		action5.setToolTipText("Logs the entire selected store store to file");
		action5.setImageDescriptor(Activator.getDefault().getImageDescriptor(
				"icons/log.gif"));

	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				HawkUIEclipseViewImpl.this.fillContextMenu(manager);
			}
		});
		// Menu menu = menuMgr.createContextMenu(viewer.getControl());
		// viewer.getControl().setMenu(menu);
		// getSite().registerContextMenu(menuMgr, viewer);
	}

	public void setVCSLoc(String loc2) {
		vcsLoc.add(loc2);

	}

	public void setVCSType(String type) {
		vcsType.add(type);

	}

	public void setIndexerName(String index) {
		indexerName = index;
	}

	public void setIndexName(String index) {
		indexName = index;
	}

	public void setIndexType(String type) {
		indexType = type;

	}

	public void setAddNew(boolean addNew) {
		this.addNew = addNew;
	}

	public void shutdown() throws Exception {
		metadata.delete();
		metadata.createNewFile();
		for (String key : indexers.keySet()) {
			IModelIndexer m = indexers.get(key);
			m.shutdown(metadata, false);
		}
		indexers = new HashMap<>();
	}

	public void setAdminPw(char[] charArray) {
		adminPw = charArray;

	}

	@Override
	public void addMetaModelParser(IMetaModelResourceFactory parser) {
		metamodelparsers.put(parser.getType(), parser);
	}

	@Override
	public void addModelParser(IModelResourceFactory parser) {
		modelparsers.put(parser.getType(), parser);
	}

	@Override
	public void addUpdater(IModelUpdater up) {
		updaters.add(up);
	}

	@Override
	public void addQueryLanguage(IQueryEngine q) {
		knownQueryLanguages.put(q.getType(), q);
	}

	@Override
	public void setKnownVCSManagerTypes(Set<String> knownvcsmonitors) {
		vcsTypes = knownvcsmonitors;
	}

	@Override
	public void setKnownBackends(Set<String> knownbackends) {
		indexTypes = knownbackends;
	}

	@Override
	public void addMetaModelUpdater(IMetaModelUpdater metaModelUpdater) {
		this.metaModelUpdater = metaModelUpdater;
	}

}
