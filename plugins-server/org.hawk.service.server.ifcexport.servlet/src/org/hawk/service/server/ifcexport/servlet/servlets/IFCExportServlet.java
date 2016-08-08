package org.hawk.service.server.ifcexport.servlet.servlets;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServlet;
import org.hawk.service.api.IFCExport;
import org.hawk.service.api.IFCExportJob;
import org.hawk.service.api.IFCExportOptions;
import org.hawk.service.api.IFCExport.Iface;
import org.hawk.service.server.ifcexport.servlet.config.IFCExportManager;
import org.hawk.service.server.ifcexport.servlet.config.IFCExportRequest;

public class IFCExportServlet extends TServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 9L;

	private static class IFCImportIface implements Iface {

		@Override
		public IFCExportJob exportAsSTEP(String hawkInstance, IFCExportOptions options) throws TException {
			IFCExportManager export_manager = IFCExportManager.getInstance();
			IFCExportJob job = export_manager.postRequest(new IFCExportRequest(hawkInstance, options));
			return job;
		}

		@Override
		public List<IFCExportJob> getJobs() throws TException {
			ArrayList<IFCExportJob> result = new ArrayList<>();
			result.addAll(IFCExportManager.getInstance().getJobs());
			return result;
		}

		@Override
		public IFCExportJob getJobStatus(String jobID) throws TException {
			return IFCExportManager.getInstance().getJobStatus(jobID);
		}

		@Override
		public boolean killJob(String jobID) throws TException {
			return IFCExportManager.getInstance().killJob(jobID);
		}

	}
	
	public IFCExportServlet() throws Exception{
		super(new IFCExport.Processor<IFCExport.Iface>(new IFCImportIface()), new TCompactProtocol.Factory());
	}
}
