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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.cfht.app.CFHTEphemeridesConfiguration;
import neo.cfht.models.Ephemeris;
import neo.cfht.models.SmallBodyRequest;
import neo.utils.UtilsFiles;

public class MpcKnownRequester implements IRequester {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(MpcKnownRequester.class);

	public static final String FORMAT_MPC_KNOWN_URL = "https://www.minorplanetcenter.net/cgi-bin/mpeph2.cgi?"
			+ "ty=e&TextArea=%s&d=%s&l=%d&i=%d&u=m&uto=0&c=%s&long=&lat=&alt=&raty=a&s=t&m=m&igd=y&ibh=y"
			+ "&adir=S&oed=&e=-2&resoc=&tit=&bu=&ch=c&ce=f&js=f";

	private static Pattern DATE_MATCH = Pattern.compile("^2\\d\\d\\d \\d\\d \\d\\d.*$");
	
	private static final String SUFFIX = "knompc";
	@Override
	public String getSuffix() {
		return MpcKnownRequester.SUFFIX;
	}
	
	private SmallBodyRequest smallBodyRequest;
	@Override
	public SmallBodyRequest getSmallBodyRequest() {
		return this.smallBodyRequest;
	}
	
	private boolean requestSuccessful;
	private List<String> cfhtEphemeridesLine;
	private List<Ephemeris> ephemerides;
	@Override
	public List<Ephemeris> getEphemerides() {
		return this.ephemerides;
	}
	
	private String outputFileNameXML;
	private String outputFileNameJSON;

	@Override
	public boolean isRequestSuccessful() {
		return this.requestSuccessful;
	}

	public MpcKnownRequester(SmallBodyRequest smallBodyRequest) {
		logger.trace("No compiler warning");
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
					.uri(URI.create(String.format(FORMAT_MPC_KNOWN_URL, 
							encodedDesignation,
							cec.getDate(),
							cec.getPositionsCount(),
							cec.getIntervalMinutes(),
							cec.getObservatory())))
					.build();
			logger.debug("Sending request for {}", this.smallBodyRequest.getDesignation());
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			logger.debug("Got response for {}", this.smallBodyRequest.getDesignation());
			String body = response.body();
			Path mpcResponsePath = this.smallBodyRequest.getCFHTEphemeridesConfiguration()
					.getOutputDirectory().resolve(String.format("%s-known-%s.mpc-response",
							CFHTEphemeridesConfiguration.PREFIX_TROUBLE,
							this.smallBodyRequest.getDesignation()));
			logger.info("Saving MPC HTTP response to {}", mpcResponsePath);
			UtilsFiles.saveWithBackup(mpcResponsePath, body);
			this.requestSuccessful = true;
			this.cfhtEphemeridesLine = new ArrayList<>();
			this.ephemerides = new ArrayList<>();
			for (String line : body.split("\n")) {
				if (DATE_MATCH.matcher(line).matches()) {
					Ephemeris ephemeris = Ephemeris.buildMPC(line);
					this.cfhtEphemeridesLine.add(ephemeris.buildXMLEphemerisLine());
					this.ephemerides.add(ephemeris);
				}
			}
			if (this.cfhtEphemeridesLine.isEmpty()) {
				logger.info("MPC Known Ephemerides Service doesn't seem to know [{}] (this might be expected)", 
						this.smallBodyRequest.getDesignation());
				this.requestSuccessful = false;
			}
		} catch (Exception e) {
			if (this.smallBodyRequest.getCFHTEphemeridesConfiguration().isDebug()) {
				logger.error("Exception caught while executing MpcKnownRequester for object [{}]: {}", 
						this.smallBodyRequest.getDesignation(), e.getMessage(), e);
			} else {
				logger.warn("Exception caught while executing MpcKnownRequester for object [{}] (this might be expected): {}",
						this.smallBodyRequest.getDesignation(), e.getMessage());
				logger.info("If you think that it is an issue, execute the same command line with '-d -useLogfile' "
						+ "and send the generated log to someone who can help");
			}
			logger.info("Marking request to MPC mpeph2 as failed");
			this.requestSuccessful = false;
		}
		return this;
	}

	@Override
	public String getCfhtXML() {
		return this.smallBodyRequest.getCFHTEphemeridesConfiguration().getCfhtXML(this.cfhtEphemeridesLine);
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
