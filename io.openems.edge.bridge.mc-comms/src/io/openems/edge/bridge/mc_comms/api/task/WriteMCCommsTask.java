package io.openems.edge.bridge.mc_comms.api.task;

import io.openems.edge.bridge.mc_comms.MCCommsBridge;
import io.openems.edge.bridge.mc_comms.api.element.MCCommsElement;
import io.openems.edge.bridge.mc_comms.util.AbstractMCCommsComponent;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsPacket;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.common.taskmanager.ManagedTask;
import io.openems.edge.common.taskmanager.Priority;

import java.nio.ByteBuffer;

/**
 * Task for writing to a slave device
 */
public class WriteMCCommsTask extends AbstractMCCommsTask implements ManagedTask {
    private final boolean ackBeforeWrite;

    /**
     * Constructor
     * @param command write command to be sent
     * @param priority priority of this task
     * @param ackBeforeWrite establish a connection before writing to the serial bus
     * @param elements elements containing values to be written
     */
    WriteMCCommsTask(int command, Priority priority, boolean ackBeforeWrite, MCCommsElement<?>... elements) {
        super(command, priority, elements);
        this.ackBeforeWrite = ackBeforeWrite;
    }

    /**
     * Writes bytes to the serial bus
     * @throws MCCommsException connection error
     */
    @Override
    public void executeQuery() throws MCCommsException {
        MCCommsProtocol protocol = this.getProtocol();
        AbstractMCCommsComponent parentComponent = protocol.getParentComponentAtomicRef().get();
        MCCommsBridge bridge = parentComponent.getMCCommsBridgeAtomicRef().get();
        int slaveAddress = parentComponent.getSlaveAddress();

        //sends connection request before writing if ackBeforeWrite == true
        if (ackBeforeWrite) {
            bridge.getTXPacketQueue().add(new MCCommsPacket(0, bridge.getMasterAddress(), slaveAddress));
            MCCommsPacket replyPacket = parentComponent.getPacket(1, 100);
            if (replyPacket == null) {
                throw new MCCommsException("[MCCOMMS] Could not establish MCComms connection with slave device " + parentComponent.getSlaveAddress());
            }
            ByteBuffer packetBuffer = ByteBuffer.allocate(15);
            for (MCCommsElement<?> element : this.elements) {
                packetBuffer.put(element.getByteBuffer().array(), element.getByteAddress(), element.getNumBytes());
            }
            bridge.getTXPacketQueue().add(new MCCommsPacket(command, bridge.getMasterAddress(), slaveAddress, packetBuffer.array()));

        }
    }
}