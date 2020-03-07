package it.uniupo;

import com.google.gson.Gson;
import it.uniupo.descriptors.Environment;
import it.uniupo.descriptors.Executor;
import it.uniupo.mqttv3.Mode;
import it.uniupo.mqttv3.MqttV3Executor;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Main class.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.18
 */
public class App {

    private final static String PARENT_LEVEL = "/f3RR0n";

    private static boolean verbosity;
    private static Environment env;
    private static String hostURI;

    public static void start(String[] resources, boolean isVerbose) throws IOException, BrokenBarrierException, InterruptedException {
        verbosity = isVerbose;
        if (!loadResources(resources)) return;
        int nExecutors = env.getConfigs().stream().mapToInt(Executor::getSize).sum();
        CyclicBarrier gate = new CyclicBarrier(nExecutors + 1);
        List<Thread> batch = new ArrayList<>(nExecutors);
        List<List<Data>> results = new ArrayList<>();
        for (int i = 0; i < env.getRunCount(); i++) {
            List<Data> localResults = new CopyOnWriteArrayList<>();
            for (Executor config : env.getConfigs()) {
                int bound = config.getSize();
                IntStream.range(0, bound).mapToObj(j -> new Thread(() -> {
                    MqttV3Executor v3Executor = new MqttV3Executor(config.getQos(), PARENT_LEVEL + config.getTopic(), config.getPayload(), env.getTimeout(), hostURI);
                    v3Executor.execute(gate);
                    localResults.add(v3Executor.getResults());
                })).forEachOrdered(t -> {
                    batch.add(t);
                    t.start();
                });
            }
            gate.await();
            for (Thread t : batch) t.join();
            results.add(localResults);
        }
        // print results
        printResults(results);
    }

    private static void printResults(List<List<Data>> results) {
        long totSendMsg = 0;
        long totRcvMsg = 0;
        long totLostMsg = 0;
        Map<Integer, Double> totAverageRTT = new HashMap<>();
        double totAverageElapsedTime = 0;
        double totSpeedRate = 0;
        int nQos = results.get(0).stream().filter(data -> data.getMode() == Mode.PUB).collect(Collectors.groupingBy(Data::getQos)).size();
        int offset = 25 * nQos;
        System.out.printf("%-30s %-30s %-30s %-" + (offset + nQos - 1) + ".30s %-30.30s %-30.30s %n%n", "sendMsg", "rcvMsg", "lostMsg*", "averageRTT (ms)", "averageElapsedTime (s)", "speedRate (msg/s)");
        for (List<Data> run : results) {
            long sendMsg = run.stream().mapToLong(Data::getSendMsg).sum();
            totSendMsg += sendMsg;
            long rcvMsg = run.stream().mapToLong(Data::getRcvMsg).sum();
            totRcvMsg += rcvMsg;
            long lostMsg = 0;
            for (Map.Entry<String, Long> entry : run.stream().collect(Collectors.groupingBy(Data::getTopic, Collectors.summingLong(Data::getSendMsg))).entrySet()) {
                String topic = entry.getKey();
                Long sum = entry.getValue();
                lostMsg += sum * run.stream().filter(foo -> foo.getTopic().equals(topic) && foo.getMode() == Mode.SUB).count() - run.stream().filter(foo -> foo.getTopic().equals(topic) && foo.getMode() == Mode.SUB).mapToLong(Data::getRcvMsg).sum();
            }
            totLostMsg += lostMsg;
            Map<Integer, Double> averageRTT = run.stream().filter(foo -> foo.getMode() == Mode.PUB).collect(Collectors.groupingBy(Data::getQos, Collectors.averagingDouble(Data::getAverageRTT)));
            averageRTT.forEach((key, value) -> totAverageRTT.merge(key, value, (v1, v2) -> totAverageRTT.get(key) + value));
            double averageElapsedTime = run.stream().mapToDouble(Data::getElapsedTime).average().orElse(0) / 1000;
            totAverageElapsedTime += averageElapsedTime;
            double speedRate = rcvMsg / averageElapsedTime;
            totSpeedRate += speedRate;
            if (verbosity) {
                System.out.printf("%-30d %-30d %-30d ", sendMsg, rcvMsg, lostMsg);
                averageRTT.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> System.out.printf("%-25.20s ", "QoS " + e.getKey() + ": " + e.getValue()));
                System.out.printf("%-30.10f %-30.10f %n", averageElapsedTime, speedRate);
            }
        }
        if (verbosity)
            System.out.println("\n" + String.join("", Collections.nCopies(30 * 5 + (offset + nQos - 1), "+")));
        System.out.printf("%-30d %-30d %-30d ", totSendMsg, totRcvMsg, totLostMsg);
        totAverageRTT.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.printf("%-25.20s ", "QoS " + e.getKey() + ": " + e.getValue() / results.size()));
        System.out.printf("%-30.10f %-30.10f %n", totAverageElapsedTime / results.size(), totSpeedRate / results.size());
        System.out.println();
    }

    private static boolean loadResources(String[] resources) throws IOException {
        File jsonEnv = null;
        try {
            jsonEnv = new File(resources[0]);
        } catch (ArrayIndexOutOfBoundsException ignored) {}
        if (jsonEnv == null || !jsonEnv.exists() || jsonEnv.isDirectory()) {
            String workingDir = System.getProperty("user.dir");
            System.out.println("File not found. Saving sample.json in " + workingDir);
            try (InputStream in = App.class.getResourceAsStream("/sample.json")) {
                Files.copy(in, Paths.get(workingDir + File.separator + "sample.json"), StandardCopyOption.REPLACE_EXISTING);
            }
            return false;
        }
        Gson gson = new Gson();
        try (BufferedReader br = Files.newBufferedReader(jsonEnv.toPath());
             InputStream in = App.class.getResourceAsStream("/schema.json")) {
            br.mark(1);

            JSONObject jsonSubject = new JSONObject(new JSONTokener(br));
            JSONObject jsonSchema = new JSONObject(new JSONTokener(in));

            Schema schema = SchemaLoader.load(jsonSchema);
            schema.validate(jsonSubject);

            br.reset();
            env = gson.fromJson(br, Environment.class);
        }
        hostURI = resources[1];
        return true;
    }

}
