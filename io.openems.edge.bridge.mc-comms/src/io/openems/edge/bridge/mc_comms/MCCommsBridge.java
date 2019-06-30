package io.openems.edge.bridge.mc_comms;

import com.fazecast.jSerialComm.SerialPort;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.openems.edge.bridge.mc_comms.api.BridgeMCComms;
import io.openems.edge.bridge.mc_comms.util.MCCommsException;
import io.openems.edge.bridge.mc_comms.util.MCCommsPacket;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.bridge.mc_comms.util.MCCommsWorker;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.IntBuffer;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;


@Designate(ocd= Config.class, factory=true)
@Component(name="Bridge.MC-Comms", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)

/**
 * MCCommsBridge: Creates a serial master that can communicate with MCComms slave devices, and interface between slaves
 * and controllers
 */
public class MCCommsBridge extends AbstractOpenemsComponent implements BridgeMCComms, OpenemsComponent {
	
	@Reference
	protected ConfigurationAdmin cm;

	private final Multimap<String, MCCommsProtocol> protocols = Multimaps
			.synchronizedListMultimap(ArrayListMultimap.create());
	private final Logger logger = LoggerFactory.getLogger(MCCommsBridge.class);
	private MCCommsWorker worker = new MCCommsWorker(this.protocols);
	private int masterAddress;
	//serial port atomic reference
	private AtomicReference<SerialPort>	serialPortAtomicRef = new AtomicReference<>();
	//concurrent queues for threads
	private final LinkedBlockingQueue<TimedByte> timedByteQueue = new LinkedBlockingQueue<>();
	private final ConcurrentLinkedQueue<MCCommsPacket> TXPacketQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentHashMap<Integer, LinkedTransferQueue<MCCommsPacket>> transferQueues = new ConcurrentHashMap<>();
	//temporal cache for incoming serial packets
	private final Cache<Long, MCCommsPacket> RXPacketCache = CacheBuilder.newBuilder()
			.expireAfterWrite(Duration.ofMillis(500))
			.maximumSize(1000)
			.build();
	//thread executor
	private final Executor executor = Executors.newFixedThreadPool(3);
	//utility threads for byte/packet handling/picking
	private final PacketBuilder packetBuilder = new PacketBuilder(timedByteQueue, this.RXPacketCache);
	private final SerialByteHandler serialByteHandler  = new SerialByteHandler(this.serialPortAtomicRef, timedByteQueue);
	private final PacketPicker packetPicker = new PacketPicker();
	
	/**
	 * Constructor
	 */
	public MCCommsBridge() {
		super(OpenemsComponent.ChannelId.values());
	}

	/**MCCommsPacketBuffer
	 * Adds the protocol
	 *
	 * @param sourceId The source to which the protocol must be mapped
	 * @param protocol The MCComms protocol to be added
	 */
	@Override
	public void addProtocol(String sourceId, MCCommsProtocol protocol) {
		this.protocols.put(sourceId, protocol);
	}

	/**
	 * Removes the protocol
	 */
	@Override
	public void removeProtocol(String sourceId) {
		this.protocols.removeAll(sourceId);
	}

	/**
	 * @return a Multimap containing all protocols
	 */
	public Multimap<String, MCCommsProtocol> getProtocols() {
		return protocols;
	}

	/**
	 * @return the serial master address of this bridge
	 */
	public int getMasterAddress() {
		return masterAddress;
	}

	/**
	 * Allows component controllers to register a transfer queue used to exchange packets with their respective slave
	 * devices
	 * @param sourceAddress slave address of the device
	 * @param transferQueue transferQueue to be registered
	 */
	public void registerTransferQueue(int sourceAddress, LinkedTransferQueue<MCCommsPacket> transferQueue) {
		transferQueues.put(sourceAddress, transferQueue);
	}

	/**
	 * @return the deque used to queue outgoing packets waiting to be written to the serial bus
	 */
	public ConcurrentLinkedQueue<MCCommsPacket> getTXPacketQueue() {
		return TXPacketQueue;
	}

