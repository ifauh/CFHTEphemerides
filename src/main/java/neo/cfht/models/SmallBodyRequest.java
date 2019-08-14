package neo.cfht.models;

import neo.cfht.app.CFHTEphemeridesConfiguration;

public class SmallBodyRequest {
	private String designation;
	private CFHTEphemeridesConfiguration cec;

	public SmallBodyRequest(String designation, CFHTEphemeridesConfiguration cec) {
		this.designation = designation;
		this.cec = cec;
	}
	public String getDesignation() {
		return this.designation;
	}
	public CFHTEphemeridesConfiguration getCFHTEphemeridesConfiguration() {
		return this.cec;
	}
}
