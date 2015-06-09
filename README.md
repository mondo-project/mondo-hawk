mondo-hawk
==========

Public repository for the prototype of Hawk scalable model indexing system

Dependencies
------------

* [Eclipse Luna Modeling Tools distribution](https://eclipse.org/downloads/index-developer.php)
* [Epsilon (interim update site)](http://download.eclipse.org/epsilon/interim/) (need: Epsilon Core / Epsilon Core Development Tools)
* [Neo4J(v2) (due to incompatible licences)](http://neo4j.com/download/other-releases/), download 2.0.1 and place `lib/*.jar` into `org.hawk.neo4j-v2.dependencies/2.0.1` and ensure they are re-exported.
* (optional) [SVNKit update site](http://eclipse.svnkit.com/1.8.x)
* (optional) [Mondix Github project](https://github.com/FTSRG/mondo-mondix)
* (optional) [Modelio source code](http://forge.modelio.org/projects/modelio3-development-app/files), download the latest version and import the (existing) projects into Eclipse.

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
