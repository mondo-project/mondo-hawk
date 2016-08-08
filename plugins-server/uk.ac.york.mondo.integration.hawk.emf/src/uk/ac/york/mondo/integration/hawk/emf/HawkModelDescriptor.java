/*******************************************************************************
 * Copyright (c) 2015 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Antonio Garcia-Dominguez - initial API and implementation
 *******************************************************************************/
package uk.ac.york.mondo.integration.hawk.emf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.york.mondo.integration.api.EffectiveMetamodelRuleset;
import uk.ac.york.mondo.integration.api.SubscriptionDurability;
import uk.ac.york.mondo.integration.api.utils.APIUtils.ThriftProtocol;

/**
 * Abstraction over the <code>.hawkmodel</code> file format. The file format is
 * a simple Java properties file. Each of the available properties is listed in
 * this class as a <code>PROPERTY_HAWK</code> constant: please consult their
 * individual documentation for details.
 */
public class HawkModelDescriptor {

	/*
	 * Note: all values of this enum must have names in uppercase, so the
	 * loadingMode values of the <code>hawk+http(s)://</code> URLs will be case
	 * insensitive.
	 */
	public static enum LoadingMode {
		/**
		 * Request every model element initially, including all attributes and
		 * references.
		 */
		GREEDY(true, true, true),

		/** Request all references initially, and request attributes on demand. */
		LAZY_ATTRIBUTES(true, false, true),

		/**
		 * Request only the root nodes' attributes and references initially. If
		 * a reference is navigated, fetch all the nodes referenced by this node.
		 */
		LAZY_CHILDREN(false, true, true),

		/**
		 * Request only the root nodes' attributes and references initially. If
		 * a reference is navigated, fetch all the nodes in this reference.
		 */
		LAZY_REFERENCES(false, true, false),

		/**
		 * Request only the root nodes without attributes. Load attributes on
		 * demand, and fetch all references from a node when one is needed.
		 */
		LAZY_ATTRIBUTES_CHILDREN(false, false, true),

		/**
		 * Request only the root nodes without attributes. Load attributes and
		 * references on demand.
		 */
		LAZY_ATTRIBUTES_REFERENCES(false, false, false)
		;

		public static String[] strings() {
			return toStringArray(values());
		}

		private final boolean greedyElements, greedyAttributes, greedyReferences;

		private LoadingMode(boolean greedyElements, boolean greedyAttributes, boolean greedyChildren) {
			this.greedyElements = greedyElements;
			this.greedyAttributes = greedyAttributes;
			this.greedyReferences = greedyElements || greedyChildren;
		}

		/**
		 * Returns <code>true</code> if all model elements should be fetched at
		 * once. If <code>false</code>, only the root elements will be fetched.
		 */
		public boolean isGreedyElements() {
			return greedyElements;
		}

		/**
		 * Returns <code>true</code> if attributes should be fetched when first
		 * fetching a node. If <code>false</code>, the attributes will be
		 * fetched on the first isSet or get call on an EAttribute from EMF.
		 */
		public boolean isGreedyAttributes() {
			return greedyAttributes;
		}

		/**
		 * Returns <code>true</code> if all references in a node should be
		 * fetched when first resolving one of those references. If
		 * <code>false</code>, only the nodes in that reference will be
		 * resolved.
		 *
		 * Setting this to <code>false</code> only makes sense if
		 * {@link #isGreedyElements()} is set to <code>false</code>. If
		 * {@link #isGreedyElements()} is true, we will already have all nodes
		 * anyway: this setting can be safely ignored in that case.
		 */
		public boolean isGreedyReferences() {
			return greedyReferences;
		}
	}

