/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.kippzonenpyranometer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.pyra.PyranometerNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Output debugging information on systemlog")
public class KippZonenPyranometerController extends Controller {

	private final Logger log = LoggerFactory.getLogger(KippZonenPyranometerController.class);

	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public KippZonenPyranometerController() {
		super();
	}

	public KippZonenPyranometerController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "PyranometerNature", description = "Sets the Pyra device.", type = Pyra.class, isOptional = true)
	public final ConfigChannel<Pyra> pyra = new ConfigChannel<Pyra>("pyra", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			PyranometerNature pyra = this.pyra.getValue().pyra;
			long statusFlags = pyra.getStatusFlags().value();
			long devMode = pyra.getDevMode().value();
			if (devMode != 1 || statusFlags != 0) {
				pyra.setClearError().pushWrite(true);
			}
		} catch (InvalidValueException | WriteChannelException e) {
			log.error("Unable to clear error: " + e.getMessage());
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}