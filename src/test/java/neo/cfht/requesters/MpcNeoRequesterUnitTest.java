package neo.cfht.requesters;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.cfht.app.CFHTEphemeridesConfiguration;
import neo.cfht.models.SmallBodyRequest;

public class MpcNeoRequesterUnitTest {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(MpcNeoRequesterUnitTest.class);

	// This doesn't test much beside giving an idea on how to use the command line
	@Test
	public void test20190814() throws Exception {
		CFHTEphemeridesConfiguration cec = CFHTEphemeridesConfiguration.parse("A10fwNJ", "-d", "-bypassVersionCheck");
		SmallBodyRequest smallBodyRequest = new SmallBodyRequest("A10fwNJ", cec);
		MpcNeoRequester mpcNeoRequester = new MpcNeoRequester(smallBodyRequest);
		mpcNeoRequester.call();
		logger.trace("{}", mpcNeoRequester.getCfhtXML());
	}
}
