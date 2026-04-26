package com.littletrip.api.model;

/**
 * Enum representing the type of tap event at a transit fare validator.
 *
 * Values:
 * <ul>
 *   <li>ON - Passenger tapped their card when boarding (tap-on)</li>
 *   <li>OFF - Passenger tapped their card when alighting (tap-off)</li>
 * </ul>
 *
 * Tap sequence: ON (start trip) → OFF (end trip and calculate fare)
 */
public enum TapType {
    ON,
    OFF
}