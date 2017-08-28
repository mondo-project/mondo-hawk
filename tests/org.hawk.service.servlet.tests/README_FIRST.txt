1. Copy The following to Debug/Run Configuration-->Arguments-->VM Arguments
"
-Dorg.osgi.service.http.port=8080 
-Dorg.osgi.service.http.port.secure=8443
-Dosgi.noShutdown=true  
-Xmx4g -Xms2g -XX:+UseG1GC -XX:+UseStringDeduplication
-Dorg.eclipse.equinox.http.jetty.customizer.class=org.hawk.service.server.gzip.Customizer 
-Dorg.eclipse.update.reconcile=false
-Dartemis.security.enabled=false
-Dhawk.artemis.host=localhost
-Dhawk.artemis.listenAll=false
-Dhawk.artemis.sslEnabled=false
-Dhawk.tcp.port=2080 
-Dhawk.tcp.thriftProtocol=TUPLE
"

2. Run/Debug As JUnit Plug-in Test