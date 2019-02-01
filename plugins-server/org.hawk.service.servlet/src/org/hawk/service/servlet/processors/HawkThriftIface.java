/*******************************************************************************
 * Copyright (c) 2015-2018 University of York, Aston University.
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
package org.hawk.service.servlet.processors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSession.QueueQuery;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.thrift.TException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hawk.core.IHawkFactory;
import org.hawk.core.IHawkPlugin;
import org.hawk.core.IMetaModelResourceFactory;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.core.IStateListener.HawkState;
import org.hawk.core.IVcsManager;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.graph.IGraphNodeReference;
import org.hawk.core.graph.IGraphTransaction;
import org.hawk.core.graph.IGraphTypeNodeReference;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.runtime.LocalHawkFactory;
import org.hawk.core.util.GraphChangeAdapter;
import org.hawk.core.util.IndexedAttributeParameters;
import org.hawk.graph.FileNode;
import org.hawk.graph.GraphWrapper;
import org.hawk.graph.MetamodelNode;
import org.hawk.graph.ModelElementNode;
import org.hawk.graph.TypeNode;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;
import org.hawk.osgiserver.HModelSchedulingRule;
import org.hawk.osgiserver.SecurePreferencesCredentialsStore;
import org.hawk.service.api.Credentials;
import org.hawk.service.api.DerivedAttributeSpec;
import org.hawk.service.api.EffectiveMetamodelRuleset;
import org.hawk.service.api.FailedQuery;
import org.hawk.service.api.File;
import org.hawk.service.api.Hawk;
import org.hawk.service.api.HawkFactoryNotFound;
import org.hawk.service.api.HawkInstance;
import org.hawk.service.api.HawkInstanceNotFound;
import org.hawk.service.api.HawkInstanceNotRunning;
import org.hawk.service.api.HawkMetamodelNotFound;
import org.hawk.service.api.HawkPlugin;
import org.hawk.service.api.HawkPluginCategory;
import org.hawk.service.api.HawkQueryOptions;
import org.hawk.service.api.HawkTypeNotFound;
import org.hawk.service.api.IndexedAttributeSpec;
import org.hawk.service.api.InvalidDerivedAttributeSpec;
import org.hawk.service.api.InvalidIndexedAttributeSpec;
import org.hawk.service.api.InvalidMetamodel;
import org.hawk.service.api.InvalidPollingConfiguration;
import org.hawk.service.api.InvalidQuery;
import org.hawk.service.api.MetamodelParserDetails;
import org.hawk.service.api.ModelElement;
import org.hawk.service.api.QueryReport;
import org.hawk.service.api.QueryResult;
import org.hawk.service.api.QueryResult._Fields;
import org.hawk.service.api.Repository;
import org.hawk.service.api.Subscription;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.api.UnknownQueryLanguage;
import org.hawk.service.api.UnknownRepositoryType;
import org.hawk.service.api.VCSAuthenticationFailed;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.artemis.server.Server;
import org.hawk.service.servlet.Activator;
import org.hawk.service.servlet.artemis.ArtemisProducerGraphChangeListener;
import org.hawk.service.servlet.artemis.ArtemisProducerStateListener;
import org.hawk.service.servlet.servlets.HawkThriftTupleServlet;
import org.hawk.service.servlet.utils.HawkModelElementEncoder;
import org.hawk.service.servlet.utils.HawkModelElementTypeEncoder;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * Entry point to the Hawk model indexers, implementing a Thrift-based API.
 */
public final class HawkThriftIface implements Hawk.Iface {

	protected class AsyncQueryExecutionJob extends Job {
		private final String uuid;
		private final String hawkInstanceName;
		private final String language;
		private final String query;
		private final HawkQueryOptions options;

		private Runnable doCancel;
		private CompletableFuture<QueryReport> report = new CompletableFuture<>();
		private long startMillis;

		protected AsyncQueryExecutionJob(String uuid, String language, HawkQueryOptions options,
				String query, String hawkInstanceName) {
			super("Running query " + uuid);

			this.language = language;
			this.options = options;
			this.query = query;
			this.uuid = uuid;
			this.hawkInstanceName = hawkInstanceName;
		}

		@Override
		protected void canceling() {
			if (doCancel != null) {
				doCancel.run();
				
				QueryReport rValue = new QueryReport();
				QueryResult rResult = new QueryResult();
				rResult.setVString("cancelled");
				rValue.setResult(rResult);
				rValue.setWallMillis(System.currentTimeMillis() - startMillis);
				rValue.setIsCancelled(true);
				report.complete(rValue);
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				startMillis = System.currentTimeMillis();
				final QueryReport rValue = performTimedQuery(hawkInstanceName, query, language, options, this::setDoCancel);
				report.complete(rValue);
				return new Status(IStatus.OK, getBundleName(), "Completed query " + uuid);
			} catch (Throwable e) {
				if (!report.isDone()) {
					report.completeExceptionally(e);
					return new Status(IStatus.ERROR, getBundleName(),
						"Query " + uuid + " failed: " + e.getMessage(), e);
				}
			}
			return new Status(IStatus.OK, getBundleName(), "Cancelled query " + uuid);
		}