	public static final String DEFAULT_FILES = "*";
	public static final String DEFAULT_REPOSITORY = "*";
	public static final String DEFAULT_URL = "http://127.0.0.1:8080/thrift/hawk/tuple";
	public static final String DEFAULT_USERNAME = "";
	public static final String DEFAULT_PASSWORD = "";
	public static final String DEFAULT_INSTANCE = "myhawk";
	public static final LoadingMode DEFAULT_LOADING_MODE = LoadingMode.GREEDY;
	public static final boolean DEFAULT_IS_SUBSCRIBED = false;
	public static final boolean DEFAULT_IS_SPLIT = true;
	public static final int DEFAULT_PAGE_SIZE = 0;
	public static final ThriftProtocol DEFAULT_TPROTOCOL = ThriftProtocol.TUPLE;
	public static final String DEFAULT_CLIENTID = System.getProperty("user.name");
	public static final SubscriptionDurability DEFAULT_DURABILITY = SubscriptionDurability.DEFAULT;
	public static final String DEFAULT_DEFAULT_NAMESPACES = "http://buildingsmart.ifc2x3tc1.ecore";

	// Empty strings mean entire model - no actual query performed
	public static final String DEFAULT_QUERY_LANGUAGE = "";
	public static final String DEFAULT_QUERY = "";

	private static final Logger LOGGER = LoggerFactory.getLogger(HawkModelDescriptor.class);
	private static final String FILE_PATTERN_SEP = ",";

	/**
	 * Property that contains a comma-separated list of file patterns to be
	 * considered (such as <code>*.xmi</code>), or <code>*</code> if all files
	 * should be considered (the default, as in {@link #DEFAULT_REPOSITORY}).
	 */
	public static final String PROPERTY_HAWK_FILES = "hawk.files";

	/**
	 * Property that limits the contents of the resource to the files in the
	 * specified version control systems. It can be a comma-separated list of
	 * glob-like patterns (where * is a wildcard), or simply <code>*</code> if
	 * all repositories should be considered (the default, as in
	 * {@link #DEFAULT_REPOSITORY}).
	 */
	public static final String PROPERTY_HAWK_REPOSITORY = "hawk.repository";

	/**
	 * Property that must be set to the name of the remote Hawk index. By
	 * default, it is {@link #DEFAULT_INSTANCE}.
	 */
	public static final String PROPERTY_HAWK_INSTANCE = "hawk.instance";

	/**
	 * Property that must be set to the URL to the remote Hawk index (e.g.
	 * <code>http://127.0.0.1:8080/thrift/hawk</code>.
	 */
	public static final String PROPERTY_HAWK_URL = "hawk.url";

	/**
	 * Property that defines the Thrift protocol that should be used for
	 * serialization. It must be one of the values of {@link ThriftProtocol}. By
	 * default, it is {@link #DEFAULT_TPROTOCOL}.
	 */
	public static final String PROPERTY_HAWK_TPROTOCOL = "hawk.thriftProtocol";

	/**
	 * Property that contains a string with one of the values in
	 * {@link LoadingMode}, indicating how should the model be lodaded. The
	 * default value is {@link #DEFAULT_LOADING_MODE}.
	 */
	public static final String PROPERTY_HAWK_LOADING_MODE = "hawk.loadingMode";

	/**
	 * Property that indicates the query language to be used for the query in
	 * {@link #PROPERTY_HAWK_QUERY}.
	 */
	public static final String PROPERTY_HAWK_QUERY_LANGUAGE = "hawk.queryLanguage";

	/**
	 * Property that contains a list of comma-separated namespace URLs, used to resolve
	 * ambiguous type names.
	 */
	public static final String PROPERTY_HAWK_DEFAULT_NAMESPACES = "hawk.defaultNamespaces";

	/**
	 * Property that specifies a query in the language indicated in
	 * {@link #PROPERTY_HAWK_QUERY_LANGUAGE} that limits the contents of the
	 * model to the results of this query.
	 */
	public static final String PROPERTY_HAWK_QUERY = "hawk.query";

	/**
	 * Property that if set to true, indicates that the client should subscribe
	 * to changes in the Hawk index for the indicated files and repository. The
	 * default value is {@link #DEFAULT_IS_SUBSCRIBED}.
	 */
	public static final String PROPERTY_HAWK_SUBSCRIBE = "hawk.subscribe";

	/**
	 * Property that should be set to a unique client ID for subscriptions. It allows
	 * Hawk to know if the same subscriber is reconnecting to an old subscription.
	 */
	public static final String PROPERTY_HAWK_CLIENTID = "hawk.clientID";

	/**
	 * Property that indicates the durability of the Artemis queue to be used
	 * for subscriptions. The valid values are those of {@link SubscriptionDurability}.
	 */
	public static final String PROPERTY_HAWK_DURABILITY = "hawk.subscriptionDurability";

