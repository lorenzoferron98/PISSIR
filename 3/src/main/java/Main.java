import it.uniupo.App;
import org.apache.commons.cli.*;

/**
 * Entry point. Comments are not our strength.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.18
 */
public class Main {

    public static final byte MAJOR = 1;
    public static final byte MINOR = 0;
    public static final byte PATCH = 0;
    public static final String VERSION = MAJOR + "." + MINOR + "." + PATCH;

    public static final String AUTHORS = "Lorenzo Ferron";

    public static final String NAME = "EnvQTT";
    private static final String DESCRIPTION = "A tool to benchmark a MQTT server";

    public static void main(String[] args) {
        // create the Options
        Options options = new Options();

        Option verbose = Option.builder("V")
                .desc("Show summary param for this test")
                .hasArg(false)
                .longOpt("verbose")
                .numberOfArgs(0)
                .required(false)
                .build();
        options.addOption(verbose);

        Option help = new Option("h", "help", false, "print this message");
        options.addOption(help);

        Option version = new Option("v", "version", false, "print the version information and exit");
        options.addOption(version);

        // create the parser
        CommandLineParser parser = new DefaultParser();
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("help")) usage(formatter, options, 0);
            else if (line.hasOption("version")) {
                System.out.println(NAME + " " + VERSION);
                System.out.println();
                System.out.println("Written by " + AUTHORS);
                System.exit(0);
            }
            boolean isVerbose = line.hasOption("verbose");
            String[] resources = line.getArgs();
            App.start(resources, isVerbose);
            System.exit(0);
        } catch (ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            usage(formatter, options, 1);
        } catch (Exception e) {
            System.err.println("Insufficient arguments provided");
            usage(formatter, options, 1);
        }
    }

    private static void usage(HelpFormatter formatter, Options options, int exitsStatus) {
        formatter.printHelp("ENV_FILE URL", DESCRIPTION, options, null, true);
        System.exit(exitsStatus);
    }

}
