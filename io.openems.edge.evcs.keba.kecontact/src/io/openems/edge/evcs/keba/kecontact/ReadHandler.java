package io.openems.edge.evcs.keba.kecontact;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.evcs.api.Evcs;

/**
 * Handles replys to Report Querys sent by {@link ReadWorker}
 *
 */
public class ReadHandler implements Consumer<String> {

	private final Logger log = LoggerFactory.getLogger(ReadHandler.class);
	private final KebaKeContact parent;
	private boolean receiveReport1 = false;
	private boolean receiveReport2 = false;
	private boolean receiveReport3 = false;

	public ReadHandler(KebaKeContact parent) {
		this.parent = parent;
	}

	@Override
	public void accept(String message) {
		
		if (message.startsWith("TCH-OK")) {
			log.debug("KEBA confirmed reception of command: TCH-OK");
			this.parent.triggerQuery();

		} else if (message.startsWith("TCH-ERR")) {
			log.warn("KEBA reported command error: TCH-ERR");
			this.parent.triggerQuery();

		} else {
			JsonElement jMessageElement;
			try {
				jMessageElement = JsonUtils.parse(message);
			} catch (OpenemsNamedException e) {
				log.error("Error while parsing KEBA message: " + e.getMessage());
				return;
			}
			JsonObject jMessage = jMessageElement.getAsJsonObject();
			// JsonUtils.prettyPrint(jMessage);
			Optional<String> idOpt = JsonUtils.getAsOptionalString(jMessage, "ID");
			if (idOpt.isPresent()) {
				// message with ID
				String id = idOpt.get();
				if (id.equals("1")) {
					/*
					 * Reply to report 1
					 */
					receiveReport1 = true;
					setString(KebaChannelId.PRODUCT, jMessage, "Product");
					setString(KebaChannelId.SERIAL, jMessage, "Serial");
					setString(KebaChannelId.FIRMWARE, jMessage, "Firmware");
					setInt(KebaChannelId.COM_MODULE, jMessage, "COM-module");

				} else if (id.equals("2")) {
					/*
					 * Reply to report 2
					 */
					receiveReport2 = true;
					setInt(KebaChannelId.STATUS, jMessage, "State");
					setInt(KebaChannelId.ERROR_1, jMessage, "Error1");
					setInt(KebaChannelId.ERROR_2, jMessage, "Error2");
					setInt(KebaChannelId.PLUG, jMessage, "Plug");
					setBoolean(KebaChannelId.ENABLE_SYS, jMessage, "Enable sys");
					setBoolean(KebaChannelId.ENABLE_USER, jMessage, "Enable user");
					setInt(KebaChannelId.MAX_CURR_PERCENT, jMessage, "Max curr %");
					setInt(KebaChannelId.CURR_USER, jMessage, "Curr user");
					setInt(KebaChannelId.CURR_FAILSAFE, jMessage, "Curr FS");
					setInt(KebaChannelId.TIMEOUT_FAILSAFE, jMessage, "Tmo FS");
					setInt(KebaChannelId.CURR_TIMER, jMessage, "Curr timer");
					setInt(KebaChannelId.TIMEOUT_CT, jMessage, "Tmo CT");
					setInt(KebaChannelId.ENERGY_LIMIT, jMessage, "Setenergy");
					setBoolean(KebaChannelId.OUTPUT, jMessage, "Output");
					setBoolean(KebaChannelId.INPUT, jMessage, "Input");
					
					
					// Set the maximum Power valid by the Hardware
					// The default value will be 32 A, because an older Keba charging station sets the value to 0 if the car is unplugged
					Optional<Integer> hwPower_ma = JsonUtils.getAsOptionalInt(jMessage, "Curr HW"); // in [mA]
					Integer hwPower = null;
					if (hwPower_ma.isPresent()) {
						if(hwPower_ma.get() == 0) {
							hwPower = 32000 * 230 / 1000; // [W]
						}else {
							hwPower = hwPower_ma.get() * 230 / 1000; // [W]
						}
					}
					
					this.parent.channel(Evcs.ChannelId.HARDWARE_POWER_LIMIT).setNextValue(hwPower);
					
				} else if (id.equals("3")) {
					/*
					 * Reply to report 3
					 */
					receiveReport3 = true;
					setInt(KebaChannelId.VOLTAGE_L1, jMessage, "U1");
					setInt(KebaChannelId.VOLTAGE_L2, jMessage, "U2");
					setInt(KebaChannelId.VOLTAGE_L3, jMessage, "U3");
					setInt(KebaChannelId.CURRENT_L1, jMessage, "I1");
					setInt(KebaChannelId.CURRENT_L2, jMessage, "I2");
					setInt(KebaChannelId.CURRENT_L3, jMessage, "I3");
					setInt(KebaChannelId.ACTUAL_POWER, jMessage, "P");
					setInt(KebaChannelId.COS_PHI, jMessage, "PF");
					setInt(KebaChannelId.ENERGY_SESSION, jMessage, "E pres");
					setInt(KebaChannelId.ENERGY_TOTAL, jMessage, "E total");

					// Set the count of the Phases that are currently used
					Channel<Integer> currentL1 = parent.channel(KebaChannelId.CURRENT_L1);
					Channel<Integer> currentL2 = parent.channel(KebaChannelId.CURRENT_L2);
					Channel<Integer> currentL3 = parent.channel(KebaChannelId.CURRENT_L3);

					if (currentL1.value().orElse(0) > 10){

						if (currentL3.value().orElse(0) > 100) {
							this.parent.logInfo(this.log, "KEBA is loading on three ladder"); 
							set(KebaChannelId.PHASES, 3);
							
							
						} else if (currentL2.value().orElse(0) > 100) {
							this.parent.logInfo(this.log, "KEBA is loading on two ladder"); 
							set(KebaChannelId.PHASES, 2);
							
						} else{
							this.parent.logInfo(this.log, "KEBA is loading on one ladder"); 
							set(KebaChannelId.PHASES, 1);
						}
						Channel<Integer> phases = this.parent.channel(KebaChannelId.PHASES);
						this.parent.channel(Evcs.ChannelId.MINIMUM_POWER).setNextValue(230 /*Spannung*/ * 6 /*min Strom*/ * phases.value().orElse(3));
						this.parent.channel(Evcs.ChannelId.MAXIMUM_POWER).setNextValue(230 /*Spannung*/ * 32 /*max Strom*/ * phases.value().orElse(3));
					}else {

						// set Min & Max Power to Default values that allows the User a power setting between those values
						Channel<Integer> min = this.parent.channel(Evcs.ChannelId.MINIMUM_POWER);
						Channel<Integer> max = this.parent.channel(Evcs.ChannelId.MAXIMUM_POWER);
						if(min.value().orElse(null)==null || max.value().orElse(null) == null) {
							this.parent.channel(Evcs.ChannelId.MINIMUM_POWER).setNextValue(230 /*Spannung*/ * 6 /*min Strom*/ * 3);
							this.parent.channel(Evcs.ChannelId.MAXIMUM_POWER).setNextValue(230 /*Spannung*/ * 32 /*max Strom*/ * 3);
						}
					}
					

					// Set CHARGE_POWER
					Optional<Integer> power_mw = JsonUtils.getAsOptionalInt(jMessage, "P"); // in [mW]
					Integer power = null;
					if (power_mw.isPresent()) {
						power = power_mw.get() / 1000; // convert to [W]
					}
					this.parent.channel(Evcs.ChannelId.CHARGE_POWER).setNextValue(power);
				}

			} else {
				/*
				 * message without ID -> UDP broadcast
				 */
				if (jMessage.has("State")) {
					setInt(KebaChannelId.STATUS, jMessage, "State");
				}
				if (jMessage.has("Plug")) {
					setInt(KebaChannelId.PLUG, jMessage, "Plug");
				}
				if (jMessage.has("Input")) {
					setBoolean(KebaChannelId.INPUT, jMessage, "Input");
				}
				if (jMessage.has("Enable sys")) {
					setBoolean(KebaChannelId.ENABLE_SYS, jMessage, "Enable sys");
				}
				if (jMessage.has("E pres")) {
					setInt(KebaChannelId.ENERGY_SESSION, jMessage, "E pres");
				}
			}
		}
	}

