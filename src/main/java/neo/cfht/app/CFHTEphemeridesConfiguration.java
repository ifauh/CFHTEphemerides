package neo.cfht.app;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import neo.exceptions.NeoIOException;
import neo.exceptions.NeoInitializationException;
import neo.exceptions.NeoSerializationException;
import neo.logging.NeoLogging;
import neo.timing.NeoZoneId;
import neo.utils.UtilsInternet;
import neo.utils.UtilsOs;
import neo.utils.UtilsResources;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command( name = "CFHTEphemerides",
	description = "Download ephemerides for 'tonight' from the MPC or JPL web service and" + 
			" generate XML input files for the CFHT. NEOCP candidates and known objects are managed.\n"
			+ "'tonight' is is the current UT day if it is before 5am HST, the next day if after.\n"
			+ "\n"
			+ "Note: Ephemerides are requested from JPL Scout (resp. MPC) for the NEOCP objects (resp. known objects).\n")
public class CFHTEphemeridesConfiguration {
	/** Logging */
	private final static Logger logger = LoggerFactory.getLogger(CFHTEphemeridesConfiguration.class);

	@Option( names = { "-d", "-debug", "--debug" }, 
			description = "Verbose debug mode", 
			required = false)
	private boolean debug;
	public boolean isDebug() {
		return this.debug;
	}
	
	@Option( names = {"-h", "-help", "--help"},
			description = "Display this help and exit",
			required = false)
	private boolean help;

    public static final String VERSION = CFHTEphemeridesConfiguration.class.getPackage().getImplementationVersion();
	@Option( names = {"-V", "-version", "--version"}, 
			versionHelp = true,
			description = "Display the version and exit",
			required = false)
	private boolean displayVersion;

	@Option( names = {"-bypassVersionCheck", "--bypassVersionCheck"}, 
			description = "Bypass the version check",
			required = false)
	private boolean bypassVersionCheck;
	
	@Option( names = {"-threads", "--threads"},
			description = "Set the maximum number of threads (default: ${DEFAULT-VALUE})",
			defaultValue = "2", /* if larger, Scout fails :-( */
			required = false)
	private int threadsCounts;
	public int getThreadsCounts() {
		return this.threadsCounts;
	}
	
	@Option( names = {"-useLogfile", "--useLogfile"},
			description = "Write all log messages to a log file",
			required = false)
	private boolean useLogfile;
	
	@Option( names = {"-date", "--date"},
			description = "[MPC|JPL] Use a different date than '${DEFAULT-VALUE}'",
			defaultValue = "tonight (UT)",
			required = false)
	private String date;
	public String getDate() {
		return this.date;
	}
	
	@Option( names = {"-i", "-interval", "--interval"},
			description = "[MPC|JPL] Interval in minutes between each position (default: ${DEFAULT-VALUE} minutes)",
			defaultValue = "30",
			required = false)
	private int intervalMinutes;
	public int getIntervalMinutes() {
		return this.intervalMinutes;
	}
	
	@Option( names = {"-c", "-count", "--count"},
			description = "[MPC only] Number of positions to be requested from MPC.\n" + 
					"     Note: Only visible (in terms of local horizon) ephemerides are displayed. \n" + 
					"       But(!), the number of ephemerides being requested from MPC doesn't \n" + 
					"       take this into account.\n" + 
					"       Increase that number if you don't get any ephemeris. Maximum 100, default: ${DEFAULT-VALUE}",
			defaultValue = "30",
			required = false)
	private int positionsCount;
	public int getPositionsCount() {
		return this.positionsCount;
	}
	
	@Option( names = {"-e", "-east", "--east"},
			description = "[JPL only] Defines the offset in degrees (default: ${DEFAULT-VALUE}) to add to RA to translate the observation point.\n" +
					"         RA values are replaced by RA + raSign * <eastOffset>/cosine(DEC)",
			defaultValue = "0.",
			required = false)
	private double eastOffset;
	
	@Option( names = {"-n", "-north", "--north"},
			description = "[JPL only] Defines the offset in degrees (default: ${DEFAULT-VALUE}) to add to DEC to translate the observation point\n" + 
					"         DEC values are replaced by DEC + <northOffset>",
			defaultValue = "0.",
			required = false)
	private double northOffset;
	
	@Option( names = {"-s", "-swapSign", "--swapSign"},
			description = "[JPL only] Swaps the sign of the RA offset \n" + 
					"         Default: ${DEFAULT-VALUE}, raSign = +1,\n" +
					"         If -swapsign is set, raSign = -1",
			required = false)
	private boolean swapSign;

	@Option( names = {"-p", "-path", "--path"},
			description = "[MPC|JPL] Output directory (default: ${DEFAULT-VALUE})",
			defaultValue = "out",
			required = false)
	private Path outputDirectory;
	public Path getOutputDirectory() {
		return this.outputDirectory;
	}
	
	@Option( names = {"-o", "-observatory", "--observatory"},
			description = "[MPC|JPL] MPC observatory code. Default: ${DEFAULT-VALUE}. Warning! Untested for telescopes different than CFHT",
			defaultValue = "568",
			required = false)
	private String observatory;
	public String getObservatory() {
		return this.observatory;
	}
	
	@Option( names = { "-norbits", "--norbits" }, 
			description = "[JPL only] Number of sampled orbits. Default: ${DEFAULT-VALUE}",
			defaultValue = "100",
			required = false)
	private int nOrbits;
	public int getNOrbits() {
		return this.nOrbits;
	}
	
	@Parameters(index = "0..*",
			description = "List of designations (NEOCP candidates or known objects or mix of those).\n" +
					"*** It is extremely important to write the designation between single \n" + 
					"*** quotes (e.g. 'P/2013 R3', '2014 AA', 'P109isj', '(87890)', ...) to prevent \n" + 
					"*** special characters from shell interpretation.")
	private List<String> designations;
	public List<String> getDesignations() {
		return this.designations;
	}
	
	private String cfhtTemplateFormat;
	public String getCfhtXML(List<String> ephemerides) {
		return String.format(this.cfhtTemplateFormat, 
				String.join("\n", ephemerides));
	}
	
	private CFHTEphemeridesConfiguration() {
		logger.trace("No compiler warning");
		this.designations = new ArrayList<>();
	}

	public static CFHTEphemeridesConfiguration parse(String ... arguments) throws NeoInitializationException {
		CFHTEphemeridesConfiguration cec = CommandLine.populateCommand(new CFHTEphemeridesConfiguration(), arguments);
		// Version
		if (cec.displayVersion) {
			System.out.println("Version " + VERSION);
			System.exit(0);
			return null;
		}
		if (cec.help) {
			new CommandLine(new CFHTEphemeridesConfiguration()).usage(System.out);
			System.exit(0);
			return null;
		}
		if (!cec.checkVersion()) {
			if (!cec.bypassVersionCheck) {
				System.err.println("!".repeat(80));
				System.err.println("!");
				System.err.println("! You are not using the last version of this software (which is " + VERSION +")");
				System.err.println("!");
				System.err.println("!");
				System.err.println("! Either download the last version at https://neo.ifa.hawaii.edu/users/cfht/CFHTEphemerides-last.jar");
				System.err.println("! Or append the -bypassVersionCheck sflag");
				System.err.println("!");
				System.err.println("!".repeat(80));
				System.exit(2);
			} else {
				logger.warn("Not using the last version of this software");
			}
		}
		Level level = cec.debug?Level.DEBUG:Level.INFO;
		String logFileName = "/dev/stdout";
		if (cec.useLogfile) {
			logFileName = "cfht_ephemerides-" +VERSION + "-" + Instant.now().toString() + ".log";
			logger.info("Logging to {}", logFileName);
		}
		NeoLogging.log2file(logFileName, level);
		logger.debug("Called with arguments: [{}]", String.join(" ", arguments));
		logger.debug("Arguments array: {}", (Object[]) arguments);
		cec.initialize();
		return cec;
	}

	private boolean checkVersion() throws NeoInitializationException {
		try {
			String currentVersion = new String(UtilsInternet.download(
					new URL("https://neo.ifa.hawaii.edu/users/cfht/VERSION"))).trim();
			if (this.debug) {
				logger.debug("Current version: [{}], software version: [{}]", currentVersion, VERSION);
			}
			return VERSION.equals(currentVersion);
		} catch (NeoIOException | MalformedURLException e) {
			throw new NeoInitializationException(e);
		}
	}

	private void initialize() throws NeoInitializationException {
		try {
			UtilsOs.mkdirs(this.outputDirectory);
		} catch (NeoIOException e) {
			logger.error("Cannot create output directory [{}]", this.outputDirectory);
			throw new NeoInitializationException(e);
		}
		// Filter out empty designations
		logger.debug("Checking if any designation is empty");
		this.designations = this.designations.stream()
				.filter(designation -> !designation.trim().isEmpty())
				.collect(Collectors.toList());
		if (this.designations.isEmpty()) {
			throw new NeoInitializationException("At least one designation is needed. Try using option -h for help");
		}
		logger.debug("Checking -count value");
		if ( (this.positionsCount<0) || (this.positionsCount>100) ) {
			throw new NeoInitializationException("-count value outside the [0:100] range: " + this.positionsCount);
		}
		logger.info("Ephemerides requested for {} (UT)", this.date);
		if ("tonight (UT)".equals(this.date)) {
			this.date = ZonedDateTime.now(NeoZoneId.UTC).toString().replaceAll("T.*", "");
			ZonedDateTime hstZDT = ZonedDateTime.now(NeoZoneId.HST);
			String hstDate = hstZDT.toString().replaceAll("T.*", "");
			if (hstDate.equals(this.date) && (hstZDT.getHour()>=5) ) {
				logger.debug("After 5am HST: Requesting data for tomorrow HST");
				this.date = ZonedDateTime.now(NeoZoneId.UTC).plusDays(1).toString().replaceAll("T.*", "");
			} else {
				logger.debug("Before 5am HST: Requesting data for today HST");
			}
			logger.info("... that is: {} (UT)", this.date);
		}
		// Load the header / footer templates
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(UtilsResources.getResourceAsString("/cfht_template_header.xml"));
			sb.append("%s\n");
			sb.append(UtilsResources.getResourceAsString("/cfht_template_footer.xml"));
			this.cfhtTemplateFormat = sb.toString(); 
		} catch (NeoSerializationException e) {
			throw new NeoInitializationException(e);
		}
	}

}