	@Override
	public String debugLog() {
		String status = "MCComms Bridge status: "; 
		for (boolean b : new boolean[] {this.isEnabled(), serialByteHandler.isAlive(), packetBuilder.isAlive(), packetPicker.isAlive()}) {
			status += (b ? "1" : "0");
		}
		
		return status;
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.masterAddress = config.masterAddress();
		SerialPort serialPort = SerialPort.getCommPort(config.portName());
		serialPort.setComPortParameters(9600, 8, 0, SerialPort.NO_PARITY);
		serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
		serialPort.openPort();
		this.serialPortAtomicRef.set(serialPort);
		//serial byte handler
		if (this.isEnabled()) {
			this.worker.activate(config.id());
		}
		//execution
		//byte reader
		this.executor.execute(serialByteHandler);
		//packet builder
		this.executor.execute(packetBuilder);
		//packet picker
		this.executor.execute(packetPicker);
	}

	@Deactivate
	protected void deactivate() {
		this.worker.deactivate();
		this.serialByteHandler.interrupt();
		this.packetBuilder.interrupt();
		this.packetPicker.interrupt();
		this.serialPortAtomicRef.get().closePort();
		super.deactivate();
	}

	/**
	 * Pulls packets from the read packets cache and puts them in the transferQueue corresponding to their sourceAddress
	 */
	private class PacketPicker extends Thread {
		public void run() {
			while (!isInterrupted()) {
				//clean up cache first to prevent CPU cycles being used on old packets
				RXPacketCache.cleanUp();
				//iterate through queued packets and try assign them to component transfer queues
				RXPacketCache.asMap().forEach((packetNanoTime, packet) -> {
					TransferQueue<MCCommsPacket> correspondingQueue = transferQueues.get(packet.getSourceAddress());
					//if a corresponding queue exists transfer to queue is successful...
					if (correspondingQueue != null && correspondingQueue.tryTransfer(packet)) {
						//...remove packet from cache
						RXPacketCache.invalidate(packetNanoTime);
					}
				});
				try {
					synchronized (packetBuilder) {
						packetBuilder.wait(); //waits on packet builder to actually produce packets
					}
				} catch (InterruptedException e) {
					interrupt();
				}

			}
		}
	}

	/**
	 * waits for bytes to come in from the serial port and attempts to construct valid packets
	 */
	private class PacketBuilder extends Thread {

		private LinkedBlockingQueue<TimedByte> timedByteQueue;
		private Cache<Long, MCCommsPacket> outputCache;

		private PacketBuilder(LinkedBlockingQueue<TimedByte> timedByteQueue, Cache<Long, MCCommsPacket> outputCache) {
			this.timedByteQueue = timedByteQueue;
			this.outputCache = outputCache;
		}

		@Override
		public void run() {
			IntBuffer packetBuffer = IntBuffer.allocate(25);
			long previousByteTime;
			long packetStartTime;
			long byteTimeDelta;
			long packetWindowTime = 35000000;
			TimedByte polledTimedUByte;
			
			//forever loop
			while (!isInterrupted()) {
				try {
					//blocking queue will block until a value is present in the queue
					polledTimedUByte = timedByteQueue.take();
					boolean endByteReceived = false;
					if (polledTimedUByte.getValue() == 83) {//don't start constructing packets until start character 'S' is received
						//record packet start time
						packetStartTime = polledTimedUByte.getTime();
						previousByteTime = polledTimedUByte.getTime();
						//byte consumer loop
						while ((polledTimedUByte.getTime() - packetStartTime) < packetWindowTime && packetBuffer.position() < 25) {
							//while packet window period (35ms) has not closed and packet is not full
							//getUnsignedByte time difference between current and last byte
							byteTimeDelta = polledTimedUByte.getTime() - previousByteTime;
							//put byte in buffer, record byte rx time
							previousByteTime = polledTimedUByte.getTime();
							packetBuffer.put(polledTimedUByte.getValue());
							if (packetBuffer.position() == 25) {
								continue;
							}
							if (endByteReceived && (byteTimeDelta > 10000000L)){
								//if endByte has been received and a pause of more than 10ms has elapsed
								break; //break out of byte consumer loop
							} else if(endByteReceived && packetBuffer.position() <= 24) {
								endByteReceived = false; //if payload byte is coincidentally 'E', prevent packet truncation
							}
							//calculate time remaining in packet window
							long remainingPacketWindowPeriod = packetWindowTime - (polledTimedUByte.getTime() - packetStartTime);
							//getUnsignedByte next timed-byte
							// ...and time out polling operation if window closes
							polledTimedUByte = timedByteQueue.poll(remainingPacketWindowPeriod, TimeUnit.NANOSECONDS);
							if (polledTimedUByte != null) {
								if (polledTimedUByte.getValue() == 69) {
									endByteReceived = true; //test if packet has truly ended on next byte consumer loop
								}
							} else {
								//if packet window closes,break out of inner while loop
								break;
							}
						}
						if (packetBuffer.position() == 25) {//if the packet has reached position 25
							if (endByteReceived) { //and the end byte has been received
								outputCache.put(packetStartTime, new MCCommsPacket(packetBuffer.array())); //output packet
								synchronized (this) {
									this.notifyAll(); //notify waiting threads that packets are available
								}
							}
						}
						//reset buffer
						packetBuffer = IntBuffer.allocate(25);
					}
				} catch (InterruptedException e) {
					this.interrupt();
				} catch (MCCommsException ignored) {} //malformed packets are ignored
			}
		}
	}

	/**
	 * Reads and writes raw bytes from/to the serial bus
	 */
	private class SerialByteHandler extends Thread {

		private final AtomicReference<SerialPort> serialPortAtomicRef;
		private InputStream inputStream;
		private OutputStream outputStream;
		private LinkedBlockingQueue<TimedByte> timedByteQueue;

		SerialByteHandler(AtomicReference<SerialPort> serialPortAtomicRef, LinkedBlockingQueue<TimedByte> timedByteQueue) {
			this.serialPortAtomicRef = serialPortAtomicRef;
			this.timedByteQueue = timedByteQueue;
		}

		@Override
		public void run() {
			while (!isInterrupted()) { //forever
				//if the port is open
				if (this.serialPortAtomicRef.get().openPort()) {
					//populate input/output streams
					if (this.inputStream == null) {
						this.inputStream = this.serialPortAtomicRef.get().getInputStream();
						continue;
					}
					if (this.outputStream == null) {
						this.outputStream = this.serialPortAtomicRef.get().getOutputStream();
						continue;
					}
					//if bytes are available to be read
					try {
						while (this.serialPortAtomicRef.get().isOpen() && inputStream.available() > 0) {
							timedByteQueue.add(new TimedByte(System.nanoTime(), inputStream.read()));
						}
					} catch (IOException ignored) {}
					//if bytes are waiting to be written
					if (!getTXPacketQueue().isEmpty())
						getTXPacketQueue().forEach(packet -> {
							try {
								outputStream.write(packet.getPacketBuffer().array());
								outputStream.flush();
							} catch (IOException e) {
								logger.warn(e.getMessage());
							}
						});
					try {
						sleep(5); //prevent maxing out the CPU on serial IO reads - TODO check system resource usage, may be heavy on small systems
					} catch (InterruptedException e) {
						interrupt();
					}
				}
			}
		}
	}

	/**
	 * Holds serial-read byte as well as the time at which it was received
	 */
	private class TimedByte{

		private final long time;
		private final int value;

		TimedByte(long time, int value) {
			this.time = time;
			this.value = value;
		}

		long getTime() {
			return time;
		}

		int getValue() {
			return value;
		}
	}
}