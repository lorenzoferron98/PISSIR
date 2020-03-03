package it.uniupo.descriptors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Class to represent an environment to run tests.
 *
 * @author Lorenzo Ferron
 * @version 2020.02.18
 */
public class Environment {

    @SerializedName("runCount")
    @Expose(serialize = false)
    private int runCount = 10;

    @SerializedName("timeout")
    @Expose(serialize = false)
    private int timeout = 30;

    @SerializedName("configs")
    @Expose(serialize = false)
    private List<Executor> configs;

    /**
     * No args constructor for use in serialization
     */
    public Environment() {
        //Empty body...
    }

    public Environment(int runCount, int timeout, List<Executor> configs) {
        this.runCount = runCount;
        this.timeout = timeout;
        this.configs = configs;
    }

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public List<Executor> getConfigs() {
        return configs;
    }

    public void setConfigs(List<Executor> configs) {
        this.configs = configs;
    }

}
