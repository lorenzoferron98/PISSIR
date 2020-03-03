package rpc.over.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.tools.jsonrpc.JacksonJsonRpcMapper;
import com.rabbitmq.tools.jsonrpc.JsonRpcClient;
import com.rabbitmq.tools.jsonrpc.JsonRpcServer;
import org.apache.commons.cli.*;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rpc.common.GUI;
import rpc.common.JsonService;
import rpc.common.RestRpc;
import rpc.common.StudentBasicApi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

/**
 * Sync RPC implementation with Json-RPC protocol over AMQP.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.23
 */
public class RabbitMQ {

    private static final String QUEUE_NAME = "Json-RPC";
    private static final int RPC_TIMEOUT_ONE_SECOND = 1000;

    private static java.net.URI URI;

    private static StudentBasicApi studentApi;

    /**
     * Entry point to demo service and REST.
     * Proxy service starts first.
     *
     * @param args contains URI
     */
    public static void main(String[] args) throws URISyntaxException, MalformedURLException {
        Options options = new Options();

        Option serviceOption = new Option("s", "service", true, "Starts as service");
        serviceOption.setArgs(1);
        serviceOption.setArgName("URL");
        serviceOption.setType(String.class);
        options.addOption(serviceOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            try {
                URI = new URI(cmd.getArgs()[0]);
            } catch (URISyntaxException e) {
                if (cmd.getArgs().length == 1)
                    throw e;
                else
                    throw new ParseException(e.getMessage());
            }
            RabbitMQ rpc = new RabbitMQ();

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
                rpc.client();
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
        formatter.printHelp("URI", "An simple example to demo RPC over AMQP", options, null, true);
        System.exit(exitsStatus);
    }

    private void client() {
        ConnectionFactory cfconn = new ConnectionFactory();
        try {
            cfconn.setUri(URI);
            try (Connection conn = cfconn.newConnection()) {
                Channel ch = conn.createChannel();
                JsonRpcClient client = new JsonRpcClient(ch, "", QUEUE_NAME, RPC_TIMEOUT_ONE_SECOND, new JacksonJsonRpcMapper());
                JsonService service = client.createProxy(JsonService.class);

                GUI gui = new GUI(service);
                gui.start();
            }
        } catch (TimeoutException e) {
            System.err.println("Can't connect to the service before timeout.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void proxy() {
        ConnectionFactory connFactory = new ConnectionFactory();
        try {
            connFactory.setUri(URI);
            try (Connection conn = connFactory.newConnection()) {
                final Channel ch = conn.createChannel();

                ch.queueDeclare(QUEUE_NAME, false, false, false, null);
                JsonRpcServer server = new JsonRpcServer(ch, QUEUE_NAME, JsonService.class, new RestRpc(studentApi), new JacksonJsonRpcMapper());

                System.out.println("Proxy service started");
                server.mainloop();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
