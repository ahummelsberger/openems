package io.openems.edge.battery.microcare.mk1;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.mc_comms.api.BridgeMCComms;
import io.openems.edge.bridge.mc_comms.api.element.*;
import io.openems.edge.bridge.mc_comms.api.task.ReadMCCommsTask;
import io.openems.edge.bridge.mc_comms.util.AbstractMCCommsComponent;
import io.openems.edge.bridge.mc_comms.util.ElementToChannelConverter;
import io.openems.edge.bridge.mc_comms.util.MCCommsProtocol;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.util.concurrent.atomic.AtomicReference;


@Designate( ocd=Config.class, factory=true)
@Component(
		name="io.openems.edge.battery.microcare.mk1",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class BatteryMK1 extends AbstractMCCommsComponent implements Battery, OpenemsComponent {

	private String bridgeID;
	
	public BatteryMK1() {
		super(OpenemsComponent.ChannelId.values(),
				Battery.ChannelId.values(),
				ChannelId.values());
	}
	
	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setMCCommsBridge(BridgeMCComms bridge) {
		super.setMCCommsBridge(bridge);
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		BATTERY_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS));

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	@Override
	protected MCCommsProtocol defineMCCommsProtocol() {
		return new MCCommsProtocol(new AtomicReference<>(this),
				new ReadMCCommsTask(180, 181, Priority.HIGH, 20000,
						m(ChannelId.BATTERY_TEMP, new SignedInt8BitElement(0), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.CAPACITY, new Integer32BitElement(1), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.READY_FOR_WORKING, new BooleanElement(5)),
						m(Battery.ChannelId.SOC, new Integer8BitElement(6), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.SOH, new Integer8BitElement(7), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.CURRENT, new Integer16BitElement(8), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.VOLTAGE, new Integer16BitElement(10), ElementToChannelConverter.SCALE_FACTOR_1)),
				new ReadMCCommsTask(182, 183, Priority.LOW,
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new Integer16BitElement(0), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.CHARGE_MAX_VOLTAGE, new Integer16BitElement(2), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new Integer16BitElement(4), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE, new Integer16BitElement(6), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.MAX_CELL_VOLTAGE, new Integer16BitElement(8), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.MIN_CELL_VOLTAGE, new Integer16BitElement(10), ElementToChannelConverter.SCALE_FACTOR_1),
						m(Battery.ChannelId.MAX_CELL_TEMPERATURE, new SignedInt8BitElement(12), ElementToChannelConverter.SCALE_FACTOR_0),
						m(Battery.ChannelId.MIN_CELL_TEMPERATURE, new SignedInt8BitElement(13), ElementToChannelConverter.SCALE_FACTOR_0))
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), cm, config.slaveAddress(), config.MCCommsBridge_id());
		bridgeID = config.MCCommsBridge_id();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

}
