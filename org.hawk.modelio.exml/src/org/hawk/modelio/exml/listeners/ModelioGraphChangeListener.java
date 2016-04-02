package org.hawk.modelio.exml.listeners;

import org.hawk.core.IModelIndexer;
import org.hawk.core.VcsCommitItem;
import org.hawk.core.graph.IGraphChangeListener;
import org.hawk.core.graph.IGraphNode;
import org.hawk.core.model.IHawkClass;
import org.hawk.core.model.IHawkObject;
import org.hawk.core.model.IHawkPackage;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.modelio.exml.metamodel.ModelioClass;

public class ModelioGraphChangeListener implements IGraphChangeListener {

	private IModelIndexer modelIndexer;

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public void setModelIndexer(IModelIndexer m) {
		this.modelIndexer = m;
	}

	@Override
	public void synchroniseStart() {
		// nothing to do
	}

	@Override
	public void synchroniseEnd() {
		// nothing to do
	}

	@Override
	public void changeStart() {
		// nothing to do
	}

	@Override
	public void changeSuccess() {
		// nothing to do
	}

	@Override
	public void changeFailure() {
		// nothing to do
	}

	@Override
	public void metamodelAddition(IHawkPackage pkg, IGraphNode pkgNode) {
		// nothing to do
	}

	@Override
	public void classAddition(IHawkClass cls, IGraphNode clsNode) {
		if (cls instanceof ModelioClass) {
			// Used to get back an EMF-compatible containment reference based on .exml PID container references
			modelIndexer.addDerivedAttribute(cls.getPackageNSURI(), cls.getName(), ModelioClass.REF_CHILDREN,
				ModelioClass.REF_PARENT_MCLASS, true, true, false, EOLQueryEngine.TYPE,
				"return self.eContents;");
		}
	}

	@Override
	public void fileAddition(VcsCommitItem s, IGraphNode fileNode) {
		// nothing to do
	}

	@Override
	public void fileRemoval(VcsCommitItem s, IGraphNode fileNode) {
		// nothing to do
	}

	@Override
	public void modelElementAddition(VcsCommitItem s, IHawkObject element, IGraphNode elementNode,
			boolean isTransient) {
		// nothing to do
	}

	@Override
	public void modelElementRemoval(VcsCommitItem s, IGraphNode elementNode, boolean isTransient) {
		// nothing to do
	}

	@Override
	public void modelElementAttributeUpdate(VcsCommitItem s, IHawkObject eObject, String attrName, Object oldValue,
			Object newValue, IGraphNode elementNode, boolean isTransient) {
		// nothing to do
	}

	@Override
	public void modelElementAttributeRemoval(VcsCommitItem s, IHawkObject eObject, String attrName,
			IGraphNode elementNode, boolean isTransient) {
		// nothing to do
	}

	@Override
	public void referenceAddition(VcsCommitItem s, IGraphNode source, IGraphNode destination, String edgelabel, boolean isTransient) {
		// nothing to do
	}

	@Override
	public void referenceRemoval(VcsCommitItem s, IGraphNode source, IGraphNode destination, String edgelabel, boolean isTransient) {
		// nothing to do
	}

}
