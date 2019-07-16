package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class App {
	private static TreeMap<LocalDateTime, Float> hourlyPrices = new TreeMap<LocalDateTime, Float>();
	private static TreeMap<LocalDateTime, Long> hourlyConsumption = new TreeMap<LocalDateTime, Long>();
	public static TreeMap<LocalDateTime, Long> chargeSchedule = new TreeMap<LocalDateTime, Long>();
	private static float minPrice;
	private static LocalDateTime cheapTimeStamp = null;
	public static LocalDateTime t0 = null;
	public static LocalDateTime t1 = null;
	private static LocalDateTime startTime;
	private static LocalDateTime endTime;
	private static long chargebleConsumption;
	private static long demand_Till_Cheapest_Hour;
	private static long availableCapacity = 1350;
	private static long nettCapacity = 12000;
	private static long maxApparentPower = 9000;
	private static long totalDemand;
	private static long remainingConsumption;
	private static long currentHourConsumption;

	public static void main(String[] args) {

		LocalDateTime now = LocalDateTime.of(2019, 6, 17, 16, 0);

		for (int i = 0; i < 16; i++) {

			hourlyConsumption.put(now.plusHours(i), (long) (2000 + (150 * i)));
		}

		if (!hourlyConsumption.isEmpty()) {
			System.out.println(
					"first Key: " + hourlyConsumption.firstKey() + " last Key: " + hourlyConsumption.lastKey());
		}

		for (Entry<LocalDateTime, Long> entry : hourlyConsumption.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Consumption: " + entry.getValue());
		}

		totalDemand = (calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
				hourlyConsumption.lastKey().plusDays(1))) + hourlyConsumption.lastEntry().getValue();

		System.out.println(" [ " + nettCapacity + " ] " + " [ " + maxApparentPower + " ] " + " [ " + availableCapacity
				+ " ] " + " [ " + totalDemand + " ] ");

		PricesTest.houlryPricesTest();
		hourlyPrices = PricesTest.getHourlyPricesTest();
		for (Entry<LocalDateTime, Float> entry : hourlyPrices.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Price: " + entry.getValue());
		}
		totalDemand = calculateDemandTillThishour(hourlyConsumption.firstKey().plusDays(1),
				hourlyConsumption.lastKey().plusDays(1)) + hourlyConsumption.lastEntry().getValue();
		System.out.println(" [ " + hourlyConsumption.firstKey() + " ] " + " [ " + hourlyConsumption.lastKey() + " ] ");
		System.out.println(" Getting schedule: ");
		chargeSchedule.clear();

		ChargeSchedule(hourlyPrices.firstKey(), hourlyConsumption.lastKey().plusDays(1), totalDemand,
				availableCapacity);

		for (Entry<LocalDateTime, Long> entry : chargeSchedule.entrySet()) {
			System.out.println("Time: " + entry.getKey() + " Consumption: " + entry.getValue());
		}

	}

	@SuppressWarnings("unused")
	private static void getChargeSchedule(LocalDateTime start, LocalDateTime end, long totalDemand,
			long availableEnergy) {

		System.out.println("Enetered Charge Schedule: ");
		System.out.println("totalDemand: " + totalDemand);
		// function to find the minimum priceHour
		cheapHour(start, end);
		System.out.println("availableCapacity: " + availableEnergy);
		demand_Till_Cheapest_Hour = calculateDemandTillThishour(start, cheapTimeStamp);
		System.out.println("demand_Till_Cheapest_Hour" + demand_Till_Cheapest_Hour);
		currentHourConsumption = hourlyConsumption.ceilingEntry(cheapTimeStamp.minusDays(1)).getValue();
		System.out.println("currentHourConsumption" + currentHourConsumption);

		/*
		 * Calculates the amount of energy that needs to be charged during the cheapest
		 * price hours.
		 */

		if (totalDemand > 0) {

			// if the battery doesn't has sufficient energy!
			if (availableEnergy >= demand_Till_Cheapest_Hour) {
				System.out.println("availableCapacity " + availableEnergy + "is greater than "
						+ "demand_Till_Cheapest_Hour" + demand_Till_Cheapest_Hour);
				totalDemand -= availableEnergy;
//				getCheapestHoursIfBatterySufficient(cheapTimeStamp.plusHours(1), end, availableEnergy, totalDemand);
				adjustRemainigConsumption(cheapTimeStamp, end, totalDemand, availableEnergy);
			} else {
				System.out.println("availableCapacity " + availableEnergy + "is less than "
						+ "demand_Till_Cheapest_Hour" + demand_Till_Cheapest_Hour);
				chargebleConsumption = totalDemand - demand_Till_Cheapest_Hour - currentHourConsumption;
				System.out.println("chargebleConsumption " + chargebleConsumption);
				if (chargebleConsumption > 0) {

					if (chargebleConsumption > maxApparentPower) {
						System.out.println("chargebleConsumption " + chargebleConsumption + "is greater than "
								+ "maxApparentPower" + maxApparentPower);
						LocalDateTime lastCheapTimeStamp = cheapTimeStamp;
						System.out.println("lastCheapTimeStamp " + lastCheapTimeStamp);

						cheapHour(start, cheapTimeStamp);
						float firstMinPrice = minPrice;
						System.out.println("firstMinPrice " + firstMinPrice);

						cheapHour(lastCheapTimeStamp.plusHours(1), end);
						System.out.println("minPrice " + minPrice);

						if (minPrice < firstMinPrice) {
							remainingConsumption = chargebleConsumption - maxApparentPower;
							System.out.println("getting into adjusting remaining charge1: ");
							adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
									hourlyConsumption.lastKey().plusDays(1), remainingConsumption, maxApparentPower);
						} else {
							if (chargebleConsumption > nettCapacity) {
								remainingConsumption = chargebleConsumption - nettCapacity;
								System.out.println("getting into adjusting remaining charge2: ");
								adjustRemainigConsumption(lastCheapTimeStamp.plusHours(1),
										hourlyConsumption.lastKey().plusDays(1), remainingConsumption, nettCapacity);
							}
						}
//						cheapHour(lastCheapTimeStamp.plusHours(1), end);
//						demand_Till_Cheapest_Hour = calculateDemandTillThishour(lastCheapTimeStamp.plusHours(1),
//								cheapTimeStamp);
//						getCheapestHoursIfBatterySufficient(cheapTimeStamp.plusHours(1), end, availableCapacity,
//								remainingConsumption);
						cheapTimeStamp = lastCheapTimeStamp;
						chargebleConsumption = maxApparentPower;
					}
					System.out.println("chargebleConsumption " + chargebleConsumption + "is less than "
							+ "maxApparentPower" + maxApparentPower);
					totalDemand = totalDemand - chargebleConsumption - currentHourConsumption - remainingConsumption;
					System.out.println("totalDemand " + totalDemand);
					remainingConsumption = 0;
					// totalDemand = totalDemand - chargebleConsumption - currentHourConsumption;
					System.out.println("Putting into schedule " + cheapTimeStamp + chargebleConsumption);
					chargeSchedule.put(cheapTimeStamp, chargebleConsumption);
					getChargeSchedule(start, cheapTimeStamp, totalDemand, availableEnergy);
				} else {
					System.out.println("Not greater than 0 ");
					totalDemand -= currentHourConsumption;
					getChargeSchedule(start, cheapTimeStamp, totalDemand, availableEnergy);
				}
			}
		}
	}

	private static void ChargeSchedule(LocalDateTime start, LocalDateTime end, long totalDemand, long availableEnergy) {
		
		System.out.println("[ " + start + " ] " + " [ "+ end + " ] ");

		if (!start.isEqual(end)) {
			cheapestHour(start, end);
			demand_Till_Cheapest_Hour = calculateDemandTillThishour(startTime, endTime);
			System.out.println("demand_Till_Cheapest_Hour: " + demand_Till_Cheapest_Hour);
			long currentConsumption = hourlyConsumption.ceilingEntry(startTime.minusDays(1)).getValue();
			
			if(availableEnergy < 0) {
				availableEnergy = 0;
			}
			if (availableEnergy < demand_Till_Cheapest_Hour) {
				
				System.out.println("availableEnergy: " + availableEnergy);
				System.out.println("currentConsumption: " + currentConsumption);

				long allowedConsumption = nettCapacity - availableEnergy;
				System.out.println("allowedConsumption: " + allowedConsumption);

				if (allowedConsumption > 0) {

					if (allowedConsumption > maxApparentPower) {
						allowedConsumption = maxApparentPower;
					}
					chargebleConsumption = demand_Till_Cheapest_Hour - currentConsumption;
					System.out.println("chargebleConsumption: " + chargebleConsumption);

					if (chargebleConsumption >= availableEnergy) {
						chargebleConsumption -= availableEnergy;
						System.out.println("chargebleConsumption: " + chargebleConsumption);
					}

					if (chargebleConsumption > 0) {
						if (chargebleConsumption > allowedConsumption) {
							System.out.println("chargebleConsumption > allowedConsumption: ");
							chargebleConsumption = allowedConsumption;
							System.out.println("allowedConsumption: " + allowedConsumption);
							System.out.println("chargebleConsumption: " + chargebleConsumption);
							availableEnergy += allowedConsumption;
							System.out.println("availableCapacity: " + availableEnergy);
							System.out.println("Putting into Schedule: " + allowedConsumption);
							chargeSchedule.put(startTime, allowedConsumption);
							totalDemand = totalDemand - chargebleConsumption - currentConsumption;
							System.out.println("totalDemand: " + totalDemand);
							ChargeSchedule(startTime.plusHours(1), end, chargebleConsumption, availableEnergy);
						} else {
							System.out.println("Putting into Schedule-------------: " + chargebleConsumption);
							totalDemand = totalDemand - chargebleConsumption - currentConsumption;
							chargeSchedule.put(startTime, chargebleConsumption);
							ChargeSchedule(endTime, end, chargebleConsumption, availableEnergy);
						}
					} else {
						availableEnergy -= currentConsumption;
						totalDemand -= currentConsumption;
						System.out.println("Avoiding Schedule: ");
						ChargeSchedule(startTime.plusHours(1), end, totalDemand, availableEnergy);
					}
				} else {
					availableEnergy -= currentConsumption;
					totalDemand -= currentConsumption;
					System.out.println("Avoiding Schedule: ");
					ChargeSchedule(startTime.plusHours(1), end, totalDemand, availableEnergy);
				}
			}else {
				System.out.println("available Energy is greater than Demand");
				availableEnergy -= currentConsumption;
				totalDemand -= currentConsumption;
				System.out.println("Avoiding Schedule: ");
				ChargeSchedule(startTime.plusHours(1), end, totalDemand, availableEnergy);
			}
		}
	}

	private static void cheapestHour(LocalDateTime start, LocalDateTime end) {
		float minCost = 0;
		LocalDateTime startCheapTime = null;
		LocalDateTime endCheapTime = null;


		// Calculates the cheapest price hour within certain Hours.
		for (Map.Entry<LocalDateTime, Float> entry : hourlyPrices.subMap(start, end).entrySet()) {

			if (startCheapTime == null) {
				startCheapTime = entry.getKey();
				minCost = entry.getValue();
			} else if (minCost > entry.getValue()) {
				endCheapTime = entry.getKey();
				break;
			}
		}

		if(endCheapTime == null) {
			endCheapTime = end;
		}
		startTime = startCheapTime;
		endTime = endCheapTime;
		startCheapTime = null;
		endCheapTime = null;
		System.out.println("endTime: " + endTime);
	}

	private static void adjustRemainigConsumption(LocalDateTime start, LocalDateTime end, long remainingConsumption,
			long availableCapacity) {

		if (!start.isEqual(end)) {
			System.out.println(start + "------- " + end);

			if (remainingConsumption > 0) {
				cheapHour(start, end);

				demand_Till_Cheapest_Hour = calculateDemandTillThishour(start, cheapTimeStamp);
				System.out.println("demand_Till_Cheapest_Hour: " + demand_Till_Cheapest_Hour);
				long currentConsumption = hourlyConsumption.ceilingEntry(cheapTimeStamp.minusDays(1)).getValue();
				System.out.println("currentConsumption: " + currentConsumption);
				if (demand_Till_Cheapest_Hour > availableCapacity) {
					demand_Till_Cheapest_Hour -= availableCapacity;
					availableCapacity = 0;
				} else {
					availableCapacity -= demand_Till_Cheapest_Hour;
					demand_Till_Cheapest_Hour = 0;
				}
				System.out.println("availableCapacity: " + availableCapacity);
				long allowedConsumption = nettCapacity - availableCapacity;
				System.out.println("allowedConsumption: " + allowedConsumption);
				System.out.println("remainingConsumption: " + remainingConsumption);

				if (allowedConsumption > 0) {

					if (allowedConsumption > maxApparentPower) {
						allowedConsumption = maxApparentPower;
					}
					remainingConsumption = remainingConsumption - currentConsumption - demand_Till_Cheapest_Hour;
					if (remainingConsumption > 0) {
						if (remainingConsumption > allowedConsumption) {
							System.out.println("remainingConsumption > allowedConsumption: ");
							remainingConsumption -= allowedConsumption;
							System.out.println("remainingConsumption: " + remainingConsumption);
							availableCapacity += allowedConsumption;
							System.out.println("availableCapacity: " + availableCapacity);
							System.out.println("Putting into Schedule: " + allowedConsumption);
							chargeSchedule.put(cheapTimeStamp, allowedConsumption);
							adjustRemainigConsumption(cheapTimeStamp.plusHours(1), end, remainingConsumption,
									availableCapacity);
						} else {
							System.out.println("Putting into Schedule: " + remainingConsumption);
							chargeSchedule.put(cheapTimeStamp, remainingConsumption);
						}
					}

				} else {
					availableCapacity -= currentConsumption;
					System.out.println("Avoiding Schedule: ");
					System.out.println(cheapTimeStamp.plusHours(1) + "------- " + end);
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
		System.out.println("cheapTimeStamp: " + cheapTimeStamp);
	}

	private static long calculateDemandTillThishour(LocalDateTime start, LocalDateTime end) {
		long demand = 0;
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
