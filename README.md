mondo-hawk
==========

Public repository for the prototype of the Hawk scalable model indexing system.

[![Travis status](https://api.travis-ci.org/mondo-project/mondo-hawk.svg?branch=master)](https://travis-ci.org/mondo-project/mondo-hawk)

The core components of Hawk and the OrientDB backend can be installed from this update site:

    http://mondo-project.github.io/mondo-hawk/updates/

Dependencies
------------

* [Eclipse Luna Modeling Tools distribution](https://eclipse.org/downloads/index-developer.php)
* [Epsilon (interim update site)](http://download.eclipse.org/epsilon/interim/) (need: Epsilon Core / Epsilon Core Development Tools / Epsilon EMF Development Tools)
* (optional) [OrientDB](http://orientdb.com/)
* (optional) [Neo4J(v2)](http://neo4j.com/download/other-releases/)
* (optional) [SVNKit update site](http://eclipse.svnkit.com/1.8.x)
* (optional) [Modelio source code](https://opensourceprojects.eu/svn/p/mondo/code/trunk/modelio).
* (optional) [BIMserver 1.4.0 (also due to imcompatible licenses)](http://bimserver.org/)

Screencasts
-----------

* [Download and configuration of Hawk onto a fresh Eclipse Luna (Modeling Tools) distribution](https://www.youtube.com/watch?v=d_DqR-0v_4s)
* [Running of basic operations](https://www.youtube.com/watch?v=hQbkA0jmBTY)
* [Use of advanced features](https://www.youtube.com/watch?v=pGL2-lJ0HAg)

*Note*: in the interest of simplicity, only the required plugins are used in this demo:

* org.hawk.core
* org.hawk.emf
* org.hawk.epsilon
* org.hawk.graph
* org.hawk.localfolder
* org.hawk.neo4j-v2
* org.hawk.neo4j-v2.dependencies
* org.hawk.ui.emc.dt2
* org.hawk.ui2

Running from source
-------------------

These instructions are from a clean download of an Eclipse Luna Modelling distribution and include all optional dependencies.

1. Clone this Git repository on your Eclipse instance (e.g. using `git clone` or EGit) and import all projects into the workspace (File > Import > Existing Projects into Workspace).
2. Import the following projects from [the MONDO Modelio 3.2 SVN repository](https://opensourceprojects.eu/svn/p/mondo/code/trunk/modelio) using the "SVN Repositories" view in the "SVN Repository Exploring" perspective (you'll need the "Subversive SVN" client from the Eclipse Marketplace):
    * api
    * app.core
    * app.preferences
    * app.project.core
    * core.help
    * core.kernel
    * core.project
    * core.project.data
    * core.session
    * core.estore.exml
    * core.ui
    * core.utils (edit its `META-INF/MANIFEST.MF` file so it only exports its own packages)
    * jdbm (you'll need to create its "src" source folder)
    * lib
    * log
    * mda.infra
    * metamodel.emfapi
    * metamodel.implementation
    * model.search.engine
    * practicalxml (you'll need to create its "src" source folder)
    * RCPTARGET/ktable
    * RCPTARGET/nebula
    * RCPTARGET/nebula-incubator
    * RCPTARGET/nebula-nattable
    * RCPTARGET/uml2_3.2
    * script.engine
    * ui
    * vaudit
    * xmi (edit its `META-INF/MANIFEST.MF` file, removing its dependency on `org.modelio.model.browser` and exporting the `org.modelio.xmi.generation` package)
4. Open the `org.hawk.targetplatform/org.hawk.targetplatform.target` file, wait for the target definition to be resolved and click on `Set as Target Platform`.
5. Close the `_hawk_runtime_example` project (it is an example project running Hawk in standalone mode) if you do not need it.
6. Close the experimental `org.hawk.epsilon.queryaware` project if you do not need it. If you do, you'll need to import the `StaticAnalysis` projects within [epsilonlabs](https://github.com/epsilonlabs/epsilonlabs) and the [effectivemetamodel](https://github.com/wrwei/org.eclipse.epsilon.labs.effectivemetamodel) project.
7. Install [IvyDE](https://ant.apache.org/ivy/ivyde/) into your Eclipse instance, right click on `org.hawk.neo4j-v2.dependencies/2.0.1` and use "Ivy > Retrieve 'dependencies'". The libraries should appear within `Referenced Libraries`.
8. Download the dependencies mentioned in `org.hawk.ifc/lib/README.txt`.
9. Force a full rebuild with `Project > Clean... > Clean all projects` if you still have errors.
10. To build the update site, run first `org.hawk.updatesite/Build Site.launch`, wait for the "PDE Export" background job to complete, refresh `org.hawk.updatesite` and then run `Publish Site.launch`. A zip file named `hawk-updatesite-YYYYMMDD.zip` will be generated in the same directory.

After all these steps, you should have a working version of Hawk with all optional dependencies and no errors.

Meta-level queries in Hawk
--------------------------

Hawk extends the regular EOL facilities to be able to query the metamodels registered within the instance:

* `Model.types` lists all the types registered in Hawk (`EClass` instances for EMF).
* `Model.metamodels` lists all the metamodels registered in Hawk (`EPackage` instances for EMF).
* `Model.getTypeOf(obj)` retrieves the type of the object `obj`.

For a metamodel `mm`, these attributes are available:

* `mm.uri` is the namespace URI of the metamodel.
* `mm.metamodelType` is the type of metamodel that was registered.
* `mm.dependencies` lists the metamodels this metamodel depends on (usually at least the Ecore metamodel for EMF-based metamodels).
* `mm.types` lists the types defined in this metamodel.
* `mm.resource` retrieves the original string representation for this metamodel (the original `.ecore` file for EMF).

For a type `t`, these attributes are available:

* `t.metamodel` retrieves the metamodel that defines the type.
* `t.all` retrieves all instances of that type efficiently (includes subtypes).
* `t.name` retrieves the name of the type.
* `t.attributes` lists the attributes of the type, as slots (see below).
* `t.references` lists the references of the type, as slots.
* `t.features` lists the attributes and references of the type.

For a slot `sl`, these attributes are available:

* `sl.name`: name of the slot.
* `sl.type`: type of the value of the slot.
* `sl.isMany`: true if this is a multi-valued slot.
* `sl.isOrdered`: true if the values should follow some order.
* `sl.isAttribute`: true if this is an attribute slot.
* `sl.isReference`: true if this is a reference slot.
* `sl.isUnique`: true if the value for this slot should be unique within its model.

Acknowledgments
---------------

![YourKit logo](yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler. YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/index.jsp), innovative and intelligent tools for profiling Java and .NET applications.