	/**
	 * Property that if set to true, will make the Hawk resource split its
	 * contents across surrogate file resources.
	 */
	public static final String PROPERTY_HAWK_SPLIT = "hawk.split";

	/**
	 * Property that if set to an integer value greater than 0, will make the
	 * initial load be "paged": since the Hawk API is stateless, what we do
	 * instead is fetching all the node IDs first in one go (which is less
	 * expensive), and then resolve their contents in batches. This usually
	 * scales better.
	 */
	public static final String PROPERTY_HAWK_PAGE_SIZE = "hawk.pageSize";

	/** Property that stores the username to be used to connect to Hawk. */
	public static final String PROPERTY_HAWK_USERNAME = "hawk.username";

	/** Property that stores the password to be used to connect to Hawk. */
	public static final String PROPERTY_HAWK_PASSWORD = "hawk.password";

	/** Prefix for a collection of properties containing the effective metamodel store. */
	public static final String PROPERTY_HAWK_EMM_PREFIX = "hawk.effectiveMetamodel";

	private String hawkURL = DEFAULT_URL;
	private String hawkInstance = DEFAULT_INSTANCE;
	private ThriftProtocol thriftProtocol = DEFAULT_TPROTOCOL;
	private String hawkUsername = DEFAULT_USERNAME;
	private String hawkPassword = DEFAULT_PASSWORD;

	private String hawkRepository = DEFAULT_REPOSITORY;
	private String[] hawkFilePatterns = new String[] { DEFAULT_FILES };
	private LoadingMode loadingMode = DEFAULT_LOADING_MODE;
	private String hawkQueryLanguage = DEFAULT_QUERY_LANGUAGE;
	private String hawkQuery = DEFAULT_QUERY;
	private String defaultNamespaces = DEFAULT_DEFAULT_NAMESPACES;
	private boolean isSplit = DEFAULT_IS_SPLIT;
	private int pageSize = DEFAULT_PAGE_SIZE;
	private EffectiveMetamodelRuleset emm = new EffectiveMetamodelRuleset();

	private boolean isSubscribed = DEFAULT_IS_SUBSCRIBED;
	private String subscriptionClientID = DEFAULT_CLIENTID;
	private SubscriptionDurability subscriptionDurability = DEFAULT_DURABILITY;

	public HawkModelDescriptor() {}

	/**
	 * Loads the contents of the input stream into this descriptor. The
	 * input stream is closed after this call completes.
	 */
	public void load(InputStream is) throws IOException {
		Properties props = new Properties();
		props.load(is);
		is.close();
		loadFromProperties(props);
	}

	/**
	 * Loads the contents of the reader into this descriptor. The
	 * reader is closed after this call completes.
	 */
	public void load(Reader r) throws IOException {
		Properties props = new Properties();
		props.load(r);
		r.close();
		loadFromProperties(props);
	}

	public String getHawkURL() {
		return hawkURL;
	}

	public void setHawkURL(String hawkURL) {
		this.hawkURL = hawkURL;
	}

	public String getHawkInstance() {
		return hawkInstance;
	}

	public void setHawkInstance(String hawkInstance) {
		this.hawkInstance = hawkInstance;
	}

	public String getHawkRepository() {
		return hawkRepository;
	}

	public void setHawkRepository(String hawkRepository) {
		this.hawkRepository = hawkRepository;
	}

	public String[] getHawkFilePatterns() {
		return hawkFilePatterns;
	}

	public void setHawkFilePatterns(String[] hawkFilePatterns) {
		this.hawkFilePatterns = hawkFilePatterns;
	}

	public LoadingMode getLoadingMode() {
		return loadingMode;
	}

	public void setLoadingMode(LoadingMode mode) {
		this.loadingMode = mode;
	}

	public String getHawkQueryLanguage() {
		return hawkQueryLanguage;
	}

	public void setHawkQueryLanguage(String hawkQueryLanguage) {
		this.hawkQueryLanguage = hawkQueryLanguage;
	}

	public String getHawkQuery() {
		return hawkQuery;
	}

