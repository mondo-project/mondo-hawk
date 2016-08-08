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
package org.hawk.service.emf;

import java.util.Map;
import java.util.Map.Entry;

import org.hawk.service.api.EffectiveMetamodelRuleset;

import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.google.common.collect.TreeBasedTable;

public class EffectiveMetamodelRulesetSerializer {

	private static final char SEPARATOR = '.';
	static final String INCLUDES_SUFFIX = ".includes";
	static final String EXCLUDES_SUFFIX = ".excludes";

	private final String propertyPrefix;

	public EffectiveMetamodelRulesetSerializer(String propertyPrefix) {
		this.propertyPrefix = propertyPrefix;
	}

	public EffectiveMetamodelRuleset load(Properties props) {
		final Map<Integer, String> incMetamodels = new TreeMap<>();
		final Table<Integer, Integer, String> incTypeTable = TreeBasedTable.create();
		final Table<Integer, Integer, ImmutableSet<String>> incSlotTable = TreeBasedTable.create();

		final Map<Integer, String> excMetamodels = new TreeMap<>();
		final Table<Integer, Integer, String> excTypeTable = TreeBasedTable.create();
		final Table<Integer, Integer, ImmutableSet<String>> excSlotTable = TreeBasedTable.create();

		for (String propName : props.stringPropertyNames()) {
			if (propName.startsWith(propertyPrefix)) {
				final String raw = propName.substring(propertyPrefix.length());
				boolean isIncludes;
				String unprefixed;
				if (raw.startsWith(INCLUDES_SUFFIX)) {
					isIncludes = true;
					unprefixed = raw.substring(INCLUDES_SUFFIX.length());
				} else if (raw.startsWith(EXCLUDES_SUFFIX)) {
					isIncludes = false;
					unprefixed = raw.substring(EXCLUDES_SUFFIX.length());
				} else {
					continue;
				}
				final String[] parts = unprefixed.split("[" + SEPARATOR + "]");

				final String propValue = props.getProperty(propName).trim();
				int iMetamodel, iType;
				switch (parts.length) {
				case 1: // prefix0 -> URI of the first metamodel
					iMetamodel = Integer.valueOf(parts[0]);
					String mmURI = propValue;
					if (isIncludes) {
						incMetamodels.put(iMetamodel, mmURI);
					} else {
						excMetamodels.put(iMetamodel, mmURI);
					}
					break;
				case 2: // prefix0.0 -> name of the first type of the first metamodel
					iMetamodel = Integer.valueOf(parts[0]);
					iType = Integer.valueOf(parts[1]);
					String type = propValue;
					if (isIncludes) {
						incTypeTable.put(iMetamodel, iType, type);
					} else {
						excTypeTable.put(iMetamodel, iType, type);
					}
					break;
				case 3: // prefix0.0.slots -> comma-separated slots for the first type of first metamodel (if not all)
					iMetamodel = Integer.valueOf(parts[0]);
					iType = Integer.valueOf(parts[1]);
					ImmutableSet<String> slots;
					if (propValue.length() > 0) {
						slots = ImmutableSet.copyOf(propValue.split("[" + SEPARATOR + "]"));
					} else {
						slots = ImmutableSet.of();
					}
					if (isIncludes) {
						incSlotTable.put(iMetamodel, iType, slots);
					} else {
						excSlotTable.put(iMetamodel, iType, slots);
					}
					break;
				default:
					throw new IllegalArgumentException(String
							.format("Property %s should only have 1-3 parts, but has %d",
									propName, parts.length));
				}
			}
		}

		final EffectiveMetamodelRuleset ruleset = new EffectiveMetamodelRuleset();

		for (final Cell<Integer, Integer, ImmutableSet<String>> mmEntry : incSlotTable.cellSet()) {
			final String mmURI = incMetamodels.get(mmEntry.getRowKey());
			final String typeName = incTypeTable.get(mmEntry.getRowKey(), mmEntry.getColumnKey());
			final ImmutableSet<String> slots = mmEntry.getValue();

			if (EffectiveMetamodelRuleset.WILDCARD.equals(typeName)) {
				ruleset.include(mmURI);
			} else if (slots.contains(EffectiveMetamodelRuleset.WILDCARD)) {
				ruleset.include(mmURI, typeName);
			} else {
				ruleset.include(mmURI, typeName, slots);
			}
		}

		for (final Cell<Integer, Integer, ImmutableSet<String>> mmEntry : excSlotTable.cellSet()) {
			final String mmURI = excMetamodels.get(mmEntry.getRowKey());
			final String typeName = excTypeTable.get(mmEntry.getRowKey(), mmEntry.getColumnKey());
			final ImmutableSet<String> slots = mmEntry.getValue();

			if (EffectiveMetamodelRuleset.WILDCARD.equals(typeName)) {
				ruleset.exclude(mmURI);
			} else if (slots.contains(EffectiveMetamodelRuleset.WILDCARD)) {
				ruleset.exclude(mmURI, typeName);
			} else {
				ruleset.exclude(mmURI, typeName, slots);
			}
		}

		return ruleset;
	}

	public void save(EffectiveMetamodelRuleset rules, Properties props) {
		if (rules.isEverythingIncluded()) {
			// by default, everything is included (see #load and EffectiveMetamodelStore javadocs) 
			return;
		}

		save(rules.getInclusionRules(), INCLUDES_SUFFIX, props);
		save(rules.getExclusionRules(), EXCLUDES_SUFFIX, props);
	}

	private void save(Table<String, String, ImmutableSet<String>> table, String suffix, Properties props) {
		int iMetamodel = 0;
		for (Entry<String, Map<String, ImmutableSet<String>>> mmEntry : table.rowMap().entrySet()) {
			final String mmURI = mmEntry.getKey();
			props.put(propertyPrefix + suffix + iMetamodel, mmURI);

			int iType = 0;
			for (Entry<String, ImmutableSet<String>> typeEntry : mmEntry.getValue().entrySet()) {
				final String type = typeEntry.getKey();
				props.put(propertyPrefix + suffix + iMetamodel + SEPARATOR + iType, type);

				final Set<String> slots = typeEntry.getValue();
				final StringBuffer sbuf = new StringBuffer();
				boolean first = true;
				for (String slot : slots) {
					if (first) {
						first = false;
					} else {
						sbuf.append(SEPARATOR);
					}
					sbuf.append(slot);
				}
				props.put(propertyPrefix + suffix + iMetamodel + SEPARATOR + iType + SEPARATOR + "slots", sbuf.toString());
				iType++;
			}

			iMetamodel++;
		}
	}
	
}
