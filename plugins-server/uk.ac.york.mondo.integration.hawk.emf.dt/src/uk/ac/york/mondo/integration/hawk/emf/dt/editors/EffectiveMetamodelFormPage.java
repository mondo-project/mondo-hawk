/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.hawk.emf.dt.editors;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.thrift.TException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

import com.google.common.collect.ImmutableSet;

import uk.ac.york.mondo.integration.api.EffectiveMetamodelRuleset;
import uk.ac.york.mondo.integration.api.Hawk;
import uk.ac.york.mondo.integration.api.HawkQueryOptions;
import uk.ac.york.mondo.integration.api.ModelElementType;
import uk.ac.york.mondo.integration.api.QueryResult;
import uk.ac.york.mondo.integration.api.SlotMetadata;
import uk.ac.york.mondo.integration.hawk.emf.HawkModelDescriptor;
import uk.ac.york.mondo.integration.hawk.emf.dt.Activator;
import uk.ac.york.mondo.integration.hawk.emf.dt.importers.EMMImporter;
import uk.ac.york.mondo.integration.hawk.emf.impl.HawkResourceImpl;

public class EffectiveMetamodelFormPage extends FormPage {

	private static final String EMM_IMPORTER_EXTID = "uk.ac.york.mondo.integration.hawk.emf.dt.emmImporter";

