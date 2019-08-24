package neo.cfht.requesters;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.cfht.app.CFHTEphemeridesConfiguration;
import neo.cfht.models.Ephemeris;
import neo.cfht.models.SmallBodyRequest;

/**
 * Request NEO Candidate from JPL Scout 
 * 
 * @author schastel
 *
 */
public class MpcNeoRequester implements IRequester {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(MpcNeoRequester.class);
	public Logger getLogger() {
		return MpcNeoRequester.logger;
	}
	
	public static final String FORMAT_MPC_CONFIRMEPH2_URL = "https://cgi.minorplanetcenter.net/cgi-bin/confirmeph2.cgi";
	private static final String SUFFIX = "neompc";
	@Override
	public String getSuffix() {
		return MpcNeoRequester.SUFFIX;
	}
	
	private SmallBodyRequest smallBodyRequest;
	@Override
	public SmallBodyRequest getSmallBodyRequest() {
		return this.smallBodyRequest;
	}
	
	private List<String> cfhtEphemeridesLines;
	private List<Ephemeris> ephemerides;
	@Override
	public List<Ephemeris> getEphemerides() {
		return this.ephemerides;
	}
	
	private boolean requestSuccessful;

	private String outputFileNameXML;
	private String outputFileNameJSON;
	
	@Override
	public boolean isRequestSuccessful() {
		return this.requestSuccessful;
	}
	
	public MpcNeoRequester(SmallBodyRequest smallBodyRequest) {
		this.smallBodyRequest = smallBodyRequest;
		this.requestSuccessful = false;
	}

	@Override
	public IRequester call() throws Exception {
		logger.debug("Requesting {}", this.smallBodyRequest.getDesignation());
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.POST(buildFormData())
					.uri(URI.create(FORMAT_MPC_CONFIRMEPH2_URL))
					.header("Content-Type", "application/x-www-form-urlencoded")
					.build();
			logger.debug("Sending request for {}", this.smallBodyRequest.getDesignation());
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			String body = response.body();
			logger.debug("Got response for {}: {} bytes", this.smallBodyRequest.getDesignation(),
					body.length());
			this.requestSuccessful = true;
			CFHTEphemeridesConfiguration cec = this.smallBodyRequest.getCFHTEphemeridesConfiguration();
			String datePattern = cec.getDate().replaceAll("-", " ");
			this.cfhtEphemeridesLines = new ArrayList<>();
			this.ephemerides = new ArrayList<>();
			for (String line : body.split("\n")) {
				if (line.startsWith(datePattern)) {
					logger.debug("Data line: [{}]", line);
					Ephemeris ephemeris = Ephemeris.buildMPC(line);
					this.cfhtEphemeridesLines.add(ephemeris.buildXMLEphemerisLine());
					this.ephemerides.add(ephemeris);
				}
			}
			logger.debug("Got {} matches", this.cfhtEphemeridesLines.size());
			if (this.ephemerides.isEmpty()) {
				logger.info("No observation found in MpcNeoRequester. Marking the request as failed");
				this.requestSuccessful = false;
			}
		} catch (Exception e) {
			if (this.smallBodyRequest.getCFHTEphemeridesConfiguration().isDebug()) {
				logger.error("Exception caught while executing MpcNeoRequester for object [{}] (this might be expected): {}",
						this.smallBodyRequest.getDesignation(),
						e.getMessage(), e);
			} else {
				logger.warn("MPC NEOCP doesn't seem to know the NEO Candidate [{}] (this might be expected): {}",
						this.smallBodyRequest.getDesignation(), e.getMessage());
				logger.info("If you think that it is an issue, execute the same command line with '-d -useLogfile' "
						+ "and send the generated log to someone who can help");
			}
			logger.info("Marking request to MPC confirmeph2 as failed");
			this.requestSuccessful = false;
		}
		return this;
	}

	/*
	 * Stolen from https://golb.hplar.ch/2019/01/java-11-http-client.html
	 */
	private BodyPublisher buildFormData() {
		Map<Object, Object> formParameters = new HashMap<>();
		String encodedDesignation = URLEncoder.encode(this.smallBodyRequest.getDesignation(), StandardCharsets.US_ASCII);
		formParameters.put("obj", encodedDesignation);
		CFHTEphemeridesConfiguration cec = this.smallBodyRequest.getCFHTEphemeridesConfiguration();
		formParameters.put("obscode", cec.getObservatory());
		formParameters.put("raty", "a");
		formParameters.put("Parallax", "1");
		formParameters.put("int", "1");
		formParameters.put("mot", "m");
		formParameters.put("sun", "x");
		formParameters.put("W", "j");
		String sParameters = formParameters.entrySet().stream().map(entry -> 
			URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8) 
			+ "="
			+ URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8)
		).collect(Collectors.joining("&"));
		return BodyPublishers.ofString(sParameters);
	}

	@Override
	public String getCfhtXML() {
		return this.smallBodyRequest.getCFHTEphemeridesConfiguration().getCfhtXML(this.cfhtEphemeridesLines);
	}
	
//	@Override
//	public String writeXML() throws NeoIOException {
//		Path outputDirectory = this.smallBodyRequest.getCFHTEphemeridesConfiguration().getOutputDirectory();
//		this.outputFileNameXML = outputDirectory.resolve(this.smallBodyRequest.getDesignation() + "-C000.mpc.xml").toString();
//		UtilsOs.mkdirs(outputDirectory);
//		try (PrintWriter writer = new PrintWriter(this.outputFileNameXML)) {
//			logger.info("Writing output XML file: {}", this.outputFileNameXML);
//			writer.println(getCfhtXML());
//			writer.close();
//		} catch (IOException e) {
//			throw new NeoIOException(e);
//		}
//		return this.outputFileNameXML;
//	}
//
//	@Override
//	public String writeJson() throws NeoIOException {
//		Path outputDirectory = this.smallBodyRequest.getCFHTEphemeridesConfiguration().getOutputDirectory();
//		this.outputFileNameJSON = outputDirectory.resolve(this.smallBodyRequest.getDesignation() + "-C000.mpc.json").toString();
//		UtilsOs.mkdirs(outputDirectory);
//		try (PrintWriter writer = new PrintWriter(this.outputFileNameJSON)) {
//			logger.info("Writing output JSON file: {}", this.outputFileNameJSON);
//			writer.println(getCfhtJSON());
//			writer.close();
//		} catch (IOException e) {
//			throw new NeoIOException(e);
//		}
//		return this.outputFileNameJSON;
//	}
	
	@Override
	public String getOutputFileNameXML() {
		return this.outputFileNameXML;
	}
	@Override
	public void setOutputFileNameXML(String outputFileNameXML) {
		this.outputFileNameXML = outputFileNameXML;
	}
	@Override
	public String getOutputFileNameJSON() {
		return this.outputFileNameJSON;
	}
	@Override
	public void setOutputFileNameJSON(String outputFileNameJSON) {
		this.outputFileNameJSON = outputFileNameJSON;
	}
	
}
