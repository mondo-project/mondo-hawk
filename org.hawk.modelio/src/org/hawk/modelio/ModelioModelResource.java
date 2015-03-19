package org.hawk.modelio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;


import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.internal.resource.UMLResourceFactoryImpl;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.modelio.gproject.data.project.DefinitionScope;
import org.modelio.gproject.data.project.FragmentType;
import org.modelio.gproject.data.project.ProjectDescriptor;
import org.modelio.gproject.data.project.ProjectDescriptorReader;
import org.modelio.gproject.fragment.IProjectFragment;
import org.modelio.gproject.fragment.exml.ExmlFragment;
import org.modelio.gproject.gproject.GProject;
import org.modelio.gproject.gproject.GProjectFactory;
import org.modelio.gproject.model.MModelServices;
import org.modelio.gproject.module.catalog.FileModuleStore;
import org.modelio.metamodel.data.MetamodelLoader;
import org.modelio.metamodel.mda.Project;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vbasic.auth.NoneAuthData;
import org.modelio.vbasic.progress.NullProgress;
import org.modelio.vcore.smkernel.mapi.MObject;
import org.modelio.xmi.generation.ExportServices;
import org.modelio.xmi.generation.GenerationProperties;

import org.hawk.core.model.*;
import org.hawk.core.IModelResourceFactory;;

public class ModelioModelResource implements IHawkModelResource {

	File modelioProj;
	private IModelResourceFactory parser;

	@Override
	public void unload() {
		modelioProj = null;
	}
	
	public ModelioModelResource(File f, IModelResourceFactory p) {
		parser = p;
		modelioProj = f;
	}

	@Override
	public Iterator<IHawkObject> getAllContents() {
		return getAllContentsSet().iterator();
	}

	@Override
	public HashSet<IHawkObject> getAllContentsSet() {

		HashSet<IHawkObject> allElements = new HashSet<IHawkObject>();
		
		Model modelioElements = null;
		
		try {
			modelioElements = getModel(modelioProj.getCanonicalPath());
		} catch (IOException e) {

		}
		
		if (modelioElements != null){
			EList<Element> all = modelioElements.allOwnedElements();
			
			for (Element e:all){
				allElements.add(new ModelioObject(e));
			}
		}
		
		return allElements;

	}
	
	private Model getModel(String path){
       //Load a Modelio project 
		Model m = null;
        
        //Start by loading the Modelio Metamodel
        MetamodelLoader.Load();

        //Load the description of a given project
        //Path p = Paths.get("/home/shah/modelio/workspace/Project2/project.conf");
        Path p = Paths.get("/media/titan-data/Hawk/uk.ac.york.cs.mde.hawk.modelio/samples/Project2/project.conf");
       // Path p = Paths.get(path);
        System.err.println("Path is: "+ path);

        ProjectDescriptor pd;
        GProject gp =null;
		try {
			pd = new ProjectDescriptorReader().read(p, DefinitionScope.LOCAL);


			//Load the project
			Path fms = Paths.get(System.getProperty("java.io.tmpdir")+"/.modelio/3.2/modules");
			//@SuppressWarnings("deprecation")
			gp = (GProject) GProjectFactory.openProject(pd, new NoneAuthData(), new FileModuleStore(fms), new NullProgress());


        //Navigate through the modelio model
        for ( IProjectFragment ipf :  gp.getOwnFragments()){
            if(ipf.getType().equals(FragmentType.EXML)){

                ExmlFragment ef = (ExmlFragment)ipf;

                Iterator<MObject> iterator = ef.doGetRoots().iterator(); 
                Project o = (Project) iterator.next();
                
                //Get the Model package of the first fragment
                Package entryPoint = o.getModel();

                //Specify the XMI file
               // File file = new File("/tmp/export.xmi");

                
                // XMI Export

                    //Initiate the generation properties
                    GenerationProperties genProp = GenerationProperties.getInstance();
                    genProp.initialize(new MModelServices(gp));
                    genProp.setTimeDisplayerActivated(false);
                    genProp.setSelectedPackage(entryPoint);
                   // genProp.setFilePath(file);
                    
                    genProp.setSelectedPackage(entryPoint);
                   // genProp.setRoundtripEnabled(false);

                    
                    //Create a EMF resource
                   // Resource resource = createResource(file.getAbsolutePath());
                     
                    //XMI Export
                    ExportServices exportService = new ExportServices();
                    //exportService.createEcoreModel(resource, null);
                    m= exportService.createEcoreModel(entryPoint, null);
               

            }
        }
		} catch (IOException e1) {

		}finally{
			if (gp !=null)
			gp.close();
		}
		return m;
	}

	@Override
	public String getType() {
		return parser.getType();
	}
	@Override
	public int getSignature(IHawkObject o) {
		return o.hashCode();
	}
    private static Resource createResource(final String resourcePath) {
        File file = new File(resourcePath);

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
               // Xmi.LOG.error(e);
            }
        }

        ResourceSet resourceSet2 = new ResourceSetImpl();

        // Register the default resource factory -- only needed for
        // stand-alone!

        //resourceSet2.getPackageRegistry().put(org.eclipse.uml2.uml.UMLPackage.eNS_URI,        UMLPackage.eINSTANCE);

        resourceSet2.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
        Map<URI, URI> uriMap = resourceSet2.getURIConverter().getURIMap();

        //URI uri = URI.createURI("jar:file:/D:/work/phoenix/toolkit/modeliotool/plugins.org.eclipse.uml2.uml.resources_org.eclipse.uml2.uml.resources_3.1.100.v201008191510.jar!/"); // for example
        URI uri = URI.createURI("jar:file:/media/titan-data/experiments-parsers/modelio/modelio3.1-src/RCPTARGET/uml2_3.2/plugins/org.eclipse.uml2.uml.resources_3.1.100.v201008191510.jar!/");
        uriMap.put(URI.createURI(UMLResource.LIBRARIES_PATHMAP), uri.appendSegment("libraries").appendSegment(""));
        uriMap.put(URI.createURI(UMLResource.METAMODELS_PATHMAP), uri.appendSegment("metamodels").appendSegment(""));
        uriMap.put(URI.createURI(UMLResource.PROFILES_PATHMAP), uri.appendSegment("profiles").appendSegment(""));
        resourceSet2.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put(Resource.Factory.Registry.DEFAULT_EXTENSION,
                new UMLResourceFactoryImpl());

        // Get the URI of the model file.

        URI fileURI2 = URI.createFileURI(file.getAbsolutePath());
        Resource resource2 = resourceSet2.createResource(fileURI2);
       
        return resource2;
    }
	
}
