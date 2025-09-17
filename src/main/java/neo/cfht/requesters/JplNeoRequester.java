package neo.cfht.requesters;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import neo.cfht.app.CFHTEphemeridesConfiguration;
import neo.cfht.models.CFHTFormatter;
import neo.cfht.models.Ephemeris;
import neo.cfht.models.SmallBodyRequest;
import neo.resources.PsNeoResources;
import neo.utils.UtilsFiles;

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
			+ "tdes=%s&eph-start=%sT04:00:00&eph-stop=%sT18:00:00&eph-step=%sm&obs-code=%s&n-orbits=%d";

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
							cec.getObservatory(),
							cec.getNOrbits())))
					.build();
			logger.debug("Sending request for {}", this.smallBodyRequest.getDesignation());
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			logger.debug("Got response for {}", this.smallBodyRequest.getDesignation());
			this.requestSuccessful = true;
			String body = response.body();
			Path jplResponsePath = this.smallBodyRequest.getCFHTEphemeridesConfiguration()
					.getOutputDirectory().resolve(String.format("send-serge-if-trouble-%s.jpl-response", 
							this.smallBodyRequest.getDesignation()));
			logger.info("Saving JPL HTTP response to {}", jplResponsePath);
			UtilsFiles.saveWithBackup(jplResponsePath, body);
			JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
			logger.debug("response = {}", PsNeoResources.toJson(jsonObject,true));
			this.cfhtEphemerides = new ArrayList<>();
			this.ephemerides = new ArrayList<>();
			JsonArray jaEph = jsonObject.get("eph").getAsJsonArray();
			for (JsonElement ephemeris : jaEph) {
				JsonObject jEphemeris = ephemeris.getAsJsonObject();
				String time = jEphemeris.get("time").getAsString();
				JsonObject jMedian = jEphemeris.get("median").getAsJsonObject();
				double ra = jMedian.get("ra").getAsDouble();
				double de = jMedian.get("dec").getAsDouble();
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