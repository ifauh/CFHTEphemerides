package neo.cfht.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.cfht.app.CFHTEphemeridesConfiguration;

public class SmallBodyRequest {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(SmallBodyRequest.class);

	private String designation;
	private String normalizedDesignation;
	private CFHTEphemeridesConfiguration cec;

	public SmallBodyRequest(String designation, CFHTEphemeridesConfiguration cec) {
		this.designation = designation;
		this.normalizedDesignation = this.designation
				.replaceAll("\\(", "_").replaceAll("\\)", "_").replaceAll("\\s\\s*", "_");
		if (!this.normalizedDesignation.equals(this.designation)) {
			logger.info("After meta-characters replacement, normalized designation of '{}' is '{}'",
					this.designation, this.normalizedDesignation);
		}
		this.cec = cec;
	}
	public String getDesignation() {
		return this.designation;
	}
	public String getNormalizedDesignation() {
		return this.normalizedDesignation;
	}
	public CFHTEphemeridesConfiguration getCFHTEphemeridesConfiguration() {
		return this.cec;
	}
}