		public Future<QueryReport> getQueryReport() {
			return report;
		}

		private String getBundleName() {
			return FrameworkUtil.getBundle(getClass()).getSymbolicName();
		}

		private void setDoCancel(Runnable doCancel) {
			this.doCancel = doCancel;
		}
	}

	/**
	 * {@link IGraphChangeListener} that waits for a synchronisation process to end and then start.
	 * The actual waiting is done through the {@link CountDownLatch#await()} method of the latch
	 * returned by {@link #getLatch()}.
	 */
	protected static final class SynchroniseLatchGraphChangeListener extends GraphChangeAdapter {
		boolean started = false;
		CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void synchroniseStart() {
			started = true;
		}

		@Override
		public void synchroniseEnd() {
			if (started) {
				latch.countDown();
			}
		}

		public CountDownLatch getLatch() {
			return latch;
		}
	}

	public static String getStateQueueName(final HModel model) {
		return "hawkstate." + model.getName();
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(HawkThriftIface.class); 

	private final ThriftProtocol thriftProtocol;
	private final Server artemisServer;

	// TODO: create Equinox declarative service for using this information for ACL
	@SuppressWarnings("unused")
	private final HttpServletRequest request;

	/* Keeps track of all the running asynchronous queries. */
	private static final Map<String, AsyncQueryExecutionJob> ASYNC_QUERIES
		= new ConcurrentHashMap<>();

	private static enum CollectElements { ALL, ONLY_ROOTS; }

	/**
	 * Only to be used from {@link HawkThriftProcessorFactory} to retrieve the
	 * original process map.
	 */
	HawkThriftIface() {
		this(null, null, null);
	}

	public HawkThriftIface(ThriftProtocol eventProtocol, HttpServletRequest request, Server artemisServer) {
		this.thriftProtocol = eventProtocol;
		this.request = request;
		this.artemisServer = artemisServer;
	}

	public ThriftProtocol getThriftProtocol() {
		return thriftProtocol;
	}

	private HModel getRunningHawkByName(String name) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		HModel model = getHawkByName(name);
		if (model.isRunning()) {
			return model;
		} else {
			throw new HawkInstanceNotRunning();
		}
	}

	private HModel getHawkByName(String name) throws HawkInstanceNotFound {
		final HModel model = HManager.getInstance().getHawkByName(name);
		if (model == null) {
			throw new HawkInstanceNotFound();
		}
		return model;
	}

	@Override
	public void registerMetamodels(String name, List<File> metamodels) throws HawkInstanceNotFound, HawkInstanceNotRunning, InvalidMetamodel, TException {
		final HModel model = getRunningHawkByName(name);

		List<java.io.File> files = new ArrayList<>();
		for (File f : metamodels) {
			try {
				// Remove path separators for now (UNIX-style / and Windows-style \)
				final String safeName = f.name.replaceAll("/", "_").replaceAll("\\\\", "_");
				final java.io.File dataFile = Activator.getInstance().writeToDataFile(safeName, f.contents);
				files.add(dataFile);
			} catch (FileNotFoundException ex) {
				throw new TException(ex);
			} catch (IOException ex) {
				throw new TException(ex);
			}
		}

		final java.io.File[] fArray = files.toArray(new java.io.File[files.size()]);
		model.registerMeta(fArray);
	}

