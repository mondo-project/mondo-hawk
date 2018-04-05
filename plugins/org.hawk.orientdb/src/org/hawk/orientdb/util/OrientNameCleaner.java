/*******************************************************************************
 * Copyright (c) 2015-2016 The University of York, Aston University.
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
 *     Antonio Garcia-Dominguez - initial API and implementation
 ******************************************************************************/
package org.hawk.orientdb.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Escapes and unescapes Hawk property/type/index names into safe
 * OrientDB field and class names.
 */
public final class OrientNameCleaner {

	private static final Map<String, String> INVALID_CLASS_CHAR_REPLACEMENTS;
	private static final Pattern PATTERN_CLASS_CHAR_REPLACEMENT;
	static {
		INVALID_CLASS_CHAR_REPLACEMENTS = new HashMap<String, String>();
		INVALID_CLASS_CHAR_REPLACEMENTS.put(":", "_hcol_");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(",", "_hcom_");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(";", "_hsco_");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(" ", "_hspa_");
		INVALID_CLASS_CHAR_REPLACEMENTS.put("%", "_hpct_");
		INVALID_CLASS_CHAR_REPLACEMENTS.put("=", "_hequ_");
		INVALID_CLASS_CHAR_REPLACEMENTS.put("@", "_hats_");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(".", "_hdot_");

		PATTERN_CLASS_CHAR_REPLACEMENT = Pattern.compile("(:|,|;| |%|=|@|[.])");
	}

	private static final Map<String, String> INVALID_FIELD_CHAR_REPLACEMENTS;
	private static final Pattern PATTERN_FIELD_CHAR_REPLACEMENT;
	static {
		INVALID_FIELD_CHAR_REPLACEMENTS = new HashMap<String, String>();
		INVALID_FIELD_CHAR_REPLACEMENTS.put(":", "_hcol_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(",", "_hcom_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(";", "_hsco_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(" ", "_hspa_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("%", "_hpct_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("=", "_hequ_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(".", "_hdot_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("/", "_hfsl_");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("\\", "_hbsl_");

		PATTERN_FIELD_CHAR_REPLACEMENT = Pattern.compile("(:|,|;| |%|=|[.]|/|\\\\)");
	}

	private OrientNameCleaner() {}

	public static String escapeClass(final String unescaped) {
		final Matcher m = PATTERN_CLASS_CHAR_REPLACEMENT.matcher(unescaped);
		final StringBuffer sb = new StringBuffer(unescaped.length());
		while (m.find()) {
			m.appendReplacement(sb, INVALID_CLASS_CHAR_REPLACEMENTS.get(m.group(1)));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public static String unescapeFromField(final String escapedPropertyName) {
		String propertyName = escapedPropertyName;
		for (Entry<String, String> entry : INVALID_FIELD_CHAR_REPLACEMENTS.entrySet()) {
			propertyName = propertyName.replace(entry.getValue(), entry.getKey());
		}
		return propertyName;
	}

	public static String escapeToField(final String unescapedFieldName) {
		final Matcher m = PATTERN_FIELD_CHAR_REPLACEMENT.matcher(unescapedFieldName);
		final StringBuffer sb = new StringBuffer(unescapedFieldName.length());
		while (m.find()) {
			m.appendReplacement(sb, INVALID_FIELD_CHAR_REPLACEMENTS.get(m.group(1)));
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
