/*******************************************************************************
 * Copyright (c) 2015 University of York.
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
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package org.hawk.service.remote.thrift;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.hawk.core.IConsole;
import org.hawk.core.ICredentialsStore;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IMetaModelUpdater;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IModelResourceFactory;
import org.hawk.core.IModelUpdater;
import org.hawk.core.IStateListener;
import org.hawk.core.IVcsManager;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.VcsRepositoryDelta;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.query.IAccessListener;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.runtime.CompositeGraphChangeListener;
import org.hawk.core.runtime.CompositeStateListener;
import org.hawk.core.util.DerivedAttributeParameters;
import org.hawk.core.util.HawkProperties;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.osgiserver.HManager;
import org.hawk.service.api.Credentials;
import org.hawk.service.api.DerivedAttributeSpec;
import org.hawk.service.api.FailedQuery;
import org.hawk.service.api.Hawk.Client;
import org.hawk.service.api.HawkInstance;
import org.hawk.service.api.HawkInstanceNotFound;
import org.hawk.service.api.HawkQueryOptions;
import org.hawk.service.api.HawkState;
import org.hawk.service.api.HawkStateEvent;
import org.hawk.service.api.IndexedAttributeSpec;
import org.hawk.service.api.InvalidQuery;
import org.hawk.service.api.MetamodelParserDetails;
import org.hawk.service.api.QueryReport;
import org.hawk.service.api.Repository;
import org.hawk.service.api.Subscription;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.api.UnknownQueryLanguage;
import org.hawk.service.api.utils.APIUtils;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.api.utils.ActiveMQBufferTransport;
import org.hawk.service.artemis.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ThriftRemoteModelIndexer implements IModelIndexer {

	private final static Logger LOGGER = LoggerFactory.getLogger(ThriftRemoteModelIndexer.class);

	private final class StatePropagationConsumer implements MessageHandler {
		private long currentTimestamp = 0;
		private HawkState currentState;
		private String currentInfo;

		public StatePropagationConsumer(HawkState state, String info) {
			this.currentState = state;
			this.currentInfo = info;
			fireState();
			fireInfo();
		}

		@Override
		public void onMessage(ClientMessage message) {
			final TProtocol proto = ThriftProtocol.JSON.getProtocolFactory().getProtocol(new ActiveMQBufferTransport(message.getBodyBuffer()));
			final HawkStateEvent change = new HawkStateEvent();
			try {
				change.read(proto);
				if (message.getTimestamp() < currentTimestamp) {
					return;
				}
				currentTimestamp = message.getTimestamp();

				if (change.getState() != currentState) {
					currentState = change.getState();
					fireState();
				}
				if (!change.getMessage().equals(currentInfo)) {
					currentInfo = change.getMessage();
					fireInfo();
				}
			} catch (Exception ex) {
				Activator.logError(ex);
			}
		}

		protected void fireInfo() {
			getCompositeStateListener().info(currentInfo);
		}

		protected void fireState() {
			switch (currentState) {
			case RUNNING:
				stateListener.state(IStateListener.HawkState.RUNNING);
				break;
			case STOPPED:
				stateListener.state(IStateListener.HawkState.STOPPED);
				break;
			case UPDATING:
				stateListener.state(IStateListener.HawkState.UPDATING);
				break;
			}
		}
	}

	private final class RemoteQueryEngine implements IQueryEngine {
		private final String language;
		private String defaultNamespaces = "";

		private RemoteQueryEngine(String language) {
			this.language = language;
		}

		@Override
		public List<String> validate(String derivationlogic) {
			// TODO should we add something for this in the API?
			// Right now we don't have a way to validate derived
			// attribute expressions for remote Hawk instances.
			return Collections.emptyList();
		}

		@Override
		public String getType() {
			return language;
		}

		@Override
		public Object query(IModelIndexer m, File query, Map<String, Object> context) throws InvalidQueryException,
				QueryExecutionException {
			try {
				return query(m, fileToString(query), context);
			} catch (IOException e) {
				throw new InvalidQueryException(e);
			}
		}

		@Override
		public Object query(IModelIndexer m, String query, Map<String, Object> context) throws InvalidQueryException,
				QueryExecutionException {
			if (context == null) {
				context = Collections.emptyMap();
			}
			String sRepoScope = (String) context.get(PROPERTY_REPOSITORYCONTEXT);
			if (sRepoScope == null) {
				sRepoScope = "*";
			}

			final String sFileScope = (String) context.get(PROPERTY_FILECONTEXT);
			final List<String> filePatterns = new ArrayList<>();
			if (sFileScope == null) {
				filePatterns.add("*");
			} else {
				final String[] sFilePatterns = sFileScope.split(",");
				for (String sFilePattern : sFilePatterns) {
					filePatterns.add(sFilePattern);
				}
			}

			try {
				final HawkQueryOptions opts = new HawkQueryOptions();
				if (context.containsKey(PROPERTY_DEFAULTNAMESPACES)) {
					opts.setDefaultNamespaces((String) context.get(PROPERTY_DEFAULTNAMESPACES));
				} else {
					opts.setDefaultNamespaces(defaultNamespaces);
				}
				opts.setRepositoryPattern(sRepoScope);
				opts.setFilePatterns(filePatterns);
				opts.setIncludeAttributes(true);
				opts.setIncludeReferences(true);
				opts.setIncludeNodeIDs(true);
				opts.setIncludeContained(false);

				final String uuid = client.get().asyncQuery(name, query, language, opts);
				if (context.containsKey(PROPERTY_CANCEL_CONSUMER)) {
					@SuppressWarnings("unchecked")
					java.util.function.Consumer<Runnable> consumer = (java.util.function.Consumer<Runnable>) context.get(PROPERTY_CANCEL_CONSUMER);
					consumer.accept(() -> {
						try {
							client.get().cancelAsyncQuery(uuid);
						} catch (TException e) {
							LOGGER.error("Failed to cancel query " + uuid, e);
						}
					});
				}
				QueryReport result = client.get().fetchAsyncQueryResults(uuid);

				return result;
			} catch (UnknownQueryLanguage|InvalidQuery ex) {
				throw new InvalidQueryException(ex);
			} catch (FailedQuery ex) {
				throw new QueryExecutionException(ex);
			} catch (TException e) {
				console.printerrln("Could not run contextful query");
				console.printerrln(e);
				return e;
			}
		}

		@Override
		public IAccessListener calculateDerivedAttributes(IModelIndexer m,
				Iterable<IGraphNode> nodes) throws InvalidQueryException,
				QueryExecutionException {
			// this dummy query engine does *not* update derived attributes
			// -- we're just a client.
			return null;
		}

		@Override
		public void setDefaultNamespaces(String defaultNamespaces) {
			this.defaultNamespaces = defaultNamespaces;
		}
	}

	/**
	 * Dummy implementation of {@link IVcsManager} that only provides the
	 * location and type and sends credential changes to the remote instance
	 * (does not retrieve remote username/password for security reasons). Only
	 * useful for the GUI when querying a remote Hawk instance using the Thrift
	 * API.
	 */
	private final class DummyVcsManager implements IVcsManager {
		private final String location, type;

		private DummyVcsManager(String location, String type) {
			this.location = location;
			this.type = type;
		}

		@Override
		public File importFile(String revision, String path, File temp) {
			// nothing to do
			return null;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public void shutdown() {
			// nothing to do
		}

		@Override
		public String getLocation() {
			return location;
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public String getHumanReadableName() {
			return location;
		}

		@Override
		public String getCurrentRevision() throws Exception {
			return null;
		}

		@Override
		public List<VcsCommitItem> getDelta(String string)
				throws Exception {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((location == null) ? 0 : location.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DummyVcsManager other = (DummyVcsManager) obj;
			if (location == null) {
				if (other.location != null)
					return false;
			} else if (!location.equals(other.location))
				return false;
			return true;
		}

		@Override
		public boolean isAuthSupported() {
			return true;
		}

		@Override
		public boolean isPathLocationAccepted() {
			return true;
		}

		@Override
		public boolean isURLLocationAccepted() {
			return true;
		}

		@Override
		public void setCredentials(String username, String password, ICredentialsStore credStore) {
			try {
				// Update both our local and remote copies of the credentials
				credStore.put(location,
						new ICredentialsStore.Credentials(username, password));
				client.get().updateRepositoryCredentials(
					name, location, new Credentials(username, password));
			} catch (Exception e) {
				console.printerrln(e);
			}
		}

		@Override
		public String getFirstRevision() throws Exception {
			return null;
		}

		@Override
		public VcsRepositoryDelta getDelta(String startRevision, String endRevision) throws Exception {
			return null;
		}

		@Override
		public void init(String vcsloc, IModelIndexer indexer) throws Exception {
			// nothing to do
		}
		
		@Override
		public void run() throws Exception {
			// nothing to do
		}

		@Override
		public String getRepositoryPath(String rawPath) {
			return rawPath;
		}

		@Override
		public String getUsername() {
			try {
				org.hawk.core.ICredentialsStore.Credentials credentials = getCredentialsStore().get(location);
				if (credentials != null) {
					return credentials.getUsername();
				}
			} catch (Exception e) {
				LOGGER.error("Could not retrieve username from credentials store", e);
			}
			return null;
		}

		@Override
		public String getPassword() {
			try {
				org.hawk.core.ICredentialsStore.Credentials credentials = getCredentialsStore().get(location);
				if (credentials != null) {
					return credentials.getPassword();
				}
			} catch (Exception e) {
				LOGGER.error("Could not retrieve password from credentials store", e);
			}
			return null;
		}

		@Override
		public boolean isFrozen() {
			try {
				return client.get().isFrozen(name, location);
			} catch (TException e) {
				LOGGER.error(String.format(
					"Could not retrieve frozen state of repository %s from remote Hawk %s",
					name, location), e);
				return false;
			}
		}

		@Override
		public void setFrozen(boolean f) {
			try {
				client.get().setFrozen(name, location, f);
			} catch (TException e) {
				LOGGER.error(String.format(
					"Could not change frozen state of repository %s of remote Hawk %s",
					name, location), e);
			}
		}
	}

	private final String name, location;
	private final ThreadLocal<Client> client;
	private final IConsole console;

	/** Folder containing the Hawk properties.xml file. */
	private final File parentFolder;
	private final ICredentialsStore credStore;
	private String dbType;

	private final List<String> enabledPlugins;

	private final CompositeStateListener stateListener = new CompositeStateListener();
	private Consumer artemisConsumer;

	public ThriftRemoteModelIndexer(String name, String location, File parentFolder, Supplier<Client> clientSupplier,
			ICredentialsStore credStore, IConsole console, List<String> enabledPlugins) throws IOException {
		this.name = name;
		this.location = location;
		this.client = ThreadLocal.withInitial(clientSupplier);
		this.credStore = credStore;
		this.console = console;
		this.parentFolder = parentFolder;
		this.enabledPlugins = enabledPlugins;

		createDummyProperties(parentFolder);

		/*
		 * We subscribe to changes in the state of this indexer until it is
		 * deleted, if it exists already. If it doesn't exist yet, we'll try
		 * again after init invokes createInstance.
		 */
		connectToArtemis();
	}

	private void createDummyProperties(File parentFolder) throws IOException {
		if (parentFolder.exists()) {
			return;
		}

		parentFolder.mkdirs();
		HawkProperties props = new HawkProperties();
		props.setDbType("dummy");
		props.setMonitoredVCS(new ArrayList<String[]>());

		XStream stream = new XStream(new DomDriver());
		stream.processAnnotations(HawkProperties.class);
		String out = stream.toXML(props);
		try (BufferedWriter b = new BufferedWriter(new FileWriter(
				getParentFolder() + File.separator + "properties.xml"))) {
			b.write(out);
			b.flush();
		}
	}

	@Override
	public void requestImmediateSync() throws Exception {
		client.get().syncInstance(name, false);
	}

	@Override
	public void shutdown(ShutdownRequestType type) throws Exception {
		if (type == ShutdownRequestType.ALWAYS) {
			// for remote instances, we only honour explicit requests by users.
			client.get().stopInstance(name);
		}
		artemisConsumer.closeSession();
	}

	@Override
	public void delete() throws Exception {
		client.get().removeInstance(name);
		artemisConsumer.closeSession();
	}

	@Override
	public IGraphDatabase getGraph() {
		console.printerrln("Graph is not accessible for " + ThriftRemoteHawk.class.getName());
		return null;
	}
	
	@Override
	public Set<IVcsManager> getRunningVCSManagers() {
		try {
			List<Repository> repositories = client.get().listRepositories(name);
			Set<IVcsManager> dummies = new HashSet<>();
			for (final Repository repo : repositories) {
				dummies.add(new DummyVcsManager(repo.uri, repo.type));
			}
			return dummies;
		} catch (TException e) {
			console.printerrln(e);
			return Collections.emptySet();
		}
	}

	@Override
	public Set<String> getKnownMMUris() {
		try {
			return new HashSet<>(client.get().listMetamodels(name));
		} catch (TException e) {
			console.printerrln(e);
			return Collections.emptySet();
		}
	}

	@Override
	public String getId() {
		return name;
	}

	@Override
	public void registerMetamodels(File... files) throws Exception {
		List<org.hawk.service.api.File> thriftFiles = new ArrayList<>();
		for (File f : files) {
			thriftFiles.add(APIUtils.convertJavaFileToThriftFile(f));
		}
		client.get().registerMetamodels(name, thriftFiles);
	}

	@Override
	public IConsole getConsole() {
		return console;
	}

	@Override
	public void addVCSManager(IVcsManager vcs, boolean persist) {
		try {
			org.hawk.core.ICredentialsStore.Credentials storedCredentials = getCredentialsStore().get(vcs.getLocation());

			Credentials credentials = null;
			if (storedCredentials != null) {
				credentials = new Credentials();
				credentials.setUsername(storedCredentials.getUsername());
				credentials.setPassword(storedCredentials.getPassword());
			}

			final Repository repo = new Repository(vcs.getLocation(), vcs.getType());
			repo.setIsFrozen(false);
			client.get().addRepository(name, repo, credentials);
		} catch (Exception e) {
			console.printerrln("Could not add the specified repository");
			console.printerrln(e);
		}
	}

	@Override
	public void addModelUpdater(IModelUpdater updater) {
		console.printerrln("Cannot add model updaters to " + this.getClass().getName());
	}

	@Override
	public void addMetaModelResourceFactory(IMetaModelResourceFactory metaModelParser) {
		console.printerrln("Cannot add metamodel resource factories to " + this.getClass().getName());
	}

	@Override
	public void addModelResourceFactory(IModelResourceFactory modelParser) {
		console.printerrln("Cannot add model resource factories to " + this.getClass().getName());
	}

	@Override
	public void setDB(IGraphDatabase db, boolean persist) {
		setDBType(db.getClass().getCanonicalName());
	}

	@Override
	public void addQueryEngine(IQueryEngine q) {
		console.printerrln("Cannot add query engines to " + this.getClass().getName());
	}

	@Override
	public void init(int minDelay, int maxDelay) throws Exception {
		try {
			client.get().startInstance(name);
		} catch (HawkInstanceNotFound ex) {
			client.get().createInstance(name, dbType, minDelay, maxDelay, enabledPlugins);
		}
		connectToArtemis();
	}

	protected void connectToArtemis() {
		try {
			HawkState currentState = HawkState.RUNNING;
			String currentInfo = "";
			for (HawkInstance instance : client.get().listInstances()) {
				if (name.equals(instance.getName())) {
					currentState = instance.getState();
					currentInfo = instance.getMessage();
				}
			}

			org.hawk.core.ICredentialsStore.Credentials creds = HManager.getInstance().getCredentialsStore().get(location);
			if (artemisConsumer == null) {
				Subscription subState = client.get().watchStateChanges(name);
				artemisConsumer = APIUtils.connectToArtemis(subState, SubscriptionDurability.TEMPORARY);
			}
			if (!artemisConsumer.isSessionOpen()) {
				if (creds != null) {
					artemisConsumer.openSession(creds.getUsername(), creds.getPassword());
				} else {
					artemisConsumer.openSession(null, null);
				}
				artemisConsumer.processChangesAsync(new StatePropagationConsumer(currentState, currentInfo));
			}
		} catch (HawkInstanceNotFound nf) {
			/*
			 * Not found yet: this is probably because of a call from the
			 * constructor right before invoking init. This is normal: we
			 * will simply try again once init has invoked createInstance.
			 */
		} catch (Exception e) {
			Activator.logError(e);
		}
	}

	@Override
	public IModelResourceFactory getModelParser(String type) {
		console.printerrln("Cannot access model parsers in " + this.getClass().getName());
		return null;
	}

	@Override
	public IMetaModelResourceFactory getMetaModelParser(String metaModelType) {
		console.printerrln("Cannot access metamodel parsers in " + this.getClass().getName());
		return null;
	}

	@Override
	public Map<String, IQueryEngine> getKnownQueryLanguages() {
		try {
			final Map<String, IQueryEngine> dummyMap = new HashMap<>();
			for (final String language : client.get().listQueryLanguages(name)) {
				dummyMap.put(language, new RemoteQueryEngine(language));
			}
			return dummyMap;
		} catch (TException e) {
			console.printerrln("Could not retrieve the known query languages");
			console.printerrln(e);
			return Collections.emptyMap();
		}
	}

	@Override
	public File getParentFolder() {
		return parentFolder;
	}

	@Override
	public IMetaModelUpdater getMetaModelUpdater() {
		console.printerrln("Cannot check the metamodel updater in " + this.getClass().getName());
		return null;
	}

	@Override
	public void setMetaModelUpdater(IMetaModelUpdater metaModelUpdater) {
		console.printerrln("Cannot change the metamodel updater in " + this.getClass().getName());
	}

	@Override
	public void addDerivedAttribute(String metamodeluri, String typename,
			String attributename, String attributetype, boolean isMany,
			boolean isOrdered, boolean isUnique, String derivationlanguage,
			String derivationlogic) {
		DerivedAttributeSpec spec = new DerivedAttributeSpec();
		spec.setMetamodelUri(metamodeluri);
		spec.setTypeName(typename);
		spec.setAttributeName(attributename);
		spec.setAttributeType(attributetype);
		spec.setIsMany(isMany);
		spec.setIsOrdered(isOrdered);
		spec.setIsUnique(isUnique);
		spec.setDerivationLanguage(derivationlanguage);
		spec.setDerivationLogic(derivationlogic);

		try {
			client.get().addDerivedAttribute(name, spec);
		} catch (TException e) {
			console.printerrln("Could not add derived attribute");
			console.printerrln(e);
		}
	}

	@Override
	public void addIndexedAttribute(String metamodeluri, String typename, String attributename) {
		IndexedAttributeSpec spec = new IndexedAttributeSpec();
		spec.setMetamodelUri(metamodeluri);
		spec.setTypeName(typename);
		spec.setAttributeName(attributename);

		try {
			client.get().addIndexedAttribute(name, spec);
		} catch (TException e) {
			console.printerrln("Could not add indexed attribute");
			console.printerrln(e);
		}
	}

	private static String fileToString(File queryFile) throws IOException, FileNotFoundException {
		final StringBuffer sbuf = new StringBuffer();
		try (BufferedReader reader = new BufferedReader(new FileReader(queryFile))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sbuf.append(line);
				sbuf.append('\n');
			}
		}
		final String query = sbuf.toString();
		return query;
	}

	@Override
	public Collection<IndexedAttributeParameters> getDerivedAttributes() {
		final List<IndexedAttributeParameters> attrs = new ArrayList<>();
		try {
			for (DerivedAttributeSpec spec : client.get().listDerivedAttributes(name)) {
				DerivedAttributeParameters params = new DerivedAttributeParameters(
						spec.metamodelUri, spec.typeName, spec.attributeName,
						spec.attributeType, spec.isMany, spec.isOrdered,
						spec.isUnique, spec.derivationLanguage, spec.derivationLogic
						);
				attrs.add(params);
			}
		} catch (TException e) {
			console.printerrln("Could not list the derived attributes");
			console.printerrln(e);
		}
		return attrs;		
	}

	@Override
	public Collection<IndexedAttributeParameters> getIndexedAttributes() {
		final List<IndexedAttributeParameters> attrs = new ArrayList<>();
		try {
			for (DerivedAttributeSpec spec : client.get().listDerivedAttributes(name)) {
				IndexedAttributeParameters params = new IndexedAttributeParameters(
						spec.metamodelUri, spec.typeName, spec.attributeName
						);
				attrs.add(params);
			}
		} catch (TException e) {
			console.printerrln("Could not list the derived attributes");
			console.printerrln(e);
		}
		return attrs;	
	}

	@Override
	public Collection<String> getIndexes() {
		return Collections.emptyList();
	}

	@Override
	public List<String> validateExpression(String derivationlanguage, String derivationlogic) {
		return getKnownQueryLanguages().get(derivationlanguage).validate(derivationlogic);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isRunning() {
		connectToArtemis();
		try {
			for (HawkInstance instance : client.get().listInstances()) {
				if (instance.name.equals(name)) {
					return instance.state != HawkState.STOPPED;
				}
			}
		} catch (TException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean addGraphChangeListener(IGraphChangeListener changeListener) {
		// TODO Add remote notifications here?
		return false;
	}

	@Override
	public boolean removeGraphChangeListener(IGraphChangeListener changeListener) {
		// TODO Add remote notifications here?
		return false;
	}

	@Override
	public CompositeGraphChangeListener getCompositeGraphChangeListener() {
		// TODO Add remote notifications here?
		return null;
	}

	@Override
	public void setSyncMetricsEnabled(Boolean enable) {
		// do nothing
	}

	@Override
	public void removeMetamodels(String... metamodelURIs) throws Exception {
		client.get().unregisterMetamodels(name, Arrays.asList(metamodelURIs));
	}

	@Override
	public ICredentialsStore getCredentialsStore() {
		return credStore;
	}

	public String getDBType() {
		return dbType;
	}

	public void setDBType(String dbtype) {
		this.dbType = dbtype;
	}

	@Override
	public boolean addStateListener(IStateListener l) {
		return stateListener.add(l);
	}

	@Override
	public boolean removeStateListener(IStateListener l) {
		return stateListener.remove(l);
	}

	@Override
	public CompositeStateListener getCompositeStateListener() {
		return stateListener;
	}

	@Override
	public String getDerivedAttributeExecutionEngine() {
		return null;
	}

	@Override
	public void removeVCSManager(IVcsManager vcs) throws Exception {
		client.get().removeRepository(name, vcs.getLocation());
	}

	@Override
	public void setPolling(int base, int max) {
		try {
			client.get().configurePolling(name, base, max);
		} catch (TException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	@Override
	public boolean removeIndexedAttribute(String metamodelUri, String typename, String attributename) {
		IndexedAttributeSpec spec = new IndexedAttributeSpec();
		spec.setMetamodelUri(metamodelUri);
		spec.setTypeName(typename);
		spec.setAttributeName(attributename);
		try {
			client.get().removeIndexedAttribute(name, spec);
			return true;
		} catch (TException e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public boolean removeDerivedAttribute(String metamodelUri, String typeName, String attributeName) {
		DerivedAttributeSpec spec = new DerivedAttributeSpec();
		spec.setMetamodelUri(metamodelUri);
		spec.setTypeName(typeName);
		spec.setAttributeName(attributeName);
		try {
			client.get().removeDerivedAttribute(name, spec);
			return true;
		} catch (TException e) {
			LOGGER.error(e.getMessage(), e);
			return false;
		}
	}

	@Override
	public void waitFor(IStateListener.HawkState targetState) throws InterruptedException {
		waitFor(targetState, 0);
	}

	@Override
	public void waitFor(IStateListener.HawkState targetState, long timeoutMillis) throws InterruptedException {
		synchronized (stateListener) {
			final long end = System.currentTimeMillis() + timeoutMillis;
			for (IStateListener.HawkState s = stateListener.getCurrentState(); s != targetState; s = stateListener.getCurrentState()) {
				if (s == IStateListener.HawkState.STOPPED) {
					throw new IllegalStateException("The selected Hawk is stopped");
				}

				if (timeoutMillis == 0) {
					stateListener.wait();
				} else {
					final long remaining = end - System.currentTimeMillis();
					if (remaining > 0) {
						// Wait for the remaining time
						stateListener.wait(remaining);
					} else {
						// Exit the loop due to timeout
						break;
					}
				}
			}
		}
	}

	@Override
	public <T> ScheduledFuture<T> scheduleTask(Callable<T> task, long delayMillis) {
		// TODO If this works to fix server configuration issues,
		// reevaluate.
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> getKnownMetamodelFileExtensions() {
		try {
			List<MetamodelParserDetails> details = client.get().listMetamodelParsers(name);
			Set<String> extensions = new HashSet<>();
			for (MetamodelParserDetails e : details) {
				extensions.addAll(e.getFileExtensions());
			}
			return extensions;
		} catch (TException e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}

	@Override
	public Set<String> getKnownMetaModelParserTypes() {
		try {
			List<MetamodelParserDetails> details = client.get().listMetamodelParsers(name);
			Set<String> types = new HashSet<>();
			for (MetamodelParserDetails e : details) {
				types.add(e.getIdentifier());
			}
			return types;
		} catch (TException e) {
			LOGGER.error(e.getMessage(), e);
			return Collections.emptySet();
		}
	}

	@Override
	public List<IModelUpdater> getModelUpdaters() {
		console.printerrln("Cannot access updaters in " + this.getClass().getName());
		return null;
	}

}
