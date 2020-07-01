package io.openems.edge.ess.generic.symmetric;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.startstop.StartStopConfig;

@ObjectClassDefinition(//
		name = "ESS Generic Managed Symmetric", //
		description = "")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Start/stop behaviour?", description = "Should this Component be forced to start or stop?")
	StartStopConfig startStop() default StartStopConfig.START;

	@AttributeDefinition(name = "Battery-Inverter-ID", description = "ID of Battery-Inverter.")
	String batteryInverter_id() default "batteryInverter0";

	@AttributeDefinition(name = "Battery-ID", description = "ID of Battery.")
	String battery_id() default "battery0";

	@AttributeDefinition(name = "Battery-Inverter target filter", description = "This is auto-generated by 'Battery-Inverter-ID'.")
	String batteryInverter_target() default "";

	@AttributeDefinition(name = "Inverter target filter", description = "This is auto-generated by 'Battery-ID'.")
	String battery_target() default "";

	String webconsole_configurationFactory_nameHint() default "ESS Generic Managed Symmetric [{id}]";

}