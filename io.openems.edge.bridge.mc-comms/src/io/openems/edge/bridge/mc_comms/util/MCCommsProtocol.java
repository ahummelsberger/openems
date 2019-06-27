package io.openems.edge.bridge.mc_comms.util;

import io.openems.edge.bridge.mc_comms.api.task.AbstractMCCommsTask;
import io.openems.edge.bridge.mc_comms.api.task.ReadMCCommsTask;
import io.openems.edge.bridge.mc_comms.api.task.WriteMCCommsTask;
import io.openems.edge.common.taskmanager.TasksManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MCCommsProtocol {

    private final TasksManager<ReadMCCommsTask> readTaskManager = new TasksManager<>();
    private final TasksManager<WriteMCCommsTask> writeTaskManager = new TasksManager<>();
    private final AtomicReference<AbstractMCCommsComponent> parentComponentAtomicRef;

    public MCCommsProtocol(AtomicReference<AbstractMCCommsComponent> parentComponentAtomicRef, AbstractMCCommsTask... MCCommsTasks) {
        this.parentComponentAtomicRef = parentComponentAtomicRef;
        for (AbstractMCCommsTask task : MCCommsTasks) {
            task.setProtocol(this);
            addTask(task);
        }
    }

    public AtomicReference<AbstractMCCommsComponent> getParentComponentAtomicRef() {
        return parentComponentAtomicRef;
    }

    private synchronized void addTask(AbstractMCCommsTask MCCommsTask) {
        if (MCCommsTask instanceof WriteMCCommsTask) {
            this.writeTaskManager.addTask((WriteMCCommsTask) MCCommsTask);
        }
        if (MCCommsTask instanceof ReadMCCommsTask) {
            this.readTaskManager.addTask((ReadMCCommsTask) MCCommsTask);
        }

    }


    public List<ReadMCCommsTask> getNextReadTasks() {
        return this.readTaskManager.getNextTasks();
    }

    public List<WriteMCCommsTask> getNextWriteTasks() {
        return this.writeTaskManager.getNextTasks();
    }
}
