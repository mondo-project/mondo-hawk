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
package uk.ac.york.mondo.integration.api;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

/**
 * In-memory representation of an effective metamodel as a list of
 * include/exclude rules.
 */
public class EffectiveMetamodelRuleset {

	/** Wildcard for types or slots */
	public static final String WILDCARD = "*";

	/** @see #getInclusionRules() */
	private final Table<String, String, ImmutableSet<String>> inclusions;

	/** @see #getExclusionRules() */
	private final Table<String, String, ImmutableSet<String>> exclusions;

	/**
	 * Default constructor.
	 */
	public EffectiveMetamodelRuleset() {
		this.inclusions = HashBasedTable.create();
		this.exclusions = HashBasedTable.create();
	}

	/** Copy constructor. */
	public EffectiveMetamodelRuleset(EffectiveMetamodelRuleset toCopy) {
		this.inclusions = HashBasedTable.create(toCopy.inclusions);
		this.exclusions = HashBasedTable.create(toCopy.exclusions);
	}

	/** Constructor from two raw maps (e.g. from Thrift). <code>null</code> values are safely ignored. */
	public EffectiveMetamodelRuleset(Map<String, Map<String, Set<String>>> inclusionRules, Map<String, Map<String, Set<String>>> exclusionRules) {
		this();
		loadMapIntoTable(inclusionRules, this.inclusions);
		loadMapIntoTable(exclusionRules, this.exclusions);
	}

	/**
	 * Brings all the exclusions and inclusion rules from another effective
	 * metamodel into this one, potentially overriding some of the existing
	 * rules.
	 */
	public void importRules(EffectiveMetamodelRuleset source) {
		exclusions.putAll(source.exclusions);
		inclusions.putAll(source.inclusions);
	}

	protected void loadMapIntoTable(Map<String, Map<String, Set<String>>> rawMap, final Table<String, String, ImmutableSet<String>> table) {
		if (rawMap != null) {
			for (final Entry<String, Map<String, Set<String>>> mmEntry : rawMap.entrySet()) {
				final String mmURI = mmEntry.getKey();
				for (final Entry<String, Set<String>> typeEntry : mmEntry.getValue().entrySet()) {
					final String typeName = typeEntry.getKey();
					final ImmutableSet<String> slots = ImmutableSet.copyOf(typeEntry.getValue());
					table.put(mmURI, typeName, slots);
				}
			}
		}
	}

	public boolean isEverythingIncluded() {
		return inclusions.isEmpty() && exclusions.isEmpty();
	}

	public boolean isIncluded(String mmURI) {
		final boolean included = inclusions.isEmpty() || inclusions.containsRow(mmURI);
		final boolean excluded = !included || exclusions.contains(mmURI, WILDCARD);
		return included && !excluded;
	}

	public boolean isFullyIncluded(String mmURI) {
		final boolean included = inclusions.isEmpty() || inclusions.contains(mmURI, WILDCARD);
		final boolean excluded = !included || exclusions.containsRow(mmURI);
		return included && !excluded;
	}

	public boolean isIncluded(String mmURI, String typeName) {
		final boolean included = inclusions.isEmpty() || inclusions.contains(mmURI, WILDCARD) || inclusions.contains(mmURI, typeName);
		final Set<String> excludedSlots = exclusions.get(mmURI, typeName);
		final boolean excluded = !included || exclusions.contains(mmURI, WILDCARD) || excludedSlots != null && excludedSlots.contains(WILDCARD);
		return included && !excluded;
	}

	public boolean isFullyIncluded(String mmURI, String typeName) {
		final Set<String> includedSlots = inclusions.get(mmURI, typeName);
		final boolean included = inclusions.isEmpty() || inclusions.contains(mmURI, WILDCARD) || (includedSlots != null && includedSlots.contains(WILDCARD));
		final boolean excluded = !included || exclusions.contains(mmURI, WILDCARD) || exclusions.contains(mmURI, typeName);
		return included && !excluded;
	}

	public boolean isIncluded(String mmURI, String typeName, String slot) {
		final Set<String> includedSlots = inclusions.get(mmURI, typeName);
		final boolean included = inclusions.isEmpty() || inclusions.contains(mmURI, WILDCARD) || (includedSlots != null && (includedSlots.contains(WILDCARD) || includedSlots.contains(slot)));
		final Set<String> excludedSlots = exclusions.get(mmURI, typeName);
		final boolean excluded = !included || exclusions.contains(mmURI, WILDCARD) || (excludedSlots != null && (excludedSlots.contains(WILDCARD) || excludedSlots.contains(slot)));
		return included && !excluded;
	}

	public void include(String mmURI) {
		inclusions.put(mmURI, WILDCARD, ImmutableSet.of(WILDCARD));
	}

	public void include(String mmURI, String type) {
		inclusions.put(mmURI, type, ImmutableSet.of(WILDCARD));
	}

	public void include(String mmURI, String type, ImmutableSet<String> slots) {
		inclusions.put(mmURI, type, slots);
	}