	private void set(KebaChannelId channelId, Object value) {
		this.parent.channel(channelId).setNextValue(value);
	}

	private void setString(KebaChannelId channelId, JsonObject jMessage, String name) {
		set(channelId, JsonUtils.getAsOptionalString(jMessage, name).orElse(null));
	}

	private void setInt(KebaChannelId channelId, JsonObject jMessage, String name) {
		set(channelId, JsonUtils.getAsOptionalInt(jMessage, name).orElse(null));
	}

	private void setBoolean(KebaChannelId channelId, JsonObject jMessage, String name) {
		Optional<Integer> enableSysOpt = JsonUtils.getAsOptionalInt(jMessage, name);
		if (enableSysOpt.isPresent()) {
			set(channelId, enableSysOpt.get() == 1);
		} else {
			set(channelId, null);
		}
	}

	/**
	 * returns true or false, if the requested report answered or not 
	 * and set that value to false
	 * 
	 * @param report
	 * @return
	 */
	public boolean hasResultandReset(Report report) {

		boolean result = false;
		switch (report) {
		case REPORT1:
			result = receiveReport1;
			receiveReport1 = false;
			break;
		case REPORT2:
			result = receiveReport2;
			receiveReport2 = false;
			break;
		case REPORT3:
			result = receiveReport3;
			receiveReport3 = false;
			break;
		}
		return result;
	}
}
