package neo.cfht.requesters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import neo.cfht.models.Ephemeris;
import neo.cfht.models.SmallBodyRequest;
import neo.exceptions.NeoIOException;
import neo.resources.PsNeoResources;
import neo.utils.UtilsOs;

public interface IRequester extends Callable<IRequester> {
	/** Logging */
	final static Logger logger = LoggerFactory.getLogger(IRequester.class);

	boolean isRequestSuccessful();
	
	public static List<IRequester> getRequesters(SmallBodyRequest smallBodyRequest) {
		List<IRequester> requesters = new ArrayList<>();
		requesters.add(new JplNeoRequester(smallBodyRequest));
		requesters.add(new MpcKnownRequester(smallBodyRequest));
		requesters.add(new MpcNeoRequester(smallBodyRequest));
		return requesters;
	}
	
	List<Ephemeris> getEphemerides();
	
	String getCfhtXML();
	
	
	default String getCfhtJSON() {
		JsonArray ephemerisPointsJAB = new JsonArray();
		getEphemerides().forEach(ephemeris -> {
			ephemerisPointsJAB.add(ephemeris.toJSON());
		});
		JsonObject ephemerisPoint = new JsonObject();
		ephemerisPoint.add("ephemeris_point", ephemerisPointsJAB);
		JsonObject jsonObject = new JsonObject();
		jsonObject.add("moving_target", ephemerisPoint);
		return PsNeoResources.toJson(jsonObject,true);
	}

	default void write() throws NeoIOException {
		if (isRequestSuccessful()) {
			writeXML();
			writeJson();
		}
	}

	SmallBodyRequest getSmallBodyRequest();
	String getSuffix();
	default String writeXML() throws NeoIOException {
		SmallBodyRequest smallBodyRequest = getSmallBodyRequest();
		Path outputDirectory = smallBodyRequest.getCFHTEphemeridesConfiguration().getOutputDirectory();
		setOutputFileNameXML(outputDirectory.resolve(
				smallBodyRequest.getNormalizedDesignation() + "-C000." + getSuffix() + ".xml").toString());
		UtilsOs.mkdirs(outputDirectory);
		try (PrintWriter writer = new PrintWriter(getOutputFileNameXML())) {
			logger.info("Writing output XML file: {}", getOutputFileNameXML());
			writer.println(getCfhtXML());
			writer.close();
		} catch (IOException e) {
			throw new NeoIOException(e);
		}
		return getOutputFileNameXML();
	}
	
	default String writeJson() throws NeoIOException {
		SmallBodyRequest smallBodyRequest = getSmallBodyRequest();
		Path outputDirectory = smallBodyRequest.getCFHTEphemeridesConfiguration().getOutputDirectory();
		setOutputFileNameJSON(outputDirectory.resolve(
				smallBodyRequest.getNormalizedDesignation() + "-C000." + getSuffix() + ".json").toString());
		UtilsOs.mkdirs(outputDirectory);
		try (PrintWriter writer = new PrintWriter(getOutputFileNameJSON())) {
			logger.info("Writing output JSON file: {}", getOutputFileNameJSON());
			writer.println(getCfhtJSON());
			writer.close();
		} catch (IOException e) {
			throw new NeoIOException(e);
		}
		return getOutputFileNameJSON();
	}
	
	String getOutputFileNameXML();
	void setOutputFileNameXML(String outputFileNameXML);
	String getOutputFileNameJSON();
	void setOutputFileNameJSON(String outputFileNameJSON);



}