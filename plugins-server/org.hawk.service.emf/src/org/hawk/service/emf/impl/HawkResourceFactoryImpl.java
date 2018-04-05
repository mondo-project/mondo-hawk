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
package org.hawk.service.emf.impl;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource.Factory;
import org.hawk.service.api.SubscriptionDurability;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;
import org.hawk.service.emf.HawkModelDescriptor;
import org.hawk.service.emf.HawkModelDescriptor.LoadingMode;

public class HawkResourceFactoryImpl implements Factory {

	private static final String URLPARAM_CLIENTID = "clientID";
	private static final String URLPARAM_DURABILITY = "durability";
	private static final String URLPARAM_SUBSCRIBE = "subscribe";
	private static final String URLPARAM_LOADING_MODE = "loadingMode";
	private static final String URLPARAM_REPOSITORY = "repository";
	private static final String URLPARAM_THRIFT_PROTOCOL = "tprotocol";
	private static final String URLPARAM_FILE_PATTERNS = "filePatterns";
	private static final String URLPARAM_INSTANCE = "instance";
	private static final String URLPARAM_QUERY = "query";
	private static final String URLPARAM_QUERY_LANGUAGE = "queryLanguage";
	private static final String URLPARAM_DEFAULT_NAMESPACES = "defaultNS";
	private static final String URLPARAM_SPLIT = "split";

	public HawkResourceFactoryImpl() {
		// TODO get credentials from Eclipse preferences?
	}

	@Override
	public HawkResourceImpl createResource(URI uri) {
		if (isHawkURL(uri)) {
			final HawkModelDescriptor descriptor = parseHawkURL(uri);
			return new HawkResourceImpl(uri, descriptor);
		} else {
			return new HawkResourceImpl(uri);
		}
	}

	/**
	 * From a {@link HawkModelDescriptor}, this method produces a string
	 * representation of an URI that can be used to load a model indexed by Hawk
	 * without a <code>.hawkmodel</code> file. If
	 * <code>removeDefaultValues</code> is true, the fields with default values
	 * will not be put in the URL. This may not work correctly if the server and
	 * the client have different default values, but it produces much shorter
	 * URLs most of the time.
	 */
	public static String generateHawkURL(HawkModelDescriptor d, boolean removeDefaultValues) throws UnsupportedEncodingException {
		final List<NameValuePair> params = new ArrayList<>();
		addParameter(params, URLPARAM_INSTANCE,
				d.getHawkInstance(), HawkModelDescriptor.DEFAULT_INSTANCE, removeDefaultValues);
		addParameter(params, URLPARAM_FILE_PATTERNS,
				join(d.getHawkFilePatterns(), ","), HawkModelDescriptor.DEFAULT_FILES, removeDefaultValues);
		addParameter(params, URLPARAM_THRIFT_PROTOCOL,
				d.getThriftProtocol().name().toUpperCase(), HawkModelDescriptor.DEFAULT_TPROTOCOL.name().toUpperCase(), removeDefaultValues);
		addParameter(params, URLPARAM_REPOSITORY,
				d.getHawkRepository(), HawkModelDescriptor.DEFAULT_REPOSITORY, removeDefaultValues);
		addParameter(params, URLPARAM_LOADING_MODE,
				d.getLoadingMode().name().toUpperCase(), HawkModelDescriptor.DEFAULT_LOADING_MODE.name().toUpperCase(), removeDefaultValues);
		addParameter(params, URLPARAM_QUERY_LANGUAGE,
				d.getHawkQueryLanguage(), HawkModelDescriptor.DEFAULT_QUERY_LANGUAGE, removeDefaultValues);
		addParameter(params, URLPARAM_QUERY,
				d.getHawkQuery(), HawkModelDescriptor.DEFAULT_QUERY, removeDefaultValues);
		addParameter(params, URLPARAM_DEFAULT_NAMESPACES,
				d.getDefaultNamespaces(), HawkModelDescriptor.DEFAULT_DEFAULT_NAMESPACES, removeDefaultValues);
		addParameter(params, URLPARAM_SPLIT,
				d.isSplit() + "", HawkModelDescriptor.DEFAULT_IS_SPLIT + "", removeDefaultValues);

		addParameter(params, URLPARAM_SUBSCRIBE,
				d.isSubscribed() + "", HawkModelDescriptor.DEFAULT_IS_SUBSCRIBED + "", removeDefaultValues);
		if (d.isSubscribed()) {
			addParameter(params, URLPARAM_DURABILITY,
				d.getSubscriptionDurability().name().toUpperCase(), HawkModelDescriptor.DEFAULT_DURABILITY.name().toUpperCase(), removeDefaultValues);
			addParameter(params, URLPARAM_CLIENTID,
				d.getSubscriptionClientID(), null, removeDefaultValues);
		}

		final StringBuffer url = new StringBuffer("hawk+");
		url.append(d.getHawkURL());
		url.append("?");
		url.append(URLEncodedUtils.format(params, Charset.forName("UTF-8")));
		return url.toString();
	}

	protected static void addParameter(final List<NameValuePair> params, final String key, final String value, final String defaultValue,
			boolean ignoreDefaultValue) {
		if (!ignoreDefaultValue || defaultValue == null || !defaultValue.equals(value)) {
			params.add(new BasicNameValuePair(key, value));
		}
	}

	protected HawkModelDescriptor parseHawkURL(URI uri) {
		// construct HawkModelDescriptor from URI on the fly
		final HawkModelDescriptor descriptor = new HawkModelDescriptor();
		final String instanceURL = uri.trimQuery().toString().replaceFirst("hawk[+]",  "");
		descriptor.setHawkURL(instanceURL);

		final List<NameValuePair> pairs = URLEncodedUtils.parse(uri.query(), Charset.forName("UTF-8"));
		for (NameValuePair pair : pairs) {
			final String v = pair.getValue();
			switch (pair.getName()) {
			case URLPARAM_INSTANCE:
				descriptor.setHawkInstance(v); break;
			case URLPARAM_FILE_PATTERNS:
				descriptor.setHawkFilePatterns(v.split(",")); break;
			case URLPARAM_THRIFT_PROTOCOL:
				descriptor.setThriftProtocol(ThriftProtocol.valueOf(v.toUpperCase())); break;
			case URLPARAM_REPOSITORY:
				descriptor.setHawkRepository(v); break;
			case URLPARAM_LOADING_MODE:
				descriptor.setLoadingMode(LoadingMode.valueOf(v.toUpperCase())); break;
			case URLPARAM_SPLIT:
				descriptor.setSplit(Boolean.valueOf(v)); break;
			case URLPARAM_SUBSCRIBE:
				descriptor.setSubscribed(Boolean.valueOf(v)); break;
			case URLPARAM_DURABILITY:
				descriptor.setSubscriptionDurability(SubscriptionDurability.valueOf(v.toUpperCase())); break;
			case URLPARAM_CLIENTID:
				descriptor.setSubscriptionClientID(v); break;
			case URLPARAM_QUERY_LANGUAGE:
				descriptor.setHawkQueryLanguage(v); break;
			case URLPARAM_QUERY:
				descriptor.setHawkQuery(v); break;
			}
		}
		return descriptor;
	}

	protected boolean isHawkURL(URI uri) {
		return uri.hasAbsolutePath() && uri.scheme() != null && uri.scheme().startsWith("hawk+");
	}

	private static String join(Object[] iterable, String delim) {
		StringBuffer sbuf = new StringBuffer();
		boolean first = true;
		for (Object o : iterable) {
			if (first) {
				first = false;
			} else {
				sbuf.append(',');
			}
			sbuf.append(o.toString());
		}
		return sbuf.toString();
	}

}
;