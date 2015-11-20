package org.hawk.core;

public interface IStateListener {
	enum HawkState {
		RUNNING, UPDATING, STOPPED
	};

	void state(HawkState state);
	void info(String s);
	void error(String s);
}
