package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

public class CalculateConsumption {

	private final Logger log = LoggerFactory.getLogger(DynamicCharge.class);

	@SuppressWarnings("unused")
	private DynamicCharge dynamicCharge;
	private long totalDemand = 0;
	private static float minPrice;
	private long totalConsumption;
	private long currentProduction;
	private long currentConsumption;
	private static long nettCapacity;
	private LocalDate dateOfT0 = null;
	private static long maxApparentPower;
	private static LocalDateTime t0 = null;
	private static LocalDateTime t1 = null;
	private LocalDate dateOfLastRun = null;
	private static long chargebleConsumption;
	private LocalDateTime currentHour = null;
	private static long remainingConsumption;
	private static long demand_Till_Cheapest_Hour;
	private static LocalDateTime cheapTimeStamp = null;

	private static TreeMap<LocalDateTime, Long> chargeSchedule = new TreeMap<LocalDateTime, Long>();
	private static TreeMap<LocalDateTime, Float> hourlyPrices = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<LocalDateTime, Long> hourlyConsumption = new TreeMap<LocalDateTime, Long>();

	public CalculateConsumption(DynamicCharge dynamicCharge) {
		this.dynamicCharge = dynamicCharge;
	}

	private enum State {

		PRODUCTION_LOWER_THAN_CONSUMPTION, PRODUCTION_DROPPED_BELOW_CONSUMPTION, PRODUCTION_HIGHER_THAN_CONSUMPTION,
		PRODUCTION_EXCEEDED_CONSUMPTION
	}

	private State currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;

	protected void run(ManagedSymmetricEss ess, SymmetricMeter meter, Config config, Sum sum) {

		int production = sum.getProductionActivePower().value().orElse(0);
		int consumption = sum.getConsumptionActivePower().value().orElse(0);
		long productionEnergy = sum.getProductionActiveEnergy().value().orElse(0L);
		long consumptionEnergy = sum.getConsumptionActiveEnergy().value().orElse(0L);

		LocalDate nowDate = LocalDate.now();
		LocalDateTime now = LocalDateTime.now();

		log.info("totalDemand: " + totalDemand + " t0: " + t0 + " t1: " + t1 + " current Hour: " + currentHour);

		switch (currentState) {
		case PRODUCTION_LOWER_THAN_CONSUMPTION:
			log.info(" State: " + currentState);

			if (t0 != null) {

				// First time of the day when production > consumption.
				// Avoids the fluctuations and shifts to next state only the next day.
				// to avoid exceptional cases (production value might be minus during night)
				if ((now.getHour() >= config.Max_Morning_hour()) && dateOfT0.isBefore(nowDate)) {
					if (production > consumption || now.getHour() > config.Max_Morning_hour()) {
						log.info(production + " is greater than " + consumption
								+ " so switching the state from PRODUCTION LOWER THAN CONSUMPTION to PRODUCTION EXCEEDING CONSUMPTION");
						this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
					}
				}

				// Detects the switching of hour
				if (now.getHour() == currentHour.plusHours(1).getHour()) {
					log.info(" Switching of the hour detected and updating " + " [ " + currentHour + " ] ");
					this.totalConsumption = (consumptionEnergy - currentConsumption)
							- (productionEnergy - currentProduction);
					hourlyConsumption.put(currentHour.withNano(0).withMinute(0).withSecond(0), totalConsumption);
					this.currentConsumption = consumptionEnergy;
					this.currentProduction = productionEnergy;
					this.currentHour = now;
				}

				// condition for initial run.
			} else if (production > consumption || now.getHour() >= config.Max_Morning_hour()) {
				this.currentState = State.PRODUCTION_EXCEEDED_CONSUMPTION;
			}
			break;

		case PRODUCTION_EXCEEDED_CONSUMPTION:
			log.info(" State: " + currentState);
			if (t1 == null && t0 != null) {

				// This is the first time of the day that "production > consumption".
				this.totalConsumption = (consumptionEnergy - currentConsumption)
						- (productionEnergy - currentProduction);
				hourlyConsumption.put(now.withNano(0).withMinute(0).withSecond(0), totalConsumption);
				this.totalDemand = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
						hourlyConsumption.lastKey().plusDays(1)) + hourlyConsumption.lastEntry().getValue();
				t1 = now;
				log.info(" t1 is set: " + t1);

				// reset values
				log.info("Resetting Values during " + now);
				t0 = null;
				chargeSchedule.clear();

				this.dateOfLastRun = nowDate;
				log.info("dateOfLastRun " + dateOfLastRun);
			}

			log.info(production + " is greater than " + consumption
					+ " so switching the state from PRODUCTION EXCEEDING CONSUMPTION to PRODUCTION HIGHER THAN CONSUMPTION ");
			this.currentState = State.PRODUCTION_HIGHER_THAN_CONSUMPTION;
			break;

		case PRODUCTION_HIGHER_THAN_CONSUMPTION:
			log.info(" State: " + currentState);

			// avoid switching to next state during the day.
			if (production < consumption && now.getHour() >= config.Max_Evening_hour()) {
				log.info(production + " is lesser than " + consumption
						+ " so switching the state from PRODUCTION HIGHER THAN CONSUMPTION to PRODUCTION DROPPED BELOW CONSUMPTION ");
				this.currentState = State.PRODUCTION_DROPPED_BELOW_CONSUMPTION;
			}
			break;

		case PRODUCTION_DROPPED_BELOW_CONSUMPTION:
			log.info(" State: " + currentState);

			t0 = now;
			this.dateOfT0 = nowDate;
			log.info("t0 is set at: " + dateOfT0);

			this.currentHour = now;
			this.currentConsumption = consumptionEnergy;
			this.currentProduction = productionEnergy;

			// avoids the initial run
			if (dateOfLastRun != null) {
				log.info("Entering Calculations: ");
				t1 = null;
			} else {
				// for the initial run, adding some average hourly consumption values to
				// schedule.
				for (int i = 0; i < 17; i++) {
					hourlyConsumption.put(now.plusHours(i).minusDays(1).withMinute(0).withSecond(0).withNano(0),
							(long) (4000));
				}
			}

			nettCapacity = ess.getNetCapacity().value().orElse(0);
			maxApparentPower = ess.getMaxApparentPower().value().orElse(0);
			int soc = ess.getSoc().value().orElse(0);
			long availableEnergy = (soc * nettCapacity) / 100;

			log.info("Collecting hourly Prices: ");
			Prices.houlryPrices();
			hourlyPrices = Prices.getHourlyPrices();

			this.totalDemand = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
					hourlyConsumption.lastKey().plusDays(1)) + hourlyConsumption.lastEntry().getValue();

