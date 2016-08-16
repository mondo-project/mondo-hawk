package org.hawk.orientdb.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Escapes and unescapes Hawk property/type/index names into safe
 * OrientDB field and class names.
 */
public final class OrientNameCleaner {

	private static final Map<String, String> INVALID_CLASS_CHAR_REPLACEMENTS;
	static {
		INVALID_CLASS_CHAR_REPLACEMENTS = new HashMap<String, String>();
		INVALID_CLASS_CHAR_REPLACEMENTS.put(":", "!hcol!");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(",", "!hcom!");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(";", "!hsco!");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(" ", "!hspa!");
		INVALID_CLASS_CHAR_REPLACEMENTS.put("%", "!hpct!");
		INVALID_CLASS_CHAR_REPLACEMENTS.put("=", "!hequ!");
		INVALID_CLASS_CHAR_REPLACEMENTS.put("@", "!hats!");
		INVALID_CLASS_CHAR_REPLACEMENTS.put(".", "!hdot!");
	}

	private static final Map<String, String> INVALID_FIELD_CHAR_REPLACEMENTS;
	static {
		INVALID_FIELD_CHAR_REPLACEMENTS = new HashMap<String, String>();
		INVALID_FIELD_CHAR_REPLACEMENTS.put(":", "!hcol!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(",", "!hcom!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(";", "!hsco!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(" ", "!hspa!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("%", "!hpct!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("=", "!hequ!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put(".", "!hdot!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("/", "!hfsl!");
		INVALID_FIELD_CHAR_REPLACEMENTS.put("\\", "!hbsl!");
	}

	private OrientNameCleaner() {}

	public static String escapeClass(final String unescaped) {
		String escaped = unescaped;
		for (Map.Entry<String, String> entry : INVALID_CLASS_CHAR_REPLACEMENTS.entrySet()) {
			escaped = escaped.replace(entry.getKey(), entry.getValue());
		}
		return escaped;
	}

	public static String unescapeFromField(final String escapedPropertyName) {
		String propertyName = escapedPropertyName;
		for (Entry<String, String> entry : INVALID_FIELD_CHAR_REPLACEMENTS.entrySet()) {
			propertyName = propertyName.replace(entry.getValue(), entry.getKey());
		}
		return propertyName;
	}

	public static String escapeToField(final String unescapedFieldName) {
		String fieldName = unescapedFieldName;
		for (Entry<String, String> entry : INVALID_FIELD_CHAR_REPLACEMENTS.entrySet()) {
			fieldName = fieldName.replace(entry.getKey(), entry.getValue());
		}
		return fieldName;
	}
}
