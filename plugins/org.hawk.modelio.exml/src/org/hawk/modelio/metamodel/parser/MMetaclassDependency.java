/*******************************************************************************
 * Copyright (c) 2017 Aston University
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Orjuwan Al-Wadeai - Modelio XML metamodel parser 
 ******************************************************************************/

package org.hawk.modelio.metamodel.parser;

public class MMetaclassDependency {

	private String name;
	private int min;
	private int max;
	private String aggregation;
	private boolean navigate;
	private boolean  cascadeDelete;
	private boolean  weakReference;

	private String oppositeName;
	private MMetaclassDependency oppositeDependency;

	private MMetaclassReference target;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public String getAggregation() {
		return aggregation;
	}

	public void setAggregation(String aggregation) {
		this.aggregation = aggregation;
	}

	public boolean isNavigate() {
		return navigate;
	}

	public void setNavigate(boolean navigate) {
		this.navigate = navigate;
	}

	public boolean isCascadeDelete() {
		return cascadeDelete;
	}

	public void setCascadeDelete(boolean cascadeDelete) {
		this.cascadeDelete = cascadeDelete;
	}

	public boolean isWeakReference() {
		return weakReference;
	}

	public void setWeakReference(boolean weakReference) {
		this.weakReference = weakReference;
	}
	
	public String getOppositeName() {
		return oppositeName;
	}

	public void setOppositeName(String oppositeName) {
		this.oppositeName = oppositeName;
	}

	public MMetaclassReference getTarget() {
		return target;
	}

	public void setTarget(MMetaclassReference target) {
		this.target = target;
	}
	
	public MMetaclassDependency getOppositeDependency() {
		return oppositeDependency;
	}

	public void setOppositeDependency(MMetaclassDependency oppositeDependency) {
		this.oppositeDependency = oppositeDependency;
	}

}