			log.info(" Getting schedule: ");
			getChargeSchedule(hourlyConsumption.firstKey().plusDays(1), hourlyConsumption.lastKey().plusDays(1),
					totalDemand, availableEnergy);

			// Resetting Values
			log.info(production + "is lesser than" + consumption
					+ "so switching the state from PRODUCTION DROPPED BELOW CONSUMPTION to PRODUCTION LOWER THAN CONSUMPTION");
			hourlyConsumption.clear();
			this.currentState = State.PRODUCTION_LOWER_THAN_CONSUMPTION;
			break;
		}
	}

	private static void getChargeSchedule(LocalDateTime start, LocalDateTime end, long totalDemand,
			long availableEnergy) {

		// function to find the minimum priceHour
		cheapHour(start, end);
		demand_Till_Cheapest_Hour = calculateDemandTillThishour(start, cheapTimeStamp);
		long currentHourConsumption = hourlyConsumption.ceilingEntry(cheapTimeStamp.minusDays(1)).getValue();

//		  Calculates the amount of energy that needs to be charged during the cheapest
//		  price hours.

		if (totalDemand > 0) {

			// if the battery doesn't has sufficient energy!
			if (availableEnergy >= demand_Till_Cheapest_Hour) {

				totalDemand -= availableEnergy;
				adjustRemainigConsumption(cheapTimeStamp, end, totalDemand, availableEnergy);
			} else {

				chargebleConsumption = totalDemand - demand_Till_Cheapest_Hour - currentHourConsumption;

				if (chargebleConsumption > 0) {

					if (chargebleConsumption > maxApparentPower) {
						LocalDateTime lastCheapTimeStamp = cheapTimeStamp;

						// checking for next cheap hour if it is before or after the first cheapest
						// hour.
						cheapHour(start, cheapTimeStamp);
						float firstMinPrice = minPrice;

						cheapHour(lastCheapTimeStamp.plusHours(1), end);

						if (minPrice < firstMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
									hourlyConsumption.lastKey().plusDays(1), remainingConsumption, maxApparentPower);
						} else {
							if (chargebleConsumption > nettCapacity) {
								remainingConsumption = chargebleConsumption - nettCapacity;
								adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
										hourlyConsumption.lastKey().plusDays(1), remainingConsumption, nettCapacity);
							}
						}

						cheapTimeStamp = lastCheapTimeStamp;
						chargebleConsumption = maxApparentPower;
					}
					totalDemand = totalDemand - chargebleConsumption - currentHourConsumption - remainingConsumption;
					remainingConsumption = 0;

					// adding into charge Schedule
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					getChargeSchedule(start, cheapTimeStamp, totalDemand, availableEnergy);
				} else {
					totalDemand -= currentHourConsumption;
					getChargeSchedule(start, cheapTimeStamp, totalDemand, availableEnergy);
				}
			}
		}
	}

	private static void adjustRemainigConsumption(LocalDateTime start, LocalDateTime end, long remainingConsumption,
			long availableCapacity) {

		if (!start.isEqual(end)) {

			if (remainingConsumption > 0) {
				cheapHour(start, end);

				demand_Till_Cheapest_Hour = calculateDemandTillThishour(start, cheapTimeStamp);
				long currentHourConsumption = hourlyConsumption.ceilingEntry(cheapTimeStamp.minusDays(1)).getValue();

				if (demand_Till_Cheapest_Hour > availableCapacity) {
					demand_Till_Cheapest_Hour -= availableCapacity;
					availableCapacity = 0;
				} else {
					availableCapacity -= demand_Till_Cheapest_Hour;
					demand_Till_Cheapest_Hour = 0;
				}

				long allowedConsumption = nettCapacity - availableCapacity;

				if (allowedConsumption > 0) {

					if (allowedConsumption > maxApparentPower) {
						allowedConsumption = maxApparentPower;
					}
					remainingConsumption = remainingConsumption - currentHourConsumption - demand_Till_Cheapest_Hour;
					if (remainingConsumption > 0) {
						if (remainingConsumption > allowedConsumption) {
							remainingConsumption -= allowedConsumption;
							availableCapacity += allowedConsumption;

							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, allowedConsumption);
							adjustRemainigConsumption(cheapTimeStamp.plusHours(1), end, remainingConsumption,
									availableCapacity);
						} else {
							// adding into charge Schedule
							chargeSchedule.put(cheapTimeStamp, remainingConsumption);
						}
					}

				} else {
					availableCapacity -= currentHourConsumption;
					adjustRemainigConsumption(cheapTimeStamp.plusHours(1), end, remainingConsumption,
							availableCapacity);
				}
			}
		}
	}

	private static void cheapHour(LocalDateTime start, LocalDateTime end) {
		minPrice = Float.MAX_VALUE;

		// Calculates the cheapest price hour within certain Hours.
		for (Map.Entry<LocalDateTime, Float> entry : hourlyPrices.subMap(start, end).entrySet()) {
			if (entry.getValue() < minPrice) {
				cheapTimeStamp = entry.getKey();
				minPrice = entry.getValue();
			}
		}
	}

	private static long calculateDemandTillThishour(LocalDateTime start, LocalDateTime end) {
		long demand = 0;

		// calculates the total demand till a certain hour.
		for (Entry<LocalDateTime, Long> entry : hourlyConsumption.entrySet()) {
			if ((entry.getKey().plusDays(1).isEqual(start) || entry.getKey().plusDays(1).isAfter(start))
					&& entry.getKey().plusDays(1).isBefore(end)) {
				demand += entry.getValue();
			}
		}
		return demand;
	}

	protected TreeMap<LocalDateTime, Long> getChargeSchedule() {
		return chargeSchedule;
	}

}
