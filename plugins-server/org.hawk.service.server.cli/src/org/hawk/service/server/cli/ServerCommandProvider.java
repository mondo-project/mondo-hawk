package org.hawk.service.server.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.hawk.core.IModelIndexer.ShutdownRequestType;
import org.hawk.osgiserver.HManager;
import org.hawk.osgiserver.HModel;

public class ServerCommandProvider implements CommandProvider {

	public static final String MSERVER_HELP_CMD = "mserverHelp";
	private HManager hawkManager;

	public Object _mserverHelp(CommandInterpreter intp) throws Exception {
		return getHelp();
	}

	/* INSTANCE HANDLING */

	public Object _hawkListInstances(CommandInterpreter intp) throws Exception {
		checkConnected();
		List<HModel> instances = new ArrayList<>(hawkManager.getHawks());
		Collections.sort(instances, new Comparator<HModel>() {
			@Override
			public int compare(HModel o1, HModel o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		if (instances.isEmpty()) {
			return "No instances exist";
		} else {
			StringBuffer sbuf = new StringBuffer();
			for (HModel i : instances) {
				sbuf.append(String.format("%s (%s)\n", i.getName(), i.getStatus().toString()));
			}
			return sbuf.toString();
		}
	}

	private void checkConnected() {
		if (hawkManager == null) {
			hawkManager = HManager.getInstance();
		}
	}

	public Object _hawkRemoveInstance(CommandInterpreter intp) throws Exception {
		checkConnected();
		final boolean hardDeletion = requiredArgument(intp, "mode").toLowerCase().equals("hard"); 
		final String name = requiredArgument(intp, "name");
		HModel hmodel = hawkManager.getHawkByName(name);
		if (hmodel == null) {
			throw new NoSuchElementException("No Hawk instance exists with name '" + name + "'");
		}
		hawkManager.delete(hmodel, hmodel.exists());
		if (hardDeletion) {
			removeRecursive(Paths.get(hmodel.getFolder()));
		}
		return String.format("Removed instance %s (%s)", name, hardDeletion ? "hard deletion" : "soft deletion");
	}

	public Object _hawkStartInstance(CommandInterpreter intp) throws Exception {
		checkConnected();
		final String name = requiredArgument(intp, "name");

		final HModel hi = hawkManager.getHawkByName(name);
		if (hi.isRunning()) {
			return String.format("Instance %s was already running", name);
		} else {
			hi.start(hawkManager);
			return String.format("Started instance %s", name);
		}
	}

	public Object _hawkStopInstance(CommandInterpreter intp) throws Exception {
		checkConnected();
		final String name = requiredArgument(intp, "name");
		final HModel hi = hawkManager.getHawkByName(name);
		if (hi.isRunning()) {
			hi.stop(ShutdownRequestType.ALWAYS);
			return String.format("Stopped instance %s", name);
		} else {
			return String.format("Instance %s was already stopped", name);
		}
	}

	public Object _hawkSyncInstance(CommandInterpreter intp) throws Exception {
		checkConnected();
		final String name = requiredArgument(intp, "name");
		final HModel hi = hawkManager.getHawkByName(name);
		if (hi.isRunning()) {
			hi.sync();
			return String.format("Requested immediate sync on instance %s", name);
		} else {
			return String.format("Instance %s is not running", name);
		}
	}

	@Override
	public String getHelp() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("---MONDO SERVER (commands are case insensitive, <> means required, [] means optional)---\n\t");
		sbuf.append(MSERVER_HELP_CMD + " - lists all the available commands for the MONDO Server\n");
		sbuf.append("--Instances--\n\t");
		sbuf.append("hawkListInstances - lists the available Hawk instances\n\t");
		sbuf.append("hawkRemoveInstance soft|hard <name> - removes an instance with the provided name, if it exists (soft deletion deregisters, hard deletion also removes storage folder)\n\t");
		sbuf.append("hawkStartInstance <name> - starts the instance with the provided name\n\t");
		sbuf.append("hawkStopInstance <name> - stops the instance with the provided name\n\t");
		sbuf.append("hawkSyncInstance <name> - forces an immediate sync on the instance with the provided name\n");
		return sbuf.toString();
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

	private static void removeRecursive(java.nio.file.Path path)
			throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<java.nio.file.Path>() {
			@Override
			public FileVisitResult visitFile(java.nio.file.Path file,
					BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(java.nio.file.Path file,
					IOException exc) throws IOException {
				// try to delete the file anyway, even if its attributes
				// could not be read, since delete-only access is
				// theoretically possible
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(java.nio.file.Path dir,
					IOException exc) throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					// directory iteration failed; propagate exception
					throw exc;
				}
			}
		});
	}
}
