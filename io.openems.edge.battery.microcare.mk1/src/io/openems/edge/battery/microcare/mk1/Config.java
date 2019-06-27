package io.openems.edge.battery.microcare.mk1;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "BMS Microcare MK1", //
        description = "Provides a service for connecting to, querying and writing to a MC-Comms enabled BMS device.")

@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "bms0";

	@AttributeDefinition(name = "Enabled?", description = "Sets the Enabled state (true/false) of this component")
    boolean enabled() default true;

    @AttributeDefinition(name = "Slave-Address", description = "Desired address of this MC-Comms slave device")
    int slaveAddress() default 2;

    @AttributeDefinition(name = "Bridge ID", description = "MC-Comms master controlling this slave device")
    String MCCommsBridge_id() default "mccomms0";

    @AttributeDefinition(name = "Alias", description = "Human-readable component alias")
	String alias() default "";

}
