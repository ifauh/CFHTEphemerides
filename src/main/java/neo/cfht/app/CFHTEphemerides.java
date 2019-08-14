package neo.cfht.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.cfht.models.SmallBodyRequest;
import neo.cfht.requesters.IRequester;
import neo.exceptions.NeoIOException;
import neo.exceptions.NeoProcessingException;

public class CFHTEphemerides {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(CFHTEphemerides.class);

	private CFHTEphemeridesConfiguration cec;

	public CFHTEphemerides(CFHTEphemeridesConfiguration cec) {
		this.cec = cec;
	}

	private void run() throws NeoProcessingException {
		List<IRequester> requesters = new ArrayList<>();
		logger.debug("Creating requesters");
		for (String designation : this.cec.getDesignations()) {
			requesters.addAll(IRequester.getRequesters(new SmallBodyRequest(designation, this.cec)));
		}
		try {
			ExecutorService executorService = Executors.newFixedThreadPool(this.cec.getThreadsCounts());
			logger.debug("Submitting {} jobs", requesters.size());
			for (IRequester requester : requesters) {
				executorService.submit(requester);
			}
			executorService.shutdown();
			logger.info("Will wait no more than 5 minutes for all requests to be answered", requesters.size());
			executorService.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			logger.error("Exception caught while executing threads: {}", e.getMessage(), e);
			throw new NeoProcessingException(e);
		}
		for (IRequester requester : requesters) {
			if (requester.isRequestSuccessful()) {
				try {
					requester.write();
				} catch (NeoIOException e) {
					logger.error("Could not write {}: {}", requester.getOutputFileName(), e.getMessage(), e);
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			CFHTEphemeridesConfiguration cec = CFHTEphemeridesConfiguration.parse(args);
			CFHTEphemerides ce = new CFHTEphemerides(cec);
			ce.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
