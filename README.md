mondo-hawk
==========

Public repository for the prototype of Hawk scalable model indexing system

--------------------------------------------------------------------------

Eclipse Luna:

https://eclipse.org/downloads/index-developer.php 

(need: Eclipse Modeling Tools distribution (as EMF is required))

Required eclipse plugins not provided:

Epsilon, eclipse update site:

http://download.eclipse.org/epsilon/interim/

(need: Epsilon Core / Epsilon Core Development Tools )

Neo4J(v2) [due to incompatible licences], download:

http://neo4j.com/download/other-releases/

(extract the .jar files from the zip file into the neo4j dependencies plugin -- and ensure they are re-exported)

[optional] SVNKit, eclipse update site:

http://eclipse.svnkit.com/1.8.x

[optional] Mondix, github project:

https://github.com/FTSRG/mondo-mondix

[optional] Modelio, download:

(download the latest version and import the (existing) projects into eclipse)

http://forge.modelio.org/projects/modelio3-development-app/files

--------------------------------------------------------------------------

Screencast of running Hawk through its UI:

download and configuration of Hawk onto a fresh Eclipse Luna (Modeling Tools) distribution:

https://www.youtube.com/watch?v=d_DqR-0v_4s

running of basic operations:

https://www.youtube.com/watch?v=hQbkA0jmBTY

use of advanced features:

https://www.youtube.com/watch?v=pGL2-lJ0HAg

note: in the interest of simplicity only the required plugins are used in this demo:

org.hawk.core

org.hawk.emf

org.hawk.epsilon

org.hawk.graph

org.hawk.localfolder

org.hawk.neo4j-v2

org.hawk.neo4j-v2.dependencies

org.hawk.ui.emc.dt2

org.hawk.ui2

--------------------------------------------------------------------------
