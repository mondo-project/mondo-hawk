package uk.ac.york.mondo.integration.artemis.server;

import java.util.Set;

import javax.security.cert.X509Certificate;

import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnection;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager2;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;

public class ShiroRealmSecurityManager implements ActiveMQSecurityManager2 {

	@Override
	public boolean validateUser(String user, String pass) {
		if (user == null && pass == null) {
			// This is a valid combination, but it only works from certain connection types
			return true;
		}

		final UsernamePasswordToken token = new UsernamePasswordToken(user, pass);
		try {
			final SecurityManager manager = SecurityUtils.getSecurityManager();
			manager.authenticate(token);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean validateUser(String arg0, String arg1, X509Certificate[] arg2) {
		return validateUser(arg0, arg1);
	}

	@Override
	public boolean validateUserAndRole(String arg0, String arg1, Set<Role> arg2, CheckType arg3) {
		return validateUser(arg0, arg1);
	}

	@Override
	public boolean validateUserAndRole(String arg0, String arg1, Set<Role> arg2, CheckType arg3, String address, RemotingConnection conn) {
		if (conn.getTransportConnection() instanceof InVMConnection) {
			// In-VM connections are always allowed
			return true;
		}
		return validateUser(arg0, arg1);
	}
}
