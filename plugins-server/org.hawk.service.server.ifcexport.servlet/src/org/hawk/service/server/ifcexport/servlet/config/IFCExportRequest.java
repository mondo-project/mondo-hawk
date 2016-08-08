/*******************************************************************************
 * Copyright (c) 2015-2016 University of York.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ran Wei - initial API and implementation
 *    Antonio Garcia-Dominguez - cleanup
 *******************************************************************************/
package org.hawk.service.server.ifcexport.servlet.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawk.service.api.IFCExportOptions;

public class IFCExportRequest {
	
	protected String hawkInstance;
	protected IFCExportOptions exportOptions;
	
	public IFCExportRequest(String hawkInstance, IFCExportOptions exportOptions) {
		this.hawkInstance = hawkInstance;
		this.exportOptions = exportOptions;
	}
	
	public String getHawkInstance() {
		return hawkInstance;
	}
	
	public IFCExportOptions getExportOptions() {
		return exportOptions;
	}
	
	public String getRepositoryPattern()
	{
		return exportOptions.getRepositoryPattern();
	}
	
	public List<String> getFilePatterns()
	{
		return exportOptions.getFilePatterns();
	}
	
	public Map<String,Map<String,Set<String>>> getIncludeRules()
	{
		return exportOptions.getIncludeRules();
	}
	
	public Map<String,Map<String,Set<String>>> getExcludeRules()
	{
		return exportOptions.getExcludeRules();
	}
}
