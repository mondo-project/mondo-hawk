mondo-hawk
==========

Public repository for the prototype of the Hawk scalable model indexing system.

Dependencies
------------

* [Eclipse Luna Modeling Tools distribution](https://eclipse.org/downloads/index-developer.php)
* [Epsilon (interim update site)](http://download.eclipse.org/epsilon/interim/) (need: Epsilon Core / Epsilon Core Development Tools)
* [Neo4J(v2) (due to incompatible licences)](http://neo4j.com/download/other-releases/)
* (optional) [SVNKit update site](http://eclipse.svnkit.com/1.8.x)
* (optional) [Mondix Github project](https://github.com/FTSRG/mondo-mondix)
* (optional) [Modelio source code](https://opensourceprojects.eu/svn/p/mondo/code/trunk/modelio), download the latest version and import the (existing) projects into Eclipse.

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
2. Clone the [MONDIX repository](https://github.com/FTSRG/mondo-mondix) and import the `eu.mondo.mondix.interfaces` project into the workspace.
3. Import the following projects from [the MONDO Modelio 3.2 SVN repository](https://opensourceprojects.eu/svn/p/mondo/code/trunk/modelio) using the "SVN Repositories" view in the "SVN Repository Exploring" perspective:
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
    * core.utils
    * jdbm
    * RCPTARGET/ktable
    * lib
    * log
    * mda.infra
    * metamodel.emfapi
    * metamodel.implementation
    * model.search.engine
    * RCPTARGET/nebula
    * RCPTARGET/nebula-incubator
    * RCPTARGET/nebula-nattable
    * practicalxml
    * script.engine
    * ui
    * RCPTARGET/uml2_3.2
    * vaudit
    * xmi (you'll need to edit its `META-INF/MANIFEST.MF` file and remove its dependency on `org.modelio.model.browser`)
4. Open the `org.hawk.targetplatform/org.hawk.targetplatform.target` file, wait for the target definition to be resolved and click on `Set as Target Platform`.
5. Close the `_hawk_runtime_example` project (it is an example project running Hawk in standalone mode) if you do not need it.
6. Close the experimental `org.hawk.epsilon.queryaware` project if you do not need it. If you do, you'll need to import the `StaticAnalysis` projects within [epsilonlabs](https://github.com/epsilonlabs/epsilonlabs) and the [effectivemetamodel](https://github.com/wrwei/org.eclipse.epsilon.labs.effectivemetamodel) project.
# Download Neo4J 2.0.1 (see above link, or try these quick links: [Linux/Mac](http://info.neotechnology.com/download-thanks.html?edition=community&release=2.0.1&flavour=unix), [Windows 64 bits](http://info.neotechnology.com/download-thanks.html?edition=community&release=2.0.1&architecture=x64), [Windows 32 bits](http://info.neotechnology.com/download-thanks.html?edition=community&release=2.0.1&architecture=x32)), unpack the ZIP file and place all the `.jar` files in `lib` into `org.hawk.neo4j-v2.dependencies/2.0.1`. The libraries should appear within `Referenced Libraries`, and they should be marked to be reexported (Project menu > Properties > Java Build Path > Order and Export).
7. Force a full rebuild with `Project > Clean... > Clean all projects` if you still have errors.

After all these steps, you should have a working version of Hawk with all optional dependencies and no errors.