	public void exclude(String mmURI) {
		exclusions.put(mmURI, WILDCARD, ImmutableSet.of(WILDCARD));
	}

	public void exclude(String mmURI, String type) {
		exclusions.put(mmURI, type, ImmutableSet.of(WILDCARD));
	}

	public void exclude(String mmURI, String type, ImmutableSet<String> slots) {
		if (slots == null || slots.isEmpty()) {
			exclusions.remove(mmURI, type);
		} else {
			exclusions.put(mmURI, type, slots);
		}
	}

	/**
	 * Undoes the effects of a {@link #include(String)} or {@link #exclude(String)} call.
	 */
	public void reset(String mmURI) {
		inclusions.remove(mmURI, WILDCARD);
		exclusions.remove(mmURI, WILDCARD);
	}

	/**
	 * Undoes the effects of a {@link #include(String, String)} or {@link #exclude(String, String)} call.
	 */
	public void reset(String mmURI, String type) {
		ImmutableSet<String> oldInclusions = inclusions.remove(mmURI, type);
		if (oldInclusions != null && !oldInclusions.contains(WILDCARD)) {
			inclusions.put(mmURI, type, oldInclusions);
		}

		ImmutableSet<String> oldExclusions = exclusions.remove(mmURI, type);
		if (oldExclusions != null && !oldExclusions.contains(WILDCARD)) {
			exclusions.put(mmURI, type, oldExclusions);
		}
	}

	/**
	 * Undoes the effects of a {@link #include(String, String, ImmutableSet)} or {@link #exclude(String, String, ImmutableSet)} call.
	 */
	public void reset(String mmURI, String type, String slot) {
		ImmutableSet<String> oldInclusions = inclusions.remove(mmURI, type);
		if (oldInclusions != null) {
			if (!oldInclusions.contains(slot)) { 
				inclusions.put(mmURI, type, oldInclusions);
			} else {
				inclusions.put(mmURI, type, copyWithout(slot, oldInclusions));
			}
		}
		
		ImmutableSet<String> oldExclusions = inclusions.remove(mmURI, type);
		if (oldExclusions != null) {
			if (!oldExclusions.contains(slot)) { 
				inclusions.put(mmURI, type, oldExclusions);
			} else {
				inclusions.put(mmURI, type, copyWithout(slot, oldExclusions));
			}
		}
	}

	protected ImmutableSet<String> copyWithout(String slot, ImmutableSet<String> oldInclusions) {
		ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
		for (String s : oldInclusions) {
			if (!slot.equals(s)) {
				builder.add(s);
			}
		}
		final ImmutableSet<String> newSet = builder.build();
		return newSet;
	}

	/**
	 * Returns an unmodifiable view of the table of inclusion rules: rows are
	 * metamodel URIs, columns are type names or WILDCARD (meaning all types in
	 * the metamodel) and cells are either sets of slot names or a singleton set
	 * with WILDCARD (meaning "all"). An empty table means "include everything".
	 */
	public Table<String, String, ImmutableSet<String>> getInclusionRules() {
		return Tables.unmodifiableTable(inclusions);
	}

	/**
	 * Returns an unmodifiable view of the table of exclusion rules: rows are
	 * metamodel URIs, columns are type names and cells are either sets of slot
	 * names or a singleton set with WILDCARD (meaning "all"). An empty table
	 * means "exclude nothing".
	 */
	public Table<String, String, ImmutableSet<String>> getExclusionRules() {
		return Tables.unmodifiableTable(exclusions);
	}

	/**
	 * Returns all the slots explicitly included (after filtering), or a singleton
	 * set with {@link #WILDCARD} if all slots are included.
	 */
	public ImmutableSet<String> getIncludedSlots(String mmURI, String typeName) {
		if (isEverythingIncluded()) {
			return ImmutableSet.of(WILDCARD);
		} else if (exclusions.contains(mmURI, WILDCARD)) {
			return ImmutableSet.of();
		}

		final ImmutableSet<String> included = inclusions.get(mmURI, typeName);
		if (included == null) {
			return ImmutableSet.of();
		}

		final ImmutableSet<String> excluded = exclusions.get(mmURI, typeName);
		if (excluded == null) {
			return included;
		}

		ImmutableSet.Builder<String> result = new ImmutableSet.Builder<>();
		for (String slot : included) {
			if (!excluded.contains(slot)) {
				result.add(slot);
			}
		}
		return result.build();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exclusions == null) ? 0 : exclusions.hashCode());
		result = prime * result + ((inclusions == null) ? 0 : inclusions.hashCode());
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
		EffectiveMetamodelRuleset other = (EffectiveMetamodelRuleset) obj;
		if (exclusions == null) {
			if (other.exclusions != null)
				return false;
		} else if (!exclusions.equals(other.exclusions))
			return false;
		if (inclusions == null) {
			if (other.inclusions != null)
				return false;
		} else if (!inclusions.equals(other.inclusions))
			return false;
		return true;
	}
}
