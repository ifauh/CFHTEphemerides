package neo.cfht.models;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import neo.models.astronomy.Declination;
import neo.models.astronomy.RightAscension;
import neo.models.time.Epoch;

public class Ephemeris {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(Ephemeris.class);

	private Epoch epoch;
	private RightAscension ra;
	private Declination de;
	private String mpcLine;
	
	public JsonObject toJSON() {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("mjd", BigDecimal.valueOf(this.epoch.getMJDUT()).setScale(12, RoundingMode.HALF_UP));
		jsonObject.addProperty("ra", BigDecimal.valueOf(this.ra.degrees()).setScale(7, RoundingMode.HALF_UP));
		jsonObject.addProperty("dec", BigDecimal.valueOf(this.de.degrees()).setScale(7, RoundingMode.HALF_UP));
		return jsonObject;
	}

	public static Ephemeris buildMPC(String mpcLine) {
		logger.debug("MPC line: [{}]", mpcLine);
		Ephemeris ephemeris = new Ephemeris();
		ephemeris.mpcLine = mpcLine;
		// Date part
		int year = Integer.parseInt(mpcLine.substring(0, 4));
		int month = Integer.parseInt(mpcLine.substring(5, 5+2));
		int day = Integer.parseInt(mpcLine.substring(8, 8+2));
		int hour = Integer.parseInt(mpcLine.substring(11, 11+2));
		int minute = Integer.parseInt(mpcLine.substring(13, 13+2));
		double dday = (double) day + ((double) hour)/24. + ((double) minute)/1440.;
		ephemeris.epoch = Epoch.createFromMPCUTDate(year, month, dday);
		// RA
		ephemeris.ra = RightAscension.fromRightAscensionHMS(mpcLine.substring(18, 24+4), " ");
		// De
		ephemeris.de = Declination.fromDeclinationDMS(mpcLine.substring(29, 36+2), " ");
		return ephemeris;
	}

	public static Ephemeris buildJPL(String time, double raDeg, double deDeg) {
		Ephemeris ephemeris = new Ephemeris();
		logger.debug("JPL time: [{}]", time.trim());
		int year = Integer.parseInt(time.substring(0, 4));
		int month = Integer.parseInt(time.substring(5, 5+2));
		int day = Integer.parseInt(time.substring(8, 8+2));
		int hour = Integer.parseInt(time.substring(11, 11+2));
		int minute = Integer.parseInt(time.substring(14, 14+2));
		int second = Integer.parseInt(time.substring(17, 17+2));
		double dday = (double) day + ((double) hour)/24. + ((double) minute)/1440. + ((double) second)/86400.;
		ephemeris.epoch = Epoch.createFromMPCUTDate(year, month, dday);
		ephemeris.ra = RightAscension.fromDegrees(raDeg);
		ephemeris.de = Declination.fromDegrees(deDeg);
		return ephemeris;
	}
	
	public String buildXMLEphemerisLine() {
		StringBuilder sb = new StringBuilder();
		// Date part
		sb.append(this.mpcLine.substring(0, 4));
		sb.append("-");
		sb.append(this.mpcLine.substring(5, 5+2));
		sb.append("-");
		sb.append(this.mpcLine.substring(8, 8+2));
		sb.append(" ");
		sb.append(this.mpcLine.substring(11, 11+2));
		sb.append(":");
		sb.append(this.mpcLine.substring(13, 13+2));
		sb.append(":00|");
		// RA
		sb.append(this.mpcLine.substring(18, 18+2));
		sb.append(":");
		sb.append(this.mpcLine.substring(21, 21+2));
		sb.append(":");
		sb.append(this.mpcLine.substring(24, 24+4));
		sb.append("0|");
		// De
		sb.append(this.mpcLine.substring(29, 29+3));
		sb.append(":");
		sb.append(this.mpcLine.substring(33, 33+2));
		sb.append(":");
		sb.append(this.mpcLine.substring(36, 36+2));
		sb.append(".0|");
		return sb.toString();
	}
}
