package rpc.over.mqtt;

import org.apache.commons.cli.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rpc.common.GUI;
import rpc.common.JsonService;
import rpc.common.RestRpc;
import rpc.common.StudentBasicApi;
import rpc.over.mqtt.jsonrpc.JsonRpcClient;
import rpc.over.mqtt.jsonrpc.JsonRpcServer;
import rpc.over.mqtt.jsonrpc.MyJacksonJsonRpcMapper;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Sync RPC implementation with Json-RPC protocol over MQTT.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.28
 */
public class Mosquitto {

    private static java.net.URI URI;
    private static StudentBasicApi studentApi;
    private static String topic;

    public static void main(String[] args) throws URISyntaxException, MalformedURLException {
        Options options = new Options();
        OptionGroup mutualOptions = new OptionGroup();

        Option serviceOption = new Option("s", "service", true, "Starts as service");
        serviceOption.setArgs(1);
        serviceOption.setArgName("URL");
        serviceOption.setType(String.class);
        mutualOptions.addOption(serviceOption);

        Option timeoutOption = new Option("t", "timeout", true, "Set a timeout in ms. Default is 5 s = 5000 ms.");
        timeoutOption.setArgs(1);
        timeoutOption.setArgName("ms");
        timeoutOption.setType(Integer.class);
        mutualOptions.addOption(timeoutOption);
        options.addOptionGroup(mutualOptions);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            try {
                URI = new URI(cmd.getArgs()[0]);
                topic = cmd.getArgs()[1];
            } catch (URISyntaxException e) {
                if (cmd.getArgs().length > 2)
                    throw new ParseException(e.getMessage());
                else
                    throw e;
            }
            Mosquitto rpc = new Mosquitto();
            if (cmd.hasOption('s')) {
                URL restApiURL = new URL(cmd.getOptionValue('s'));
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Shutdown received");
                    Runtime.getRuntime().halt(0);
                }));
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(restApiURL.toString())
                        .addConverterFactory(JacksonConverterFactory.create())
                        .build();

                studentApi = retrofit.create(StudentBasicApi.class);
                rpc.proxy();
            } else {
                Integer timeout = null;
                if (cmd.hasOption('t')) {
                    timeout = Integer.valueOf(cmd.getOptionValue('t'));
                    if (timeout <= 0) throw new ParseException("Non-negative timeout expected.");
                }
                rpc.client(timeout);

                System.exit(0);
            }
        } catch (ParseException e) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + e.getMessage());
            usage(formatter, options, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Insufficient arguments provided");
            usage(formatter, options, 1);
        }
    }

    private static void usage(HelpFormatter formatter, Options options, int exitsStatus) {
        formatter.printHelp("URI topic", "An simple example to demo RPC over MQTT", options, null, true);
        System.exit(exitsStatus);
    }


    private void client(Integer timeout) {
        try {
            JsonRpcClient client = timeout == null ?
                    new JsonRpcClient(URI.toString(), new MyJacksonJsonRpcMapper(), topic) :
                    new JsonRpcClient(URI.toString(), new MyJacksonJsonRpcMapper(), topic, timeout);
            JsonService service = client.createProxy(JsonService.class);

            GUI gui = new GUI(service);
            gui.start();

            client.disconnectClient();
            client.closeClientAndExit();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void proxy() {

        try {
            JsonRpcServer server = new JsonRpcServer(URI.toString(), topic, JsonService.class, new RestRpc(studentApi), new MyJacksonJsonRpcMapper());
            server.mainloop();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
