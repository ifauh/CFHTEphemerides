package neo.cfht.models;

import neo.models.astronomy.Declination;
import neo.models.astronomy.RightAscension;

public class CFHTFormatter {
	public static String raForCFHT(double ra) {
		return RightAscension.fromDegrees(ra).hmsMPC(2).replaceAll(" ", ":");
	}

	public static String deForCFHT(double de) {
		return Declination.fromDegrees(de).dmsMPC(1).replaceAll(" ", ":");
	}
}
