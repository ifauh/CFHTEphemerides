package neo.cfht.requesters;

public class MPCCommon {

	public static String buildEphemeris(String line) {
		StringBuilder sb = new StringBuilder();
		// Date part
		sb.append(line.substring(0, 4));
		sb.append("-");
		sb.append(line.substring(5, 5+2));
		sb.append("-");
		sb.append(line.substring(8, 8+2));
		sb.append(" ");
		sb.append(line.substring(11, 11+2));
		sb.append(":");
		sb.append(line.substring(13, 13+2));
		sb.append(":00|");
		// RA
		sb.append(line.substring(18, 18+2));
		sb.append(":");
		sb.append(line.substring(21, 21+2));
		sb.append(":");
		sb.append(line.substring(24, 24+4));
		sb.append("0|");
		// De
		sb.append(line.substring(29, 29+3));
		sb.append(":");
		sb.append(line.substring(33, 33+2));
		sb.append(":");
		sb.append(line.substring(36, 36+2));
		sb.append(".0|");
		return sb.toString();
	}

}