	private final class ImportEMMSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent event) {
			IConfigurationElement[] importerElements = Platform.getExtensionRegistry()
					.getConfigurationElementsFor(EMM_IMPORTER_EXTID);

			// We only give the choice to the user if there are 2+ importers
			Object[] selected = importerElements;
			if (importerElements.length > 1) {
				final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				final ListDialog lstDialog = new ListDialog(shell);
				lstDialog.setContentProvider(new ArrayContentProvider());
				lstDialog.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return ((IConfigurationElement) element).getDeclaringExtension().getLabel();
					}
				});
				lstDialog.setInput(importerElements);
				lstDialog.setTitle("Select an importer");
				if (lstDialog.open() == Window.OK) {
					selected = lstDialog.getResult();
				} else {
					selected = new Object[0];
				}
			}

			if (selected.length > 0) {
				final IConfigurationElement selectedCE = (IConfigurationElement) selected[0];
				try {
					final EMMImporter importer = (EMMImporter) selectedCE.createExecutableExtension("class");
					importer.importEffectiveMetamodelInto(store);
					setEffectiveMetamodel(store);
					treeViewer.refresh();
					getEditor().setDirty(true);
				} catch (CoreException ex) {
					Activator.getDefault().logError(ex);
				}
			}
		}
	}

	private final class SetStateSelectionListener extends SelectionAdapter {
		private final MetamodelEditingSupport editingSupport;
		private final RowState newState;

		private SetStateSelectionListener(MetamodelEditingSupport editingSupport, RowState newState) {
			this.newState = newState;
			this.editingSupport = editingSupport;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			final ISelection selection = treeViewer.getSelection();
			if (selection instanceof IStructuredSelection) {
				final IStructuredSelection ssel = (IStructuredSelection)selection;
				for (final Iterator<?> it = ssel.iterator(); it.hasNext(); ) {
					Object element = it.next();
					editingSupport.setValue(element, newState.ordinal());
				}
			}
		}
	}

	private final class MetamodelCellLabelProvider extends CellLabelProvider {
		private final MetamodelEditingSupport editingSupport;
		private final int iColumn;

		private MetamodelCellLabelProvider(MetamodelEditingSupport editingSupport, int iColumn) {
			this.editingSupport = editingSupport;
			this.iColumn = iColumn;
		}

		@Override
		public void update(ViewerCell cell) {
			final Node element = (Node)cell.getElement();
			final RowState state = editingSupport.getState(element);
			if (iColumn == 0) {
				cell.setText(((Node)element).label);
			} else {
				cell.setText("" + state.getLabel());
			}

			final RowState inheritedState = getInheritedState(element, state);
			setBackgroundColor(cell, inheritedState);
		}

		private RowState getInheritedState(Node element, RowState state) {
			switch (state) {
			case INCLUDED:
			case EXCLUDED:
				return state;
			default:
				if (element.parent == null) {
					return RowState.DEFAULT;
				} else {
					return getInheritedState(element.parent, editingSupport.getState(element.parent));
				}
			}
		}

		private void setBackgroundColor(ViewerCell cell, final RowState state) {
			switch (state) {
			case INCLUDED:
				cell.setBackground(clrIncludes);
				break;
			case EXCLUDED:
				cell.setBackground(clrExcludes);
				break;
			default:
				if (cell.getElement() instanceof MetamodelNode) {
					final MetamodelNode mn = (MetamodelNode)cell.getElement();
					if (store.getInclusionRules().containsRow(mn.label) || store.getExclusionRules().containsRow(mn.label)) {
						cell.setBackground(clrPartial);
					} else {
						cell.setBackground(null);
					}
				} else if (cell.getElement() instanceof TypeNode) {
					final TypeNode tn = (TypeNode)cell.getElement();
					if (store.getInclusionRules().contains(tn.parent.label, tn.label) || store.getExclusionRules().contains(tn.parent.label, tn.label)) {
						cell.setBackground(clrPartial);
					} else {
						cell.setBackground(null);
					}
				} else {
					cell.setBackground(null);
				}
				break;
			}
		}
	}

	private static enum RowState {
		DEFAULT ("Default"), EXCLUDED ("Excluded"), INCLUDED ("Included");
		private final String label;

		RowState(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		public static String[] getLabels() {
			final List<String> labels = new ArrayList<>();
			for (RowState s : RowState.values()) {
				labels.add(s.label);
			}
			return labels.toArray(new String[labels.size()]);
		}
	}

	private final class MetamodelEditingSupport extends EditingSupport {

		private MetamodelEditingSupport(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			final TreeViewer viewer = (TreeViewer)getViewer();
			return new ComboBoxCellEditor(viewer.getTree(), RowState.getLabels());
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		public Object getValue(Object element) {
			final RowState state = getState(element);
			return state == null ? 0 : state.ordinal();
		}

		protected RowState getState(Object element) {
			RowState state = null;
			if (element instanceof MetamodelNode) {
				final MetamodelNode mn = (MetamodelNode)element;
				if (store.getInclusionRules().contains(mn.label, EffectiveMetamodelRuleset.WILDCARD)) {
					state = RowState.INCLUDED;
				} else if (store.getExclusionRules().contains(mn.label, EffectiveMetamodelRuleset.WILDCARD)) {
					state = RowState.EXCLUDED;
				} else {
					state = RowState.DEFAULT;
				}
			} else if (element instanceof TypeNode) {
				final TypeNode tn = (TypeNode)element;
				final MetamodelNode mn = (MetamodelNode) tn.parent;
				final ImmutableSet<String> includedSlots = store.getInclusionRules().get(mn.label, tn.label);
				if (includedSlots != null && includedSlots.contains(EffectiveMetamodelRuleset.WILDCARD)) {
					state = RowState.INCLUDED;
				}
				else {
					final ImmutableSet<String> excludedSlots = store.getExclusionRules().get(mn.label, tn.label);
					if (excludedSlots != null && excludedSlots.contains(EffectiveMetamodelRuleset.WILDCARD)) {
						state = RowState.EXCLUDED;
					} else {
						state = RowState.DEFAULT;
					}
				}
			} else if (element instanceof SlotNode) {
				final SlotNode sn = (SlotNode)element;
				final TypeNode tn = (TypeNode)sn.parent;
				final MetamodelNode mn = (MetamodelNode)tn.parent;
				final ImmutableSet<String> includedSlots = store.getInclusionRules().get(mn.label, tn.label);
				if (includedSlots != null && includedSlots.contains(sn.label)) {
					state = RowState.INCLUDED;
				}
				else {
					final ImmutableSet<String> excludedSlots = store.getExclusionRules().get(mn.label, tn.label);
					if (excludedSlots != null && excludedSlots.contains(sn.label)) {
						state = RowState.EXCLUDED;
					} else {
						state = RowState.DEFAULT;
					}
				}
			}
			return state;
		}

		@Override
		protected void setValue(Object element, Object value) {
			final RowState oldValue = RowState.values()[(Integer) getValue(element)];
			final RowState newValue = RowState.values()[(Integer)value];
			if (oldValue.equals(newValue)) {
				return;
			}

			final ColumnViewer v = getViewer();
			if (element instanceof MetamodelNode) {
				MetamodelNode mn = (MetamodelNode)element;
				store.reset(mn.label);

				switch (newValue) {
				case INCLUDED:
					store.include(mn.label);
					break;
				case EXCLUDED:
					store.exclude(mn.label);
					break;
				default:
					break;
				}

				v.update(mn, null);
				for (Node tn : mn.children) {
					v.update(tn, null);
					for (Node sn : tn.children) {
						v.update(sn, null);
					}
				}
			} else if (element instanceof TypeNode) {
				TypeNode tn = (TypeNode)element;

				store.reset(tn.parent.label, tn.label);
				switch (newValue) {
				case INCLUDED:
					store.include(tn.parent.label, tn.label);
					break;
				case EXCLUDED:
					store.exclude(tn.parent.label, tn.label);
					break;
				default:
					break;
				}

				v.update(tn.parent, null);
				v.update(tn, null);
				for (Node sn : tn.children) {
					v.update(sn, null);
				}
			} else if (element instanceof SlotNode) {
				final SlotNode sn = (SlotNode) element;
				final Node tn = sn.parent;

				switch (oldValue) {
				case INCLUDED: {
						final ImmutableSet<String> oldSlots = store.getInclusionRules().get(tn.parent.label, tn.label);
						final ImmutableSet<String> newSlots = removeSlot(sn, oldSlots);
						if (newSlots != null) {
							store.include(tn.parent.label, tn.label, newSlots);
						}
					}
					break;
				case EXCLUDED: {
						final ImmutableSet<String> oldSlots = store.getExclusionRules().get(tn.parent.label, tn.label);
						final ImmutableSet<String> newSlots = removeSlot(sn, oldSlots);
						if (newSlots != null) {
							store.exclude(tn.parent.label, tn.label, newSlots);
						}
					}
					break;
				default:
					break;
				}

				switch (newValue) {
				case INCLUDED: {
					final ImmutableSet<String> oldSlots = store.getInclusionRules().get(tn.parent.label, tn.label);
					final ImmutableSet<String> newSlots = addSlot(sn, oldSlots);
					if (newSlots != null) {
						store.include(tn.parent.label, tn.label, newSlots);
					}
					break;
				}
				case EXCLUDED: {
					final ImmutableSet<String> oldSlots = store.getExclusionRules().get(tn.parent.label, tn.label);
					final ImmutableSet<String> newSlots = addSlot(sn, oldSlots);
					if (newSlots != null) {
						store.exclude(tn.parent.label, tn.label, newSlots);
					}
					break;
				}
				default:
					break;
				}

				v.update(sn.parent.parent, null);
				v.update(sn.parent, null);
				v.update(sn, null);
			}

			getEditor().setDirty(true);
		}

		protected ImmutableSet<String> addSlot(final SlotNode sn, ImmutableSet<String> oldSlots) {
			ImmutableSet<String> newSlots = oldSlots;
			if (oldSlots == null) {
				return ImmutableSet.of(sn.label);
			} else if ( !oldSlots.contains(EffectiveMetamodelRuleset.WILDCARD) && !oldSlots.contains(sn.label)) {
				ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<String>();
				builder.addAll(oldSlots);
				builder.add(sn.label);
				newSlots = builder.build();
			}
			return newSlots;
		}

		protected ImmutableSet<String> removeSlot(final SlotNode sn, ImmutableSet<String> oldSlots) {
			ImmutableSet<String> newSlots = oldSlots;
			if (oldSlots != null && !oldSlots.contains(EffectiveMetamodelRuleset.WILDCARD) && oldSlots.contains(sn.label)) {
				ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<String>();
				for (String s : oldSlots) {
					if (!sn.label.equals(s)) {
						builder.add(s);
					}
				}
				newSlots = builder.build();
			}
			return newSlots;
		}
	}

	private EffectiveMetamodelRuleset store = new EffectiveMetamodelRuleset();
	private TreeViewer treeViewer;
	private Color clrExcludes, clrIncludes, clrPartial;

	private abstract static class Node implements Comparable<Node> {
		public final Node parent;
		public final List<Node> children = new ArrayList<>();
		public final String label;

		public Node(Node parent, String label) {
			this.parent = parent;
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}

		@Override
		public int compareTo(Node o) {
			return label.compareTo(o.label);
		}
	}

	private static class MetamodelNode extends Node {
		public MetamodelNode(Node parent, String mmURI) {
			super(parent, mmURI);
		}
	}

	private static class TypeNode extends Node {
		public TypeNode(Node parent, String name) {
			super(parent, name);
		}
	}

	private static class SlotNode extends Node {
		public SlotNode(Node parent, String name) {
			super(parent, name);
		}
	}

	private final class MetamodelContentProvider implements ITreeContentProvider {
		protected Object[] roots = null;

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != null) {
				computeRoots();
			}
		}

		protected void computeRoots() {
			if (roots == null) {
				final HawkMultiPageEditor editor = getEditor();
				try {
					final HawkModelDescriptor descriptor = editor.buildDescriptor();
					final Hawk.Client client = editor.connectToHawk(descriptor);
					final List<QueryResult> results = client.query(descriptor.getHawkInstance(),
						"return Model.types;", HawkResourceImpl.EOL_QUERY_LANG,
						new HawkQueryOptions());

					final Map<String, MetamodelNode> mmNodes = new TreeMap<>();
					for (QueryResult qr : results) {
						if (qr.isSetVModelElementType()) {
							final ModelElementType met = qr.getVModelElementType();
							MetamodelNode mn = mmNodes.get(met.getMetamodelUri());
							if (mn == null) {
								mn = new MetamodelNode(null, met.getMetamodelUri());
								mmNodes.put(met.getMetamodelUri(), mn);
							}

							final TypeNode tn = new TypeNode(mn, met.getTypeName());
							mn.children.add(tn);
							if (met.isSetAttributes()) {
								for (SlotMetadata attr : met.getAttributes()) {
									final SlotNode sn = new SlotNode(tn, attr.getName());
									tn.children.add(sn);
								}
							}
							if (met.isSetReferences()) {
								for (SlotMetadata ref : met.getReferences()) {
									final SlotNode sn = new SlotNode(tn, ref.getName());
									tn.children.add(sn);
								}
							}
							Collections.sort(tn.children);
						}
					}

					roots = new ArrayList<>(mmNodes.values()).toArray();
					for (Node root : mmNodes.values()) {
						Collections.sort(root.children);
					}
				} catch (TException | URISyntaxException e) {
					Activator.getDefault().logError(e);
				}
			}
		}

		@Override
		public void dispose() {
			roots = null;
			clrExcludes.dispose();
			clrIncludes.dispose();
			clrPartial.dispose();
		}

		@Override
		public Object getParent(Object element) {
			return ((Node)element).parent;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			computeRoots();
			if (roots == null) {
				return new Object[0];
			} else {
				return roots;
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((Node)parentElement).children.toArray();
		}

		@Override
		public boolean hasChildren(Object element) {
			return !((Node)element).children.isEmpty();
		}
	}

	public EffectiveMetamodelFormPage(HawkMultiPageEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	public HawkMultiPageEditor getEditor() {
		return (HawkMultiPageEditor) super.getEditor();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().setText("Effective Metamodel");

		clrIncludes = new Color(managedForm.getForm().getDisplay(), new RGB(180, 255, 255));
		clrExcludes = new Color(managedForm.getForm().getDisplay(), new RGB(255, 200, 255));
		clrPartial = new Color(managedForm.getForm().getDisplay(), new RGB(255, 255, 220));
		
		final FormToolkit toolkit = managedForm.getToolkit();
		final TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 1;
		final Composite formBody = managedForm.getForm().getBody();
		formBody.setLayout(layout);

		final FormText formText = toolkit.createFormText(formBody, true);
		formText.setText(
				"<p>"
				+ "<p>This page allows for limiting the types and slots that should be retrieved through the Hawk API.</p>"
				+ "<p>With everything set to 'Default', all metamodels, types and slots are retrieved.</p>"
				+ "<p>With everything set to 'Default' or 'Includes', only the included elements are retrieved.</p>"
				+ "<p>With everything set to 'Default' or 'Excludes', everything but the excluded elements is retrieved.</p>"
				+ "<p>Using all three values, only the elements which are 1. included and 2. not excluded are retrieved.</p>"
				+ "<p>The shown metamodels are those registered in the Hawk server: please make sure the Instance section of the descriptor has been setup correctly before using this page.</p>"
				+ "</p>",
				true, false);

		final Composite cTable = toolkit.createComposite(formBody, SWT.NONE);
		cTable.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB));
		cTable.setLayout(Utils.createTableWrapLayout(2));

		final Composite cTree = new Composite(cTable, SWT.NONE);
		final TableWrapData cTreeLayoutData = new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB);
		cTreeLayoutData.maxHeight = 500;
		cTree.setLayoutData(cTreeLayoutData);
		TreeColumnLayout tcl_cTree = new TreeColumnLayout();
		cTree.setLayout(tcl_cTree);

		treeViewer = new TreeViewer(cTree, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL);
		treeViewer.getTree().setHeaderVisible(true);
		final MetamodelContentProvider contentProvider = new MetamodelContentProvider();
		treeViewer.setUseHashlookup(true);
		treeViewer.setContentProvider(contentProvider);

		final MetamodelEditingSupport editingSupport = new MetamodelEditingSupport(treeViewer);
    	final TreeViewerColumn labelColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
    	labelColumn.getColumn().setText("Element");
		labelColumn.setLabelProvider(new MetamodelCellLabelProvider(editingSupport, 0));
		tcl_cTree.setColumnData(labelColumn.getColumn(), new ColumnWeightData(100, 0, true));

		final TreeViewerColumn stateColumn = new TreeViewerColumn(treeViewer, SWT.NONE);
		stateColumn.getColumn().setText("State");
		stateColumn.setLabelProvider(new MetamodelCellLabelProvider(editingSupport, 1));
		stateColumn.setEditingSupport(editingSupport);
		tcl_cTree.setColumnData(stateColumn.getColumn(), new ColumnPixelData(100));

		final Composite cButtons = toolkit.createComposite(cTable, SWT.WRAP);
		final FillLayout cButtonsLayout = new FillLayout(SWT.VERTICAL);
		cButtonsLayout.spacing = 7;
		cButtonsLayout.marginWidth = 3;
		cButtons.setLayout(cButtonsLayout);

		final Button btnIncludeAll = new Button(cButtons, SWT.NONE);
		btnIncludeAll.setText("Include all");
		btnIncludeAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEffectiveMetamodel(new EffectiveMetamodelRuleset());
				treeViewer.refresh();
				getEditor().setDirty(true);
			}
		});

		final Button btnExcludeAll = new Button(cButtons, SWT.NONE);
		btnExcludeAll.setText("Exclude all");
		btnExcludeAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final EffectiveMetamodelRuleset ruleset = new EffectiveMetamodelRuleset();
				for (Object on : contentProvider.roots) {
					final MetamodelNode mn = (MetamodelNode)on;
					ruleset.exclude(mn.label);
				}
				setEffectiveMetamodel(ruleset);
				treeViewer.refresh();
				getEditor().setDirty(true);
			}
		});

		final Button btnExclude = new Button(cButtons, SWT.NONE);
		btnExclude.setText("Exclude");
		btnExclude.addSelectionListener(new SetStateSelectionListener(editingSupport, RowState.EXCLUDED));
		btnExclude.setEnabled(false);

		final Button btnInclude = new Button(cButtons, SWT.NONE);
		btnInclude.setText("Include");
		btnInclude.addSelectionListener(new SetStateSelectionListener(editingSupport, RowState.INCLUDED));
		btnInclude.setEnabled(false);

		final Button btnReset = new Button(cButtons, SWT.NONE);
		btnReset.setText("Reset");
		btnReset.addSelectionListener(new SetStateSelectionListener(editingSupport, RowState.DEFAULT));
		btnReset.setEnabled(false);

		final Button btnImport = new Button(cButtons, SWT.NONE);
		btnImport.setText("Import...");
		btnImport.addSelectionListener(new ImportEMMSelectionListener());

		treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final boolean isAnySelected = !event.getSelection().isEmpty();
				btnExclude.setEnabled(isAnySelected);
				btnInclude.setEnabled(isAnySelected);
				btnReset.setEnabled(isAnySelected);
			}
		});

		treeViewer.setInput(store);
		treeViewer.expandToLevel(2);
	}

	public EffectiveMetamodelRuleset getEffectiveMetamodel() {
		return store;
	}

	public void setEffectiveMetamodel(EffectiveMetamodelRuleset newStore) {
		this.store = newStore;
		if (treeViewer != null) {
			treeViewer.setInput(this.store);
		}
	}
}