	public void setHawkQuery(String hawkQuery) {
		this.hawkQuery = hawkQuery;
	}

	public boolean isSubscribed() {
		return isSubscribed;
	}

	public void setSubscribed(boolean isSubscribed) {
		this.isSubscribed = isSubscribed;
	}

	public ThriftProtocol getThriftProtocol() {
		return thriftProtocol;
	}

	public void setThriftProtocol(ThriftProtocol thriftProtocol) {
		this.thriftProtocol = thriftProtocol;
	}

	public String getSubscriptionClientID() {
		return subscriptionClientID;
	}

	public void setSubscriptionClientID(String clientID) {
		this.subscriptionClientID = clientID;
	}

	public SubscriptionDurability getSubscriptionDurability() {
		return subscriptionDurability;
	}

	public void setSubscriptionDurability(SubscriptionDurability subscriptionDurability) {
		this.subscriptionDurability = subscriptionDurability;
	}

	/**
	 * Returns <code>true</code> if the contents of the index should be split by
	 * indexed file (producing surrogate resources in the resource set), or
	 * <code>false</code> if everything should be in the original resource.
	 *
	 * Normally we want this to be <code>true</code> for editors and validators,
	 * which may want the original URI of the resource, and <code>false</code>
	 * for model transformations, which just want the set of all elements. CloudATL
	 * needs this to be set to <code>false</code>, for instance.
	 */
	public boolean isSplit() {
		return isSplit;
	}

	public void setSplit(boolean newValue) {
		isSplit = newValue;
	}

	public boolean isPaged() {
		return pageSize > 0;
	}

	/**
	 * Returns an integer value for the page size of the initial fetch. If
	 * greater than 0, the initial fetch will be done in two stages: first fetch
	 * all the node IDs, and then fetch their contents in batches. The Hawk API
	 * is stateless, so it doesn't support 'real' paged queries, but we can
	 * emulate them this way.
	 *
	 * This should be enabled for large models, to prevent the server from
	 * choking while trying to encode huge result sets.
	 */
	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public String getDefaultNamespaces() {
		return defaultNamespaces;
	}

	public void setDefaultNamespaces(String defaultNamespaces) {
		this.defaultNamespaces = defaultNamespaces;
	}

	public String getUsername() {
		return hawkUsername;
	}

	public void setUsername(String hawkUsername) {
		this.hawkUsername = hawkUsername;
	}

	public String getPassword() {
		return hawkPassword;
	}

	public void setPassword(String hawkPassword) {
		this.hawkPassword = hawkPassword;
	}

	public EffectiveMetamodelRuleset getEffectiveMetamodel() {
		return emm;
	}

	public void setEffectiveMetamodel(EffectiveMetamodelRuleset emm) {
		this.emm = emm;
	}

	public void save(OutputStream os) throws IOException {
		createProperties().store(os, "");
	}

	public void save(Writer w) throws IOException {
		createProperties().store(w, "");
	}

	private Properties createProperties() {
		final Properties props = new Properties();
		props.setProperty(PROPERTY_HAWK_URL, hawkURL);
		props.setProperty(PROPERTY_HAWK_INSTANCE, hawkInstance);
		props.setProperty(PROPERTY_HAWK_TPROTOCOL, thriftProtocol.toString());
		props.setProperty(PROPERTY_HAWK_USERNAME, hawkUsername);
		props.setProperty(PROPERTY_HAWK_PASSWORD, hawkPassword);

		props.setProperty(PROPERTY_HAWK_REPOSITORY, hawkRepository);
		props.setProperty(PROPERTY_HAWK_FILES, concat(hawkFilePatterns, FILE_PATTERN_SEP));
		props.setProperty(PROPERTY_HAWK_LOADING_MODE, loadingMode.toString());
		props.setProperty(PROPERTY_HAWK_QUERY_LANGUAGE, hawkQueryLanguage);
		props.setProperty(PROPERTY_HAWK_QUERY, hawkQuery);
		props.setProperty(PROPERTY_HAWK_DEFAULT_NAMESPACES, defaultNamespaces);
		props.setProperty(PROPERTY_HAWK_SPLIT, Boolean.toString(isSplit));
		props.setProperty(PROPERTY_HAWK_PAGE_SIZE, Integer.toString(pageSize));

		props.setProperty(PROPERTY_HAWK_SUBSCRIBE, Boolean.toString(isSubscribed));
		props.setProperty(PROPERTY_HAWK_CLIENTID, subscriptionClientID);
		props.setProperty(PROPERTY_HAWK_DURABILITY, subscriptionDurability.toString());

		new EffectiveMetamodelRulesetSerializer(PROPERTY_HAWK_EMM_PREFIX).save(emm, props);
		return props;
	}

