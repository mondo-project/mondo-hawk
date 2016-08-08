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
package org.hawk.service.emf.dt.editors;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.epsilon.common.dt.util.ListContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.HawkInstance;
import org.hawk.service.api.HawkState;
import org.hawk.service.api.Repository;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.emf.HawkModelDescriptor;
import org.hawk.service.emf.HawkModelDescriptor.LoadingMode;
import org.hawk.service.emf.dt.Activator;
import org.hawk.service.emf.dt.editors.fields.FormCheckBoxField;
import org.hawk.service.emf.dt.editors.fields.FormComboBoxField;
import org.hawk.service.emf.dt.editors.fields.FormSection;
import org.hawk.service.emf.dt.editors.fields.FormTextField;
import org.hawk.service.emf.impl.HawkResourceFactoryImpl;

class DetailsFormPage extends FormPage {

	static abstract class ContentSection extends FormSection implements ModifyListener, SelectionListener {
		private final FormTextField fldFilePatterns;
		private final FormTextField fldRepositoryURL;
		private final FormComboBoxField fldLoadingMode;
		private final FormTextField fldQueryLanguage;
		private final FormTextField fldQuery;
		private final FormTextField fldDefaultNamespaces;
		private final FormCheckBoxField fldSplit;
		private final FormTextField fldPaged;

