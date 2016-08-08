package org.hawk.service.server.users.cli;

import java.io.Console;
import java.net.ConnectException;

import org.apache.thrift.TException;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.hawk.service.api.UserNotFound;
import org.hawk.service.api.UserProfile;
import org.hawk.service.api.Users;
import org.hawk.service.api.utils.APIUtils;
import org.hawk.service.api.utils.APIUtils.ThriftProtocol;

public class UsersCommandProvider implements CommandProvider {
	private Users.Client client;

	public Object _usersHelp(CommandInterpreter intp) {
		return getHelp();
	}

	/* CONNECTION HANDLING */

	public Object _usersConnect(CommandInterpreter intp) throws Exception {
		final String url = requiredArgument(intp, "url");

		final String username = intp.nextArgument();
		String password = intp.nextArgument();
		if (username != null && password == null) {
			Console console = System.console();
			if (console == null) {
				throw new Exception("No console: cannot read password safely");
			}

			console.writer().print("Password: ");
			password = String.valueOf(console.readPassword());
		}

		client = APIUtils.connectTo(Users.Client.class, url, ThriftProtocol.JSON, username, password);
		return null;
	}

	public Object _usersDisconnect(CommandInterpreter intp) throws Exception {
		if (client != null) {
			client.getInputProtocol().getTransport().close();
			client = null;
			return "Connection closed";
		}
		else {
			return "Connection already closed";
		}
	}

	/* USER MANAGEMENT */

	public Object _usersAdd(CommandInterpreter intp) throws Exception {
		checkConnected();
		final String username = requiredArgument(intp, "username");
		final String realName = requiredArgument(intp, "realname");
		final boolean isAdmin = Boolean.valueOf(requiredArgument(intp, "isAdmin"));
		String password = intp.nextArgument();

		if (password == null) {
			Console console = System.console();
			if (console == null) {
				throw new Exception("No console: cannot read password safely");
			}

			String repeat;
			do {
				console.writer().print("Password for new user: ");
				password = String.valueOf(console.readPassword());
				console.writer().print("Repeat password for new user: ");
				repeat = String.valueOf(console.readPassword());

				if (!password.equals(repeat)) {
					console.writer().println("Passwords do not match.");
				} else {
					break;
				}
			} while (true);
		}

		UserProfile profile = new UserProfile();
		profile.setAdmin(isAdmin);
		profile.setRealName(realName);
		client.createUser(username, password, profile);

		return "Created user account " + username;
	}

	public Object _usersUpdateProfile(CommandInterpreter intp) throws UserNotFound, TException, ConnectException {
		checkConnected();
		final String username = requiredArgument(intp, "username");
		final String realName = requiredArgument(intp, "realname");
		final boolean isAdmin = Boolean.valueOf(requiredArgument(intp, "isAdmin"));

		UserProfile profile = new UserProfile();
		profile.setAdmin(isAdmin);
		profile.setRealName(realName);
		client.updateProfile(username, profile);

		return "Updated profile for user account " + username;
	}

	public Object _usersUpdatePassword(CommandInterpreter intp) throws Exception {
		checkConnected();
		final String username = requiredArgument(intp, "username");
		String password = intp.nextArgument();
		if (password == null) {
			Console console = System.console();
			if (console == null) {
				throw new Exception("No console: cannot read password safely");
			}

			console.writer().print("Password: ");
			password = String.valueOf(console.readPassword());
		}

		client.updatePassword(username, password);
		return "Updated password for user account " + username;
	}

	public Object _usersRemove(CommandInterpreter intp) throws UserNotFound, TException, ConnectException {
		checkConnected();
		final String username = requiredArgument(intp, "username");
		client.deleteUser(username);
		return "Removed user account " + username;
	}

	/* HELP */

	@Override
	public String getHelp() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("---User management (commands are case insensitive)---\n\t");
		sbuf.append("usersHelp - lists all the available commands for Users\n");
		sbuf.append("--Connections--\n\t");
		sbuf.append("usersConnect <url> [username] [password] - connects to a Thrift endpoint\n\t");
		sbuf.append("usersDisconnect - disconnects from the current Thrift endpoint\n");
		sbuf.append("--Commands--\n\t");
		sbuf.append("usersAdd <username> <realname> <isAdmin: true|false> [password] - adds the user to the database\n\t");
		sbuf.append("usersUpdateProfile <username> <realname> <isAdmin: true|false> - changes the personal information of a user\n\t");
		sbuf.append("usersUpdatePassword <username> [password] - changes the password of a user\n\t");
		sbuf.append("usersRemove <username> - removes a user\n\t");
		sbuf.append("usersCheck <username> [password] - validates credentials\n");
		return sbuf.toString();
	}

	/**
	 * Ensures that a connection has been established.
	 * @throws ConnectException No connection has been established yet.
	 * @see #_hawkConnect(CommandInterpreter)
	 * @see #_hawkDisconnect(CommandInterpreter)
	 */
	private void checkConnected() throws ConnectException {
		if (client == null) {
			throw new ConnectException("Please connect to a Thrift endpoint first!");
		}
	}

	/**
	 * Reads an expected argument from the interpreter.
	 * @throws IllegalArgumentException The argument has not been provided.
	 */
	private String requiredArgument(CommandInterpreter intp, String argumentName) {
		String value = intp.nextArgument();
		if (value == null) {
			throw new IllegalArgumentException(
				String.format("Required argument '%s' has not been provided", argumentName));
		}
		return value;
	}

}