	private void loadFromProperties(Properties props) throws IOException {
		this.hawkURL = requiredProperty(props, PROPERTY_HAWK_URL);
		this.hawkInstance = requiredProperty(props, PROPERTY_HAWK_INSTANCE);
		this.thriftProtocol = ThriftProtocol.valueOf(optionalProperty(props, PROPERTY_HAWK_TPROTOCOL, DEFAULT_TPROTOCOL + ""));
		this.hawkUsername = optionalProperty(props, PROPERTY_HAWK_USERNAME, DEFAULT_USERNAME);
		this.hawkPassword = optionalProperty(props, PROPERTY_HAWK_PASSWORD, DEFAULT_PASSWORD);

		this.hawkRepository = optionalProperty(props, PROPERTY_HAWK_REPOSITORY, DEFAULT_REPOSITORY);
		this.hawkFilePatterns = optionalProperty(props, PROPERTY_HAWK_FILES, DEFAULT_FILES).split(FILE_PATTERN_SEP);
		this.loadingMode = LoadingMode.valueOf(optionalProperty(props, PROPERTY_HAWK_LOADING_MODE, DEFAULT_LOADING_MODE + ""));
		this.hawkQueryLanguage = optionalProperty(props, PROPERTY_HAWK_QUERY_LANGUAGE, DEFAULT_QUERY_LANGUAGE);
		this.hawkQuery = optionalProperty(props, PROPERTY_HAWK_QUERY, DEFAULT_QUERY);
		this.defaultNamespaces = optionalProperty(props, PROPERTY_HAWK_DEFAULT_NAMESPACES, DEFAULT_DEFAULT_NAMESPACES);
		this.isSplit = Boolean.valueOf(optionalProperty(props, PROPERTY_HAWK_SPLIT, DEFAULT_IS_SPLIT + ""));
		this.pageSize = Integer.valueOf(optionalProperty(props, PROPERTY_HAWK_PAGE_SIZE, DEFAULT_PAGE_SIZE + ""));
		this.emm = new EffectiveMetamodelRulesetSerializer(PROPERTY_HAWK_EMM_PREFIX).load(props);

		this.isSubscribed = Boolean.valueOf(optionalProperty(props, PROPERTY_HAWK_SUBSCRIBE, Boolean.toString(DEFAULT_IS_SUBSCRIBED)));
		this.subscriptionClientID = optionalProperty(props, PROPERTY_HAWK_CLIENTID, DEFAULT_CLIENTID);
		this.subscriptionDurability = SubscriptionDurability.valueOf(optionalProperty(props, PROPERTY_HAWK_DURABILITY, DEFAULT_DURABILITY + ""));
	}

	private static String requiredProperty(Properties props, String name) throws IOException {
		final String value = (String) props.get(name);
		if (value == null) {
			throw new IOException(name + " has not been set");
		}
		return value;
	}

	private static String optionalProperty(Properties props, String name, String defaultValue) throws IOException {
		final String value = (String) props.get(name);
		if (value == null) {
			LOGGER.info("{} has not been set, using {} as default", name, defaultValue);
			return defaultValue;
		}
		return value;
	}

	private static String concat(final String[] elems, final String separator) {
		final StringBuffer sbuf = new StringBuffer();
		boolean bFirst = true;
		for (String filePattern : elems) {
			if (bFirst) {
				bFirst = false;
			} else {
				sbuf.append(separator);
			}
			sbuf.append(filePattern);
		}
		return sbuf.toString();
	}

	private static <T> String[] toStringArray(Object[] c) {
		final String[] strings = new String[c.length];
		int i = 0;
		for (Object o : c) {
			strings[i++] = o + "";
		}
		return strings;
	}
}
