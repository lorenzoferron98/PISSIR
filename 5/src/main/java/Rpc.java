import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.tools.jsonrpc.JacksonJsonRpcMapper;
import com.rabbitmq.tools.jsonrpc.JsonRpcClient;
import com.rabbitmq.tools.jsonrpc.JsonRpcServer;
import okhttp3.OkHttpClient;
import org.apache.commons.cli.*;
import org.eclipse.paho.client.mqttv3.internal.NetworkModuleService;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rpc.JsonService;
import rpc.RestRpc;
import rpc.StudentBasicApi;
import rpc.common.GUI;
import rpc.json.MyJacksonJsonRpcMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A class to start client or service class over AMQP or MQTT.
 *
 * @author Lorenzo Ferron
 * @version 2020.03.04
 */
public class Rpc {

    private static int timeout = 5000;
    private static URI uri;
    private static String requestLocation;
    private static URL apiUrl;

    private static StudentBasicApi studentApi;

    public Rpc() {
        // Empty body ...
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option serviceOption = new Option("s", "service", true, "Starts as service");
        serviceOption.setArgs(1);
        serviceOption.setArgName("URL_API");
        serviceOption.setType(URL.class);
        options.addOption(serviceOption);

        Option timeoutOption = new Option("t", "timeout", true,
                "Set a timeout in ms. Default is " + TimeUnit.MILLISECONDS.toSeconds(timeout) + " s = " + timeout + " ms.");
        timeoutOption.setArgs(1);
        timeoutOption.setArgName("ms");
        timeoutOption.setType(Number.class);
        options.addOption(timeoutOption);

        Option help = new Option("h", "help", false, "Show this help");
        help.setArgs(0);
        options.addOption(help);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption('h')) usage(formatter, options, 0);

            try {
                uri = new URI(cmd.getArgs()[0]);
            } catch (URISyntaxException e) {
                throw new ParseException(e.getMessage());
            }
            requestLocation = cmd.getArgs()[1];
            if (cmd.hasOption('t')) {
                timeout = ((Number) cmd.getParsedOptionValue("t")).intValue();
                if (timeout <= 0) throw new ParseException("Negative timeout provided: " + timeout);
            }
            apiUrl = (URL) cmd.getParsedOptionValue("s");

            Rpc rpc = new Rpc();

            boolean isMqttUri = false;
            try {
                NetworkModuleService.validateURI(uri.toString());
                isMqttUri = true;
            } catch (IllegalArgumentException ignored) {
            }

            if (apiUrl == null) {
                if (isMqttUri) rpc.clientOverMQTT();
                else rpc.clientOverAMQP();
                System.exit(0);
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Shutdown received");
                    Runtime.getRuntime().halt(0);
                }));

                final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .readTimeout(timeout, TimeUnit.MILLISECONDS)
                        .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                        .build();

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(apiUrl.toString())
                        .addConverterFactory(JacksonConverterFactory.create())
                        .client(okHttpClient)
                        .build();
                studentApi = retrofit.create(StudentBasicApi.class);
                if (isMqttUri) rpc.proxyOverMQTT();
                else rpc.proxyOverAMQP();
            }
        } catch (ParseException e) {
            System.err.println("Parsing failed. Reason: " + e.getMessage());
            usage(formatter, options, 1);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Missing arguments");
            usage(formatter, options, 1);
        }
    }

    private static void usage(HelpFormatter formatter, Options options, int exitStatus) {
        formatter.printHelp("broker_URI req_location", "A simple example to demo RPC over AMQP or MQTT", options, null, true);
        System.exit(exitStatus);
    }

    private void proxyOverAMQP() {
        ConnectionFactory connFactory = new ConnectionFactory();
        try {
            connFactory.setUri(uri);
            try (Connection conn = connFactory.newConnection()) {
                final Channel ch = conn.createChannel();
                ch.queueDeclare(requestLocation, false, false, false, null);

                JsonRpcServer proxy = new JsonRpcServer(ch, requestLocation, JsonService.class, new RestRpc(studentApi), new JacksonJsonRpcMapper());

                System.out.println("Proxy service started over AMQP");
                proxy.mainloop();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void proxyOverMQTT() {
        try {
            rpc.json.JsonRpcServer proxy = new rpc.json.JsonRpcServer(uri.toString(), requestLocation, JsonService.class, new RestRpc(studentApi), new MyJacksonJsonRpcMapper());

            System.out.println("Proxy service started over MQTT");
            proxy.mainloop();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void clientOverMQTT() {
        try {
            rpc.json.JsonRpcClient client = new rpc.json.JsonRpcClient(uri.toString(), new MyJacksonJsonRpcMapper(), requestLocation, timeout);
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

    private void clientOverAMQP() {
        ConnectionFactory cfconn = new ConnectionFactory();
        try {
            cfconn.setUri(uri);
            try (Connection conn = cfconn.newConnection()) {
                Channel ch = conn.createChannel();
                JsonRpcClient client = new JsonRpcClient(ch, "", requestLocation, timeout, new JacksonJsonRpcMapper());
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

}
