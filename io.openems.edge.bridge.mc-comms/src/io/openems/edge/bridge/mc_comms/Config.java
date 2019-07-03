package io.openems.edge.bridge.mc_comms;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Bridge MC-Comms", //
        description = "Provides a service for connecting to, querying and writing to a MC-Comms device.")

@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "mccomms0";

	@AttributeDefinition(name = "Enabled?", description = "Sets the Enabled state (true/false) of this component")
    boolean enabled() default true;

    @AttributeDefinition(name = "Port-Name", description = "The name of the serial port - e.g. '/dev/ttyUSB0' or 'COM3'")
    String portName() default "/dev/pts/2";

    @AttributeDefinition(name = "Master-Address", description = "Desired address of the MC-Comms master")
    int masterAddress() default 1;
    
    @AttributeDefinition(name = "Alias", description = "Human-readable component alias")
	String alias() default "";
}
