package io.openems.edge.bridge.mc_comms.util;

import com.google.common.collect.Multimap;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.worker.AbstractCycleWorker;
import io.openems.edge.bridge.mc_comms.api.task.ReadMCCommsTask;
import io.openems.edge.bridge.mc_comms.api.task.WriteMCCommsTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class MCCommsWorker extends AbstractCycleWorker {

    private final Logger logger = LoggerFactory.getLogger(MCCommsWorker.class);
    private final Multimap<String, MCCommsProtocol> protocols;

    public MCCommsWorker(Multimap<String, MCCommsProtocol> protocols) {
        this.protocols = protocols;
    }

    @Override
    public void activate(String name) {
        super.activate(name);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void forever() {
        // get the read tasks for this run
        List<ReadMCCommsTask> nextReadTasks = this.getNextReadTasks();

        /*
         * execute next read tasks
         */
        nextReadTasks.forEach(readTask -> {
            /*
             * Execute next read abstractTask
             */
            try {
                readTask.executeQuery();
            } catch (OpenemsException e) {
                logError(logger, readTask.toString() + " read failed: " + e.getMessage());
            }
        });
    }

    /**
     * Log an error message including the Component ID.
     *
     * @param log
     * @param message
     */
    protected void logError(Logger log, String message) {
        log.error(message);
    }

    /**
     * Returns the 'nextReadTasks' list.
     */
    private List<ReadMCCommsTask> getNextReadTasks() {
        List<ReadMCCommsTask> result = new ArrayList<>();
        protocols.values().forEach(protocol -> {
            // get the next read tasks from the protocol
            List<ReadMCCommsTask> nextReadTasks = protocol.getNextReadTasks();
            result.addAll(nextReadTasks);
            // }
        });
        return result;
    }

    private List<WriteMCCommsTask> getNextWriteTasks() {
        List<WriteMCCommsTask> result = new ArrayList<>();
        protocols.values().forEach(protocol -> {
            result.addAll(protocol.getNextWriteTasks());
        });
        return result;
    }
}