		public ContentSection(FormToolkit toolkit, Composite parent) {
			super(toolkit, parent, "Contents", "Filters on the contents of the index to be read as a model");
		    cContents.setLayout(Utils.createTableWrapLayout(2));

		    this.fldRepositoryURL = new FormTextField(toolkit, cContents, "<a href=\"selectRepository\">Repository URL</a>:", HawkModelDescriptor.DEFAULT_REPOSITORY);
		    this.fldFilePatterns = new FormTextField(toolkit, cContents, "<a href=\"selectFiles\">File pattern(s)</a>:", HawkModelDescriptor.DEFAULT_FILES);
		    this.fldLoadingMode = new FormComboBoxField(toolkit, cContents, "Loading mode:", HawkModelDescriptor.LoadingMode.strings());
		    this.fldQueryLanguage = new FormTextField(toolkit, cContents, "<a href=\"selectQueryLanguage\">Query language</a>:", HawkModelDescriptor.DEFAULT_QUERY_LANGUAGE);
		    this.fldQuery = new FormTextField(toolkit, cContents, "Query:", HawkModelDescriptor.DEFAULT_QUERY);
		    this.fldDefaultNamespaces = new FormTextField(toolkit, cContents, "Default namespaces:", HawkModelDescriptor.DEFAULT_DEFAULT_NAMESPACES);
		    this.fldSplit = new FormCheckBoxField(toolkit, cContents, "Split by file:", HawkModelDescriptor.DEFAULT_IS_SPLIT);
		    this.fldPaged = new FormTextField(toolkit, cContents, "Page size for initial load:", HawkModelDescriptor.DEFAULT_PAGE_SIZE + "");

		    this.fldRepositoryURL.getText().setToolTipText(
		        "Pattern for the URL repositories to be fetched (* means 0+ arbitrary characters).");
		    this.fldFilePatterns.getText().setToolTipText(
		        "Comma-separated patterns for the files repositories to be fetched (* means 0+ arbitrary characters).");
		    this.fldQueryLanguage.getText().setToolTipText(
		        "Language in which the query will be written. If empty, the entire model will be retrieved.");
		    this.fldQuery.getText().setToolTipText(
		        "Query to be used for the initial contents of the model. If empty, the entire model will be retrieved.");
		    this.fldDefaultNamespaces.getText().setToolTipText(
			"Comma-separated list of namespaces used to disambiguate types if multiple matches are found.");
		    this.fldSplit.getCheck().setToolTipText(
		    	"If checked, the contents of the index will be split by file, using surrogate resources. Otherwise, the entire contents of the index will be under this resource (needed for CloudATL).");
		    this.fldPaged.getText().setToolTipText(
			"If set to a value > 0, the initial load will be done in two stages: 1 request for the node IDs + N requests for their contents (in batches). Recommended for very large models that cannot be sent in one go.");

		    fldRepositoryURL.getText().addModifyListener(this);
		    fldFilePatterns.getText().addModifyListener(this);
		    fldLoadingMode.getCombo().addSelectionListener(this);
		    fldQueryLanguage.getText().addModifyListener(this);
		    fldQuery.getText().addModifyListener(this);
		    fldDefaultNamespaces.getText().addModifyListener(this);
		    fldSplit.getCheck().addSelectionListener(this);
		    fldPaged.getText().addModifyListener(this);

		    final HyperlinkAdapter hyperlinkListener = new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					switch (e.getHref().toString()) {
					case "selectQueryLanguage":
						selectQueryLanguage();
						break;
					case "selectRepository":
						selectRepository();
						break;
					case "selectFiles":
						selectFiles();
						break;
					}
				}
			};
			this.fldQueryLanguage.getLabel().addHyperlinkListener(hyperlinkListener);
			this.fldRepositoryURL.getLabel().addHyperlinkListener(hyperlinkListener);
			this.fldFilePatterns.getLabel().addHyperlinkListener(hyperlinkListener);
		}

		public String[] getFilePatterns() {
			return fldFilePatterns.getText().getText().trim().split(",");
		}

		public String getRepositoryURL() {
			return fldRepositoryURL.getText().getText().trim();
		}

		public LoadingMode getLoadingMode() {
			return LoadingMode.values()[fldLoadingMode.getCombo().getSelectionIndex()];
		}

		public String getQueryLanguage() {
			return fldQueryLanguage.getText().getText().trim();
		}

		public String getQuery() {
			return fldQuery.getText().getText().trim();
		}

		public boolean isSplit() {
			return fldSplit.getCheck().getSelection();
		}

		public int getPageSize() {
			try {
				return Integer.valueOf(fldPaged.getText().getText());
			} catch (NumberFormatException ex) {
				return HawkModelDescriptor.DEFAULT_PAGE_SIZE;
			}
		}

		public String getDefaultNamespaces() {
			return fldDefaultNamespaces.getText().getText().trim();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == fldLoadingMode.getCombo()) {
				loadingModeChanged();
			} else if (e.widget == fldSplit.getCheck()) {
				splitChanged();
			}
		}

		@Override
		public void modifyText(ModifyEvent e) {
			if (e.widget == fldRepositoryURL.getText()) {
				repositoryURLChanged();
			} else if (e.widget == fldFilePatterns.getText()) {
				filePatternsChanged();
			} else if (e.widget == fldQueryLanguage.getText()) {
				queryLanguageChanged();
			} else if (e.widget == fldQuery.getText()) {
				queryChanged();
			} else if (e.widget == fldDefaultNamespaces.getText()) {
				defaultNamespacesChanged();
			} else if (e.widget == fldPaged.getText()) {
				pageSizeChanged();
			}
		}

		public void setFilePatterns(String... patterns) {
			fldFilePatterns.setTextWithoutListener(Utils.concat(patterns, ","), this);
		}

		public void setRepositoryURL(String url) {
			fldRepositoryURL.setTextWithoutListener(url, this);
		}

		public void setLoadingMode(LoadingMode lazy) {
			fldLoadingMode.getCombo().select(lazy.ordinal());
		}

		public void setQueryLanguage(String queryLanguage) {
			fldQueryLanguage.setTextWithoutListener(queryLanguage, this);
		}

		public void setQuery(String query) {
			fldQuery.setTextWithoutListener(query, this);
		}

		public void setSplit(boolean isSplit) {
			fldSplit.getCheck().setSelection(isSplit);
		}

		public void setPageSize(int size) {
			fldPaged.getText().setText(size + "");
		}

		public void setDefaultNamespaces(String defaultNS) {
			fldDefaultNamespaces.setTextWithoutListener(defaultNS, this);
		}

		protected abstract void filePatternsChanged();
		protected abstract void repositoryURLChanged();
		protected abstract void loadingModeChanged();
		protected abstract void queryLanguageChanged();
		protected abstract void queryChanged();
		protected abstract void defaultNamespacesChanged();
		protected abstract void splitChanged();
		protected abstract void pageSizeChanged();

		protected abstract void selectQueryLanguage();
		protected abstract void selectRepository();
		protected abstract void selectFiles();
	}

	static abstract class InstanceSection extends FormSection implements ModifyListener, SelectionListener {
		private final FormTextField fldInstanceName;
		private final FormTextField fldServerURL;
		private final FormComboBoxField fldTProtocol;
		private final FormTextField fldUsername;
		private final FormTextField fldPassword;

		public InstanceSection(FormToolkit toolkit, Composite parent) {
			super(toolkit, parent, "Instance", "Access details for the remote Hawk instance.");
		    cContents.setLayout(Utils.createTableWrapLayout(2));

		    this.fldServerURL = new FormTextField(toolkit, cContents, "Server URL:", "");
		    this.fldTProtocol = new FormComboBoxField(toolkit, cContents, "Thrift protocol:", ThriftProtocol.strings());
		    this.fldInstanceName = new FormTextField(toolkit, cContents, "<a href=\"selectInstance\">Instance name</a>:", "");
		    this.fldUsername = new FormTextField(toolkit, cContents, "Username:", "");
		    this.fldPassword = new FormTextField(toolkit, cContents, "Password:", "", SWT.BORDER | SWT.PASSWORD);

		    fldServerURL.getText().addModifyListener(this);
		    fldInstanceName.getText().addModifyListener(this);
		    fldTProtocol.getCombo().addSelectionListener(this);
		    fldUsername.getText().addModifyListener(this);
		    fldPassword.getText().addModifyListener(this);

		    fldUsername.getText().setToolTipText(
		    	"Username to be included in the .hawkmodel file, to log "
			+ "into the Hawk Thrift API. To use the Eclipse secure storage "
			+ "instead, keep blank.");
		    fldPassword.getText().setToolTipText(
			"Plaintext password to be included in the .hawkmodel file, to log "
			+ "into the Hawk Thrift API. To use the Eclipse secure storage "
			+ "instead, keep blank.");

		    fldInstanceName.getLabel().addHyperlinkListener(new HyperlinkAdapter() {
		    	public void linkActivated(HyperlinkEvent e) {
		    		switch (e.getHref().toString()) {
		    		case "selectInstance":
		    			selectInstance();
		    			break;
		    		}
		    	}
		    });
		}

		public String getInstanceName() {
			return fldInstanceName.getText().getText().trim();
		}

		public String getServerURL() {
			return fldServerURL.getText().getText().trim();
		}

		public ThriftProtocol getThriftProtocol() {
			return ThriftProtocol.values()[fldTProtocol.getCombo().getSelectionIndex()];
		}

		public void setInstanceName(String name) {
			fldInstanceName.setTextWithoutListener(name, this);
		}

		public void setServerURL(String url) {
			fldServerURL.setTextWithoutListener(url, this);
		}

		public void setThriftProtocol(ThriftProtocol t) {
			fldTProtocol.getCombo().select(t.ordinal());
		}

		public String getUsername() {
			return fldUsername.getText().getText().trim();
		}

		public void setUsername(String u) {
			fldUsername.setTextWithoutListener(u, this);
		}

		public String getPassword() {
			return fldPassword.getText().getText().trim();
		}

		public void setPassword(String p) {
			fldPassword.setTextWithoutListener(p, this);
		}

		@Override
		public void modifyText(ModifyEvent e) {
			if (e.widget == fldServerURL.getText()) {
				serverURLChanged();
			} else if (e.widget == fldInstanceName.getText()) {
				instanceNameChanged();
			} else if (e.widget == fldUsername.getText()) {
				usernameChanged();
			} else if (e.widget == fldPassword.getText()) {
				passwordChanged();
			}
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == fldTProtocol.getCombo()) {
				thriftProtocolChanged();
			}
		}

		protected abstract void instanceNameChanged();
		protected abstract void serverURLChanged();
		protected abstract void thriftProtocolChanged();
		protected abstract void usernameChanged();
		protected abstract void passwordChanged();
		protected abstract void selectInstance();
	}

	static abstract class SubscriptionSection extends FormSection implements SelectionListener, ModifyListener {
		private final FormCheckBoxField fldSubscribe;
		private final FormTextField fldClientID;
		private final FormComboBoxField fldDurability;

		public SubscriptionSection(FormToolkit toolkit, Composite parent) {
			super(toolkit, parent, "Subscription", "Configuration parameters for subscriptions to changes in the models indexed by Hawk.");
		    cContents.setLayout(Utils.createTableWrapLayout(2));

		    this.fldSubscribe = new FormCheckBoxField(toolkit, cContents, "Subscribe:", HawkModelDescriptor.DEFAULT_IS_SUBSCRIBED);
		    this.fldClientID = new FormTextField(toolkit, cContents, "Client ID:", HawkModelDescriptor.DEFAULT_CLIENTID);
		    this.fldDurability = new FormComboBoxField(toolkit, cContents, "Durability:", toStringArray(SubscriptionDurability.values()));

		    fldSubscribe.getCheck().addSelectionListener(this);
		    fldClientID.getText().addModifyListener(this);
		    fldDurability.getCombo().addSelectionListener(this);
		}

		private String[] toStringArray(Object[] values) {
			final String[] sArray = new String[values.length];

			int i = 0;
			for (Object v : values) {
				sArray[i++] = v + "";
			}
			return sArray;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (e.widget == fldSubscribe.getCheck()) {
				subscribeChanged();
			} else if (e.widget == fldDurability.getCombo()) {
				durabilityChanged();
			}
		}

		@Override
		public void modifyText(ModifyEvent e) {
			if (e.widget == fldClientID.getText()) {
				clientIDChanged();
			}
		}

		public boolean isSubscribed() {
			return fldSubscribe.getCheck().getSelection();
		}

		public void setSubscribed(boolean subscribed) {
			fldSubscribe.getCheck().setSelection(subscribed);
		}

		public String getClientID() {
			return fldClientID.getText().getText().trim();
		}

		public void setClientID(String s) {
			fldClientID.setTextWithoutListener(s, this);
		}

		public SubscriptionDurability getDurability() {
			return SubscriptionDurability.values()[fldDurability.getCombo().getSelectionIndex()];
		}

		public void setDurability(SubscriptionDurability s) {
			fldDurability.getCombo().select(s.ordinal());
		}

		protected abstract void subscribeChanged();
		protected abstract void clientIDChanged();
		protected abstract void durabilityChanged();
	}

	private InstanceSection instanceSection;
	private ContentSection contentSection;
	private SubscriptionSection subscriptionSection;

	DetailsFormPage(HawkMultiPageEditor editor, String id, String title) {
		super(editor, id, title);
	}

	@Override
	public HawkMultiPageEditor getEditor() {
		return (HawkMultiPageEditor) super.getEditor();
	}

	@Override
	public boolean isEditor() {
		return true;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		managedForm.getForm().setText("Remote Hawk Descriptor");

		final FormToolkit toolkit = managedForm.getToolkit();
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 1;
		final Composite formBody = managedForm.getForm().getBody();
		formBody.setLayout(layout);

		final FormText formText = toolkit.createFormText(formBody, true);
		formText.setText("<form><p>"
				+ "<a href=\"reopenEcore\">Open with Exeed</a> "
				+ "<a href=\"copyShortURL\">Copy short URL to clipboard</a> "
				+ "<a href=\"copyLongURL\">Copy long URL to clipboard</a>"
				+ "</p></form>", true, true);
		formText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				final String href = e.getHref().toString();
				switch (href) {
				case "reopenEcore":
					HawkMultiPageEditorContributor.reopenWithExeed(getEditor());
					break;
				case "copyLongURL":
				case "copyShortURL":
					try {
						final String url = HawkResourceFactoryImpl.generateHawkURL(getEditor().buildDescriptor(), "copyShortURL".equals(href));
						final Clipboard cb = new Clipboard(getSite().getShell().getDisplay());
						cb.setContents(new Object[]{url}, new Transfer[]{TextTransfer.getInstance()});
						cb.dispose();
					} catch (UnsupportedEncodingException ex) {
						Activator.getDefault().logError(ex);
					}
					break;
				}
			}
		});

		this.instanceSection = new InstanceSection(toolkit, formBody) {
			@Override protected void instanceNameChanged() { getEditor().setDirty(true); }
			@Override protected void serverURLChanged()    { getEditor().setDirty(true); }
			@Override protected void thriftProtocolChanged() { getEditor().setDirty(true); }
			@Override protected void usernameChanged() { getEditor().setDirty(true); }
			@Override protected void passwordChanged() { getEditor().setDirty(true); }
			@Override protected void selectInstance() {
				final HawkModelDescriptor d = getEditor().buildDescriptor();
				try {
					Hawk.Client client = getEditor().connectToHawk(d);
					final List<HawkInstance> instances = client.listInstances();
					Collections.sort(instances);
					client.getInputProtocol().getTransport().close();

					final Shell shell = formText.getShell();
					final ListDialog dlg = new ListDialog(shell);
					dlg.setInput(instances);
					dlg.setContentProvider(new ListContentProvider());
					dlg.setLabelProvider(new LabelProvider(){
						@Override
						public String getText(Object o) {
							if (o instanceof HawkInstance) {
								final HawkInstance hi = (HawkInstance)o;
								return hi.name;
							}
							return super.getText(o);
						}

						@Override
						public Image getImage(Object o) {
							if (o instanceof HawkInstance) {
								final HawkInstance hi = (HawkInstance) o;
								return Activator.getImageDescriptor(hi.state != HawkState.STOPPED
									? "/icons/nav_go.gif" : "/icons/nav_stop.gif")
									.createImage();
							}
							return super.getImage(o);
						}
					});
					dlg.setMessage("Select a Hawk instance:");
					dlg.setTitle("Hawk instance selection");
					if (dlg.open() == IDialogConstants.OK_ID) {
						final Object[] selected = dlg.getResult();
						if (selected.length > 0) {
							setInstanceName(((HawkInstance)selected[0]).name);
							instanceNameChanged();
						}
					}
				} catch (Exception ex) {
					Activator.getDefault().logError(ex);
				}
			}
		};
		this.contentSection = new ContentSection(toolkit, formBody) {
			@Override protected void filePatternsChanged()  { getEditor().setDirty(true); }
			@Override protected void repositoryURLChanged() { getEditor().setDirty(true); }
			@Override protected void loadingModeChanged() { getEditor().setDirty(true); }
			@Override protected void queryLanguageChanged() { getEditor().setDirty(true); }
			@Override protected void queryChanged() { getEditor().setDirty(true); }
			@Override protected void defaultNamespacesChanged() { getEditor().setDirty(true); }
			@Override protected void splitChanged() { getEditor().setDirty(true); }
			@Override protected void pageSizeChanged() { getEditor().setDirty(true); }

			@Override protected void selectQueryLanguage() {
				final HawkModelDescriptor d = getEditor().buildDescriptor();
				try {
					Hawk.Client client = getEditor().connectToHawk(d);
					final String[] languages = client.listQueryLanguages(
							d.getHawkInstance()).toArray(new String[0]);
					client.getInputProtocol().getTransport().close();

					final Shell shell = formText.getShell();
					final ListDialog dlg = new ListDialog(shell);
					dlg.setInput(languages);
					dlg.setContentProvider(new ArrayContentProvider());
					dlg.setLabelProvider(new LabelProvider());
					dlg.setMessage("Select a query language:");
					dlg.setTitle("Query language selection");
					dlg.setInitialSelections(new String[]{d.getHawkQueryLanguage()});
					if (dlg.open() == IDialogConstants.OK_ID) {
						final Object[] selected = dlg.getResult();
						if (selected.length > 0) {
							setQueryLanguage(selected[0].toString());
						} else {
							setQueryLanguage("");
						}
						queryLanguageChanged();
					}
				} catch (Exception ex) {
					Activator.getDefault().logError(ex);
				}
			}

			@Override
			protected void selectRepository() {
				final HawkModelDescriptor d = getEditor().buildDescriptor();
				try {
					Hawk.Client client = getEditor().connectToHawk(d);
					List<Repository> repositories = client.listRepositories(d.getHawkInstance());
					final String[] repos = new String[1 + repositories.size()];
					repos[0] = "*";
					int iRepo = 1;
					for (Repository r : repositories) {
						repos[iRepo++] = r.uri;
					}
					Arrays.sort(repos);
					client.getInputProtocol().getTransport().close();

					final Shell shell = formText.getShell();
					final ListDialog dlg = new ListDialog(shell);
					dlg.setInput(repos);
					dlg.setContentProvider(new ArrayContentProvider());
					dlg.setLabelProvider(new LabelProvider());
					dlg.setMessage("Select a repository:");
					dlg.setTitle("Repository selection");
					dlg.setInitialSelections(new String[]{d.getHawkRepository()});
					if (dlg.open() == IDialogConstants.OK_ID) {
						final Object[] selected = dlg.getResult();
						if (selected.length > 0) {
							setRepositoryURL(selected[0].toString());
						} else {
							setRepositoryURL("*");
						}
						repositoryURLChanged();
					}
				} catch (Exception ex) {
					Activator.getDefault().logError(ex);
				}
			}

			@Override
			protected void selectFiles() {
				final HawkModelDescriptor d = getEditor().buildDescriptor();
				try {
					Hawk.Client client = getEditor().connectToHawk(d);
					final String[] files = client.listFiles(
							d.getHawkInstance(), Arrays.asList(d.getHawkRepository()),
							Arrays.asList("*")).toArray(new String[0]);
					Arrays.sort(files);
					client.getInputProtocol().getTransport().close();

					final Shell shell = formText.getShell();
					final ListSelectionDialog dlg = new ListSelectionDialog(
							shell, files, new ArrayContentProvider(),
							new LabelProvider(), "Select files (zero files = all files):");
					dlg.setTitle("File selection");
					dlg.setInitialSelections(d.getHawkFilePatterns());
					if (dlg.open() == IDialogConstants.OK_ID) {
						final Object[] selected = dlg.getResult();
						if (selected.length > 0) {
							final String[] sFiles = new String[selected.length];
							for (int i = 0; i < selected.length; i++) {
								sFiles[i] = selected[i].toString();
							}
							setFilePatterns(sFiles);
						} else {
							setFilePatterns("*");
						}
						filePatternsChanged();
					}
				} catch (Exception ex) {
					Activator.getDefault().logError(ex);
				}
			}
		};
		this.subscriptionSection = new SubscriptionSection(toolkit, formBody) {
			@Override protected void subscribeChanged() { getEditor().setDirty(true); }
			@Override protected void clientIDChanged() { getEditor().setDirty(true); }
			@Override protected void durabilityChanged() { getEditor().setDirty(true); }
		};
	}

	public InstanceSection getInstanceSection() {
		return instanceSection;
	}

	public ContentSection getContentSection() {
		return contentSection;
	}

	public SubscriptionSection getSubscriptionSection() {
		return subscriptionSection;
	}
}