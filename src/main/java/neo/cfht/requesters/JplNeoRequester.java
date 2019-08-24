package neo.cfht.requesters;

import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.cfht.app.CFHTEphemeridesConfiguration;
import neo.cfht.models.CFHTFormatter;
import neo.cfht.models.Ephemeris;
import neo.cfht.models.SmallBodyRequest;
import neo.serialization.json.JsonHelpers;

/**
 * Request NEO Candidate from JPL Scout 
 * 
 * @author schastel
 *
 */
public class JplNeoRequester implements IRequester {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(JplNeoRequester.class);

	public static final String FORMAT_JPL_SCOUT_URL = "https://ssd-api.jpl.nasa.gov/scout.api?"
			+ "tdes=%s&eph-start=%sT04:00:00&eph-stop=%sT18:00:00&eph-step=%sm&obs-code=%s";

	private static final String SUFFIX = "neojpl";
	@Override
	public String getSuffix() {
		return JplNeoRequester.SUFFIX;
	}
	
	private SmallBodyRequest smallBodyRequest;
	@Override
	public SmallBodyRequest getSmallBodyRequest() {
		return this.smallBodyRequest;
	}

	private List<String> cfhtEphemerides;
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
	
	public JplNeoRequester(SmallBodyRequest smallBodyRequest) {
		this.smallBodyRequest = smallBodyRequest;
		this.requestSuccessful = false;
	}

	@Override
	public IRequester call() throws Exception {
		logger.debug("Requesting {}", this.smallBodyRequest.getDesignation());
		try {
			String encodedDesignation = URLEncoder.encode(this.smallBodyRequest.getDesignation(), StandardCharsets.US_ASCII);
			CFHTEphemeridesConfiguration cec = this.smallBodyRequest.getCFHTEphemeridesConfiguration();
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(String.format(FORMAT_JPL_SCOUT_URL, 
							encodedDesignation,
							cec.getDate(), cec.getDate(),
							cec.getIntervalMinutes(),
							cec.getObservatory())))
					.build();
			logger.debug("Sending request for {}", this.smallBodyRequest.getDesignation());
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			logger.debug("Got response for {}", this.smallBodyRequest.getDesignation());
			this.requestSuccessful = true;
			JsonObject jsonObject = JsonHelpers.readObject(new StringReader(response.body()));
			logger.debug("response = {}", JsonHelpers.prettyString(jsonObject));
			this.cfhtEphemerides = new ArrayList<>();
			this.ephemerides = new ArrayList<>();
			for (JsonValue ephemeris : jsonObject.getJsonArray("eph")) {
				JsonObject jEphemeris = JsonObject.class.cast(ephemeris);
				String time = jEphemeris.getString("time");
				JsonObject jMedian = jEphemeris.getJsonObject("median");
				double ra = Double.parseDouble(jMedian.getString("ra"));
				double de = Double.parseDouble(jMedian.getString("dec"));
				logger.debug("{}:{}:{}", time, ra, de);
				this.cfhtEphemerides.add(String.format("%s|%s|%s|", 
						time, CFHTFormatter.raForCFHT(ra), CFHTFormatter.deForCFHT(de)));
				this.ephemerides.add(Ephemeris.buildJPL(time, ra, de));
			}
		} catch (Exception e) {
			if (this.smallBodyRequest.getCFHTEphemeridesConfiguration().isDebug()) {
				logger.error("Exception caught while executing JplNeoRequester for object [{}] (this might be expected): {}",
						this.smallBodyRequest.getDesignation(),
						e.getMessage(), e);
			} else {
				logger.warn("JPL Scout doesn't seem to know the NEO candidate [{}] (this might be expected): {}",
						this.smallBodyRequest.getDesignation(), e.getMessage());
				logger.info("If you think that it is an issue, execute the same command line with '-d -useLogfile' "
						+ "and send the generated log to someone who can help");
			}
			logger.info("Marking request to JPL Scout as failed");
			this.requestSuccessful = false;
		}
		return this;
	}

	@Override
	public String getCfhtXML() {
		return this.smallBodyRequest.getCFHTEphemeridesConfiguration().getCfhtXML(this.cfhtEphemerides);
	}
	
//	@Override
//	public String writeXML() throws NeoIOException {
//		Path outputDirectory = this.smallBodyRequest.getCFHTEphemeridesConfiguration().getOutputDirectory();
//		this.outputFileNameXML = outputDirectory.resolve(this.smallBodyRequest.getDesignation() + "-C000.jpl.xml").toString();
//		UtilsOs.mkdirs(outputDirectory);
//		try (PrintWriter writer 
//				= new PrintWriter(this.outputFileNameXML)) {
//			logger.info("Writing output XML file: {}", this.outputFileNameXML);
//			writer.println(getCfhtXML());
//			writer.close();
//		} catch (IOException e) {
//			throw new NeoIOException(e);
//		}
//		return this.outputFileNameXML;
//	}
	
//	@Override
//	public String writeJson() throws NeoIOException {
//		Path outputDirectory = this.smallBodyRequest.getCFHTEphemeridesConfiguration().getOutputDirectory();
//		this.outputFileNameJSON = outputDirectory.resolve(this.smallBodyRequest.getDesignation() + "-C000.jpl.json").toString();
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
