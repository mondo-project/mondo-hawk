package org.hawk.backend.tests;

import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * JUnit 4 test rule that changes logging so it will only report errors.
 * This is needed so we won't hit the 4MB log size limit from Travis and
 * have the job terminated.
 */
public class LogbackOnlyErrorsRule extends ExternalResource {
	private Level oldLevel;
	private Logger logger;

	@Override
	protected void before() throws Throwable {
		logger = (Logger) LoggerFactory.getLogger("org.hawk");
		oldLevel = logger.getLevel();
		logger.setLevel(Level.ERROR);
	}

	@Override
	protected void after() {
		logger.setLevel(oldLevel);
	}
}