	@Override
	public void unregisterMetamodels(String name, List<String> metamodels) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getRunningHawkByName(name);
		model.removeMetamodels(metamodels.toArray(new String[metamodels.size()]));
	}

	@Override
	public List<String> listMetamodels(String name) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		final HModel model = getRunningHawkByName(name);
		return model.getRegisteredMetamodels();
	}

	@Override
	public List<String> listQueryLanguages(String name) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		final HModel model = getRunningHawkByName(name);
		return new ArrayList<String>(model.getKnownQueryLanguages());
	}

	@Override
	public QueryResult query(String name, String query, String language, HawkQueryOptions opts)
			throws HawkInstanceNotFound, UnknownQueryLanguage, InvalidQuery,
			FailedQuery, TException {
		return performQuery(name, query, language, opts, null);
	}

	@Override
	public QueryReport timedQuery(String name, String query, String language, HawkQueryOptions opts)
			throws HawkInstanceNotFound, UnknownQueryLanguage, InvalidQuery,
			FailedQuery, TException {
		return performTimedQuery(name, query, language, opts, null);
	}

	private QueryResult performQuery(String name, String query, String language, HawkQueryOptions opts, Consumer<Runnable> cancelConsumer)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, InvalidQuery, FailedQuery, TException {
		final HModel model = getRunningHawkByName(name);
		try {
			final Map<String, Object> context = new HashMap<>();
			if (opts.isSetDefaultNamespaces()) {
				context.put(IQueryEngine.PROPERTY_DEFAULTNAMESPACES, opts.getDefaultNamespaces());
			}
	
			if (opts.isSetRepositoryPattern() || opts.isSetFilePatterns()) {
				final boolean allRepositories = !opts.isSetRepositoryPattern() || "*".equals(opts.getRepositoryPattern());
				final boolean allFiles = !opts.isSetFilePatterns() || Arrays.asList("*").equals(opts.getFilePatterns());
				if (!allRepositories || !allFiles) {
					context.put(IQueryEngine.PROPERTY_REPOSITORYCONTEXT, opts.isSetRepositoryPattern() ? opts.getRepositoryPattern() : "*");
					context.put(IQueryEngine.PROPERTY_FILECONTEXT, opts.isSetFilePatterns() ? join(opts.getFilePatterns(), ",") : "*");
				}
			}
			if (cancelConsumer != null) {
				context.put(IQueryEngine.PROPERTY_CANCEL_CONSUMER, cancelConsumer);
			}
			Object ret = model.query(query, language, context);
	
			final GraphWrapper gw = new GraphWrapper(model.getGraph());
			final HawkModelElementEncoder enc = new HawkModelElementEncoder(gw);
			enc.setUseContainment(opts.includeContained);
			enc.setIncludeNodeIDs(opts.includeNodeIDs);
			enc.setIncludeAttributes(opts.includeAttributes);
			enc.setIncludeReferences(opts.includeReferences);
			enc.setIncludeDerived(opts.includeDerived);
			final EffectiveMetamodelRuleset emm = new EffectiveMetamodelRuleset(
					opts.getEffectiveMetamodelIncludes(), opts.getEffectiveMetamodelExcludes());
			if (!emm.isEverythingIncluded()) {
				enc.setEffectiveMetamodel(emm);
			}
	
			final HawkModelElementTypeEncoder typeEnc = new HawkModelElementTypeEncoder(gw);
			try (final IGraphTransaction t = model.getGraph().beginTransaction()) {
				return encodeValue(model, ret, enc, typeEnc);
			}
		} catch (InvalidQueryException ex) {
			throw new InvalidQuery(ex.getMessage());
		} catch (QueryExecutionException ex) {
			throw new FailedQuery(ex.getMessage());
		} catch (Exception ex) {
			throw new TException(ex);
		}
	}

	private QueryReport performTimedQuery(String name, String query, String language, HawkQueryOptions opts, Consumer<Runnable> cancelConsumer)
			throws HawkInstanceNotFound, UnknownQueryLanguage, InvalidQuery, FailedQuery, TException {
		final long startMillis = System.currentTimeMillis();
		final QueryResult result = performQuery(name, query, language, opts, cancelConsumer);
		final long endMillis = System.currentTimeMillis();

		final QueryReport queryReport = new QueryReport();
		queryReport.setResult(result);
		queryReport.setWallMillis(endMillis - startMillis);
		return queryReport;
	}

	private String join(List<String> strings, String separator) {
		final StringBuffer sbuf = new StringBuffer();
		boolean first = true;
		for (String s : strings) {
			if (first) {
				first = false;
			} else {
				sbuf.append(separator);
			}
			sbuf.append(s);
		}
		return sbuf.toString();
	}

	@SuppressWarnings("unchecked")
	private QueryResult encodeValue(final HModel model, Object ret,
			HawkModelElementEncoder enc,
			HawkModelElementTypeEncoder typeEnc) throws Exception {
		if (ret instanceof Boolean) {
			return new QueryResult(_Fields.V_BOOLEAN, (Boolean)ret);
		} else if (ret instanceof Byte) {
			return new QueryResult(_Fields.V_BYTE, (Byte)ret);
		} else if (ret instanceof Double || ret instanceof Float) {
			return new QueryResult(_Fields.V_DOUBLE, ((Number)ret).doubleValue());
		} else if (ret instanceof Integer) {
			return new QueryResult(_Fields.V_INTEGER, (Integer)ret);
		} else if (ret instanceof Long) {
			return new QueryResult(_Fields.V_LONG, (Long)ret);
		} else if (ret instanceof Short) {
			return new QueryResult(_Fields.V_SHORT, (Short)ret);
		} else if (ret instanceof String) {
			return new QueryResult(_Fields.V_STRING, (String)ret);
		} else if (ret instanceof IGraphTypeNodeReference) {
			final IGraphNode n = ((IGraphTypeNodeReference)ret).getNode();
			return new QueryResult(_Fields.V_MODEL_ELEMENT_TYPE, typeEnc.encode(n));
		} else if (ret instanceof IGraphNodeReference) {
			final IGraphNodeReference ref = (IGraphNodeReference)ret;
			if (!enc.isEncoded(ref.getId())) {
				final ModelElement meEncoded = enc.encode(ref.getNode());
				if (meEncoded != null) {
					return new QueryResult(_Fields.V_MODEL_ELEMENT, meEncoded);
				}
			}
		} else if (ret instanceof IGraphNode) {
			final ModelElementNode meNode = new ModelElementNode((IGraphNode)ret);
			if (!enc.isEncoded(meNode)) {
				final ModelElement meEncoded = enc.encode(meNode);
				if (meEncoded != null) {
					return new QueryResult(_Fields.V_MODEL_ELEMENT, meEncoded);
				}
			}
		} else if (ret instanceof Map) {
			final Map<String, QueryResult> result = new HashMap<>();
			for (Map.Entry<Object, Object> entry : ((Map<Object, Object>)ret).entrySet()) {
				result.put(entry.getKey() + "", encodeValue(model, entry.getValue(), enc, typeEnc));
			}
			return new QueryResult(_Fields.V_MAP, result);
		} else if (ret instanceof Iterable) {
			final List<QueryResult> result = new ArrayList<>();
			final Iterable<?> c = (Iterable<?>) ret;
			for (Object o : c) {
				result.add(encodeValue(model, o, enc, typeEnc));
			}
			return new QueryResult(_Fields.V_LIST, result);
		}

		// Fallback on converting to a string
		return new QueryResult(_Fields.V_STRING, ret + "");
	}

	@Override
	public List<ModelElement> resolveProxies(String name, List<String> ids, HawkQueryOptions options) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getRunningHawkByName(name);

		final IGraphDatabase graph = model.getGraph();
		try (IGraphTransaction tx = graph.beginTransaction()) {
			final HawkModelElementEncoder encoder = new HawkModelElementEncoder(new GraphWrapper(graph));
			encoder.setIncludeNodeIDs(true);
			encoder.setUseContainment(false);
			encoder.setIncludeAttributes(options.isIncludeAttributes());
			encoder.setIncludeReferences(options.isIncludeReferences());
			encoder.setIncludeDerived(options.isIncludeDerived());
			final EffectiveMetamodelRuleset emm = new EffectiveMetamodelRuleset(
					options.getEffectiveMetamodelIncludes(),
					options.getEffectiveMetamodelExcludes());
			if (!emm.isEverythingIncluded()) {
				encoder.setEffectiveMetamodel(emm);
			}
			
			for (String id : ids) {
				try {
					encoder.encode(id);
				} catch (Exception ex) {
					LOGGER.error(ex.getMessage(), ex);
				}
			}
			return encoder.getElements();
		} catch (Exception ex) {
			throw new TException(ex);
		}
	}

	@Override
	public void addRepository(String name, Repository repo, Credentials credentials) throws HawkInstanceNotFound, HawkInstanceNotRunning, UnknownRepositoryType, VCSAuthenticationFailed {

		// TODO Integrate with centralized repositories API
		final HModel model = getRunningHawkByName(name);
		try {
			final String username = credentials != null ? credentials.username : null;
			final String password = credentials != null ? credentials.password : null;
			model.addVCS(repo.uri, repo.type, username, password, repo.isFrozen);
		} catch (NoSuchElementException ex) {
			throw new UnknownRepositoryType();
		}
	}

	@Override
	public void removeRepository(String name, String uri) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getRunningHawkByName(name);
		try {
			for (IVcsManager mgr : new ArrayList<>(model.getIndexer().getRunningVCSManagers())) {
				if (mgr.getLocation().equals(uri)) {
					model.removeRepository(mgr);
					break;
				}
			}
		} catch (Exception ex) {
			throw new TException(ex);
		}
	}

	@Override
	public void updateRepositoryCredentials(String name, String uri, Credentials cred) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getRunningHawkByName(name);
		for (IVcsManager mgr : model.getRunningVCSManagers()) {
			if (mgr.getLocation().equals(uri)) {
				mgr.setCredentials(cred.username, cred.password, model.getManager().getCredentialsStore());
				return;
			}
		}
	}

	@Override
	public List<Repository> listRepositories(String name) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		final HModel model = getRunningHawkByName(name);
		final List<Repository> repos = new ArrayList<Repository>();
		for (IVcsManager mgr : model.getRunningVCSManagers()) {
			final Repository repo = new Repository(mgr.getLocation(), mgr.getType());
			repo.setIsFrozen(mgr.isFrozen());
			repos.add(repo);
		}
		return repos;
	}

	@Override
	public List<String> listRepositoryTypes() {
		return new ArrayList<String>(HManager.getInstance().getVCSTypes());
	}

	@Override
	public List<String> listFiles(String name, List<String> repository, List<String> filePatterns) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getRunningHawkByName(name);

		final IGraphDatabase graph = model.getGraph();
		try (IGraphTransaction t = graph.beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(graph);

			final Set<FileNode> fileNodes = gw.getFileNodes(repository, filePatterns);
			final List<String> files = new ArrayList<>(fileNodes.size());
			for (FileNode node : fileNodes) {
				files.add(node.getFilePath());
			}

			return files;
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			throw new TException(ex);
		}
	}

	@Override
	public void configurePolling(String name, int base, int max) throws HawkInstanceNotFound, HawkInstanceNotRunning, InvalidPollingConfiguration {
		final HModel model = getRunningHawkByName(name);
		model.configurePolling(base, max);
	}

	@Override
	public void addDerivedAttribute(String name, DerivedAttributeSpec spec)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, InvalidDerivedAttributeSpec,
			TException {
		final HModel model = getRunningHawkByName(name);

		try {
			model.addDerivedAttribute(
				spec.metamodelUri, spec.typeName, spec.attributeName, spec.attributeType,
				spec.isMany, spec.isOrdered, spec.isUnique,
				spec.derivationLanguage, spec.derivationLogic);
		} catch (Exception ex) {
			throw new TException(ex);
		}
	}

	@Override
	public void removeDerivedAttribute(String name, DerivedAttributeSpec spec) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		final HModel model = getRunningHawkByName(name);
		model.removeDerivedAttribute(
			spec.metamodelUri, spec.typeName, spec.attributeName);
	}

	@Override
	public List<DerivedAttributeSpec> listDerivedAttributes(String name) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		final HModel model = getRunningHawkByName(name);

		final List<DerivedAttributeSpec> specs = new ArrayList<>();
		for (IndexedAttributeParameters sIndexedAttr : model.getDerivedAttributes()) {
			final DerivedAttributeSpec spec = new DerivedAttributeSpec(sIndexedAttr.getMetamodelUri(),
					sIndexedAttr.getTypeName(),
					sIndexedAttr.getAttributeName());
			specs.add(spec);
		}
		return specs;
	}

	@Override
	public void addIndexedAttribute(String name, IndexedAttributeSpec spec)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, InvalidIndexedAttributeSpec, TException {
		final HModel model = getRunningHawkByName(name);
		try {
			model.addIndexedAttribute(spec.metamodelUri, spec.typeName, spec.attributeName);
		} catch (Exception e) {
			throw new TException(e);
		}
	}

	@Override
	public void removeIndexedAttribute(String name, IndexedAttributeSpec spec) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		final HModel model = getRunningHawkByName(name);
		model.removeIndexedAttribute(spec.metamodelUri, spec.typeName, spec.attributeName);
	}

	@Override
	public List<IndexedAttributeSpec> listIndexedAttributes(String name) throws HawkInstanceNotFound, HawkInstanceNotRunning {
		final HModel model = getRunningHawkByName(name);

		final List<IndexedAttributeSpec> specs = new ArrayList<>();
		for (IndexedAttributeParameters sIndexedAttr : model.getIndexedAttributes()) {

			final IndexedAttributeSpec spec = new IndexedAttributeSpec(sIndexedAttr.getMetamodelUri(), 
					sIndexedAttr.getTypeName(),
					sIndexedAttr.getAttributeName());
			specs.add(spec);
		}
		return specs;
	}

	@Override
	public List<ModelElement> getModel(String name, HawkQueryOptions opts) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		return collectElements(name, CollectElements.ALL, opts);
	}

	@Override
	public List<ModelElement> getRootElements(String name, HawkQueryOptions opts) throws TException {
		opts.setIncludeNodeIDs(true);
		return collectElements(name, CollectElements.ONLY_ROOTS, opts);
	}

	private List<ModelElement> collectElements(String name, final CollectElements collectType, final HawkQueryOptions opts)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getRunningHawkByName(name);

		// TODO filtering by repository
		try (IGraphTransaction tx = model.getGraph().beginTransaction()) {
			final GraphWrapper gw = new GraphWrapper(model.getGraph());
			final HawkModelElementEncoder encoder = new HawkModelElementEncoder(gw);
			encoder.setIncludeAttributes(opts.includeAttributes);
			encoder.setIncludeReferences(opts.includeReferences);
			encoder.setIncludeDerived(opts.includeDerived);
			encoder.setIncludeNodeIDs(opts.includeNodeIDs);
			encoder.setUseContainment(opts.includeContained);

			final EffectiveMetamodelRuleset emm = new EffectiveMetamodelRuleset(
					opts.getEffectiveMetamodelIncludes(), opts.getEffectiveMetamodelExcludes());
			if (!emm.isEverythingIncluded()) {
				encoder.setEffectiveMetamodel(emm);
			}

			final Set<FileNode> fileNodes = gw.getFileNodes(Arrays.asList(opts.getRepositoryPattern()), opts.getFilePatterns());
			if (emm.getInclusionRules().isEmpty() || collectType == CollectElements.ONLY_ROOTS) {
				// No explicitly included types or we only want the roots - start with the files
				for (FileNode fileNode : fileNodes) {
					LOGGER.info("Retrieving elements from file {}", opts.getFilePatterns());

					switch (collectType) {
					case ALL:
						for (ModelElementNode meNode : fileNode.getModelElements()) {
							encoder.encode(meNode);
						}
						break;
					case ONLY_ROOTS:
						for (ModelElementNode meNode : fileNode.getRootModelElements()) {
							encoder.encode(meNode);
						}
						break;
					}
				}
			} else {
				// Explicitly listed metamodels/types - start with the types
				// TODO: talk with Dimitris about this (optimal traversal can vary)
				for (Entry<String, Map<String, ImmutableSet<String>>> mmEntry : emm.getInclusionRules().rowMap().entrySet()) {
					final String mmURI = mmEntry.getKey();
					final MetamodelNode mn = gw.getMetamodelNodeByNsURI(mmURI);
					for (final TypeNode tn : mn.getTypes()) {
						// Filter by type
						if (emm.isIncluded(mmURI, tn.getTypeName())) {
							LOGGER.info("Retrieving elements from type {}", tn.getTypeName());
							for (final ModelElementNode meNode : tn.getAll()) {
								// Filter by scope
								if (fileNodes.contains(meNode.getFileNode())) {
									encoder.encode(meNode);
								}
							} // for (meNode)
						}
					} // for (tn)
				} // for (mmEntry)
			}

			return encoder.getElements();
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage(), ex);
			throw new TException(ex);
		}
	}

	@Override
	public void createInstance(String name, String backend, int minDelay, int maxDelay, List<String> plugins, String factoryName) throws TException {
		try {
			final HManager manager = HManager.getInstance();
			if (manager.getHawkByName(name) == null) {
				IHawkFactory factory;
				if (factoryName == null) {
					factory = new LocalHawkFactory();
				} else {
					factory = manager.getHawkFactoryInstances().get(factoryName);
					if (factory == null) {
						throw new HawkFactoryNotFound();
					}
				}

				HModel model = HModel.create(factory, name, storageFolder(name),
						null, backend, plugins,
						manager, new SecurePreferencesCredentialsStore(), minDelay, maxDelay);
				addStateListener(model);
			}
		} catch (Exception ex) {
			throw new TException(ex);
		}
	}

	@Override
	public List<String> listPlugins() throws TException {
		return HManager.getInstance().getAvailablePlugins()
			.stream().map(p -> p.getType()).collect(Collectors.toList());
	}

	@Override
	public List<HawkPlugin> listPluginDetails() throws TException {
		List<IHawkPlugin> hawkPlugins = HManager.getInstance().getAvailablePlugins();

		final List<HawkPlugin> plugins = new ArrayList<>();
		for (IHawkPlugin hp : hawkPlugins) {
			HawkPlugin p = new HawkPlugin();
			p.setDescription(hp.getHumanReadableName());
			p.setCategory(HawkPluginCategory.valueOf(hp.getCategory().name()));
			p.setName(hp.getType());
			plugins.add(p);
		}

		return plugins;
	}

	@Override
	public List<String> listBackends() throws TException {
		return new ArrayList<>(HManager.getInstance().getIndexTypes());
	}

	@Override
	public List<HawkInstance> listInstances() throws TException {
		final List<HawkInstance> instances = new ArrayList<>();
		for (HModel m : HManager.getInstance().getHawks()) {
			final HawkInstance instance = new HawkInstance();
			instance.name = m.getName();
			instance.state = ArtemisProducerStateListener.mapHawkStateToThrift(m.getStatus());
			instance.message = m.getInfo();
			instances.add(instance);
		}
		return instances;
	}

	@Override
	public void removeInstance(String name) throws HawkInstanceNotFound, TException {
		final HModel model = getHawkByName(name);
		try {
			HManager.getInstance().delete(model, true);
			removeStateListener(model);
		} catch (BackingStoreException e) {
			throw new TException(e.getMessage(), e);
		}
	}

	@Override
	public void startInstance(String name) throws HawkInstanceNotFound, TException {
		final HModel model = getHawkByName(name);
		if (!model.isRunning()) {
			model.start(HManager.getInstance());
			addStateListener(model);
		}
	}

	@Override
	public void stopInstance(String name) throws HawkInstanceNotFound, TException {
		final HModel model = getHawkByName(name);
		if (model.isRunning()) {
			model.stop(ShutdownRequestType.ALWAYS);
			removeStateListener(model);
		}
	}

	protected void addStateListener(final HModel model) {
		try {
			final ArtemisProducerStateListener stateListener = new ArtemisProducerStateListener(model, getStateQueueName(model));
			model.getIndexer().addStateListener(stateListener);
		} catch (Exception e) {
			LOGGER.error("Could not add the state listener", e);
		}
	}

	protected void removeStateListener(final HModel model) {
		try {
			final ArtemisProducerStateListener stateListener = new ArtemisProducerStateListener(model, getStateQueueName(model));
			model.getIndexer().removeStateListener(stateListener);
		} catch (Exception e) {
			LOGGER.error("Could not remove the state listener", e);
		}
	}

	@Override
	public Subscription watchModelChanges(String name,
			String repositoryUri, List<String> filePaths,
			String uniqueClientID, SubscriptionDurability durability)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getHawkByName(name);

		// TODO keep track of durable subscriptions and save/restore/list/delete them?
		try {
			final ArtemisProducerGraphChangeListener listener = new ArtemisProducerGraphChangeListener(
					model.getName(), repositoryUri, filePaths, durability, thriftProtocol);

			// TODO sanitize unique client ID?
			final String queueAddress = listener.getQueueAddress();
			final String queueName = queueAddress + "." + uniqueClientID;
			createQueue(queueAddress, queueName, durability);

			model.addGraphChangeListener(listener);
			return new Subscription(artemisServer.getHost(), artemisServer.getPort(), queueAddress, queueName, artemisServer.isSSLEnabled());
		} catch (Exception e) {
			LOGGER.error("Could not register the new listener", e);
			throw new TException(e);
		}
	}

	@Override
	public Subscription watchStateChanges(String name) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getHawkByName(name);
		try {
			// We do not create the queue itself, as it's going to be a temporary one (and only clients can do that).
			// All we do is provide the connection details to the queue.
			final String queueAddress = getStateQueueName(model);
			final String queueName = queueAddress + "." + UUID.randomUUID();
			return new Subscription(artemisServer.getHost(), artemisServer.getPort(), queueAddress, queueName, artemisServer.isSSLEnabled());
		} catch (Exception e) {
			LOGGER.error("Could not register the new listener", e);
			throw new TException(e);
		}
	}

	private void createQueue(final String queueAddress, final String queueName, SubscriptionDurability durability) throws ActiveMQException, Exception {
		final TransportConfiguration inVMTransportConfig = new TransportConfiguration(InVMConnectorFactory.class.getName());
		try (ServerLocator loc = ActiveMQClient.createServerLocatorWithoutHA(inVMTransportConfig)) {
			try (ClientSessionFactory sf = loc.createSessionFactory()) {
				try (ClientSession session = sf.createSession()) {
					final QueueQuery queryResults = session.queueQuery(new SimpleString(queueName));
					if (!queryResults.isExists()) {
						switch (durability) {
						case TEMPORARY:
							// If we created a temporary queue here, it'd be removed right after closing the ClientSession:
							// there's no point. Only clients may create temporary queues (e.g. through their Consumer).
							LOGGER.warn("Only a client may create a temporary queue: ignoring request");
							break;
						case DURABLE:
						case DEFAULT:
							session.createQueue(queueAddress, queueName, durability == SubscriptionDurability.DURABLE);
							break;
						default:
							throw new IllegalArgumentException("Unknown subscription durability " + durability);
						}
						session.commit();
					}
				}
			}
		}
	}

	private java.io.File storageFolder(String instanceName) throws IOException {
		java.io.File dataFile = FrameworkUtil.getBundle(HawkThriftTupleServlet.class).getDataFile("hawk-" + instanceName);
		if (!dataFile.exists()) {
			dataFile.mkdir();
			LOGGER.info("Created storage directory for instance '{}' in '{}'", instanceName, dataFile.getPath());
		} else {
			LOGGER.info("Reused storage directory for instance '{}' in '{}'", instanceName, dataFile.getPath());
		}
		return dataFile;
	}

	@Override
	public void syncInstance(String name, boolean waitForSync) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getRunningHawkByName(name);
		try {
			// Register a graph change listener if we want to wait for the sync
			SynchroniseLatchGraphChangeListener listener = null;
			if (waitForSync) {
				model.getIndexer().waitFor(HawkState.RUNNING);

				listener = new SynchroniseLatchGraphChangeListener();
				model.getIndexer().addGraphChangeListener(listener);
			}

			// Request the sync
			model.sync();

			// Wait for the sync to end, if desired
			if (listener != null) {
				listener.getLatch().await();
				model.getIndexer().removeGraphChangeListener(listener);
			}

		} catch (Exception e) {
			throw new TException("Could not force an immediate synchronisation", e);
		}
	}

	@Override
	public boolean isFrozen(String name, String uri) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		for (Repository repo : listRepositories(name)) {
			if (repo.uri.equals(uri)) {
				return repo.isFrozen;
			}
		}
		return false;
	}

	@Override
	public void setFrozen(String name, String uri, boolean isFrozen) throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getHawkByName(name);
		for (IVcsManager manager : model.getRunningVCSManagers()) {
			if (manager.getLocation().equals(uri)) {
				manager.setFrozen(isFrozen);
				break;
			}
		}
	}

	@Override
	public List<MetamodelParserDetails> listMetamodelParsers(String name)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, TException {
		final HModel model = getHawkByName(name);

		final List<MetamodelParserDetails> results = new ArrayList<>();
		for (IMetaModelResourceFactory parser : model.getMetamodelParsers()) {
			MetamodelParserDetails details = new MetamodelParserDetails();
			details.setIdentifier(parser.getType());
			details.setFileExtensions(new HashSet<>(parser.getMetaModelExtensions()));
		}

		return results;
	}

	@Override
	public List<String> listTypeNames(String hawkInstanceName, String metamodelURI)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, HawkMetamodelNotFound, TException {
		final HModel model = getRunningHawkByName(hawkInstanceName);
		try {
			return model.getIntrospector().getTypes(metamodelURI);
		} catch (NoSuchElementException ex) {
			throw new HawkMetamodelNotFound();
		}
	}

	@Override
	public List<String> listAttributeNames(String hawkInstanceName, String metamodelURI, String typeName)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, HawkMetamodelNotFound, HawkTypeNotFound, TException {
		final HModel model = getRunningHawkByName(hawkInstanceName);

		if (!model.getIndexer().getKnownMMUris().contains(metamodelURI)) {
			throw new HawkMetamodelNotFound();
		}
		try {
			return model.getIntrospector().getAttributes(metamodelURI, typeName);
		} catch (NoSuchElementException ex) {
			throw new HawkTypeNotFound();
		}
	}

	@Override
	public String asyncQuery(String hawkInstanceName, String query, String language, HawkQueryOptions options)
			throws HawkInstanceNotFound, HawkInstanceNotRunning, UnknownQueryLanguage, InvalidQuery, TException {
		final HModel model = getRunningHawkByName(hawkInstanceName);

		final String queryUUID = UUID.randomUUID().toString();
		final AsyncQueryExecutionJob timedQueryJob = new AsyncQueryExecutionJob(queryUUID, language, options, query, hawkInstanceName);
		ASYNC_QUERIES.put(queryUUID, timedQueryJob);
		timedQueryJob.setRule(new HModelSchedulingRule(model));
		timedQueryJob.schedule();

		return queryUUID;
	}

	@Override
	public void cancelAsyncQuery(String queryID) throws TException {
		AsyncQueryExecutionJob queryJob = ASYNC_QUERIES.get(queryID);
		if (queryJob == null) {
			throw new InvalidQuery("Cannot find query with UUID " + queryID);
		} else {
			queryJob.cancel();
			ASYNC_QUERIES.remove(queryID);
		}
	}

	@Override
	public QueryReport fetchAsyncQueryResults(String queryID) throws TException {
		AsyncQueryExecutionJob queryJob = ASYNC_QUERIES.get(queryID);
		if (queryJob == null) {
			throw new InvalidQuery("Cannot find query with UUID " + queryID);
		} else {
			try {
				return queryJob.getQueryReport().get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				final String reason = "Query " + queryID + " was interrupted";
				LOGGER.error(reason, e);
				throw new FailedQuery(reason);
			} catch (ExecutionException e) {
				LOGGER.error(e.getMessage(), e);
				throw new FailedQuery(e.getMessage());
			} finally {
				ASYNC_QUERIES.remove(queryID);
			}
		}
	}
}