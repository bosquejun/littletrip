import { describe, it, expect } from 'vitest';
import { mapTripDto, getTripsUrl } from '../api.js';

const sampleDto = {
  id: '550e8400-e29b-41d4-a716-446655440000',
  cardToken: '4532118899224455',
  fromStopId: 'Airport Hub',
  toStopId: 'North Station',
  started: '2026-01-01T10:00:00Z',
  finished: '2026-01-01T10:45:00Z',
  durationSecs: 2700,
  chargeAmount: 1200,
  operatorId: 'Westside Transit Co.',
  status: 'COMPLETED',
};

describe('mapTripDto', () => {
  it('maps chargeAmount from cents to dollars', () => {
    const trip = mapTripDto(sampleDto);
    expect(trip.chargeAmount).toBe(12);
  });

  it('maps cardToken to pan', () => {
    const trip = mapTripDto(sampleDto);
    expect(trip.pan).toBe('4532118899224455');
  });

  it('maps id as string', () => {
    const trip = mapTripDto(sampleDto);
    expect(trip.id).toBe('550e8400-e29b-41d4-a716-446655440000');
  });

  it('maps started to startTime', () => {
    const trip = mapTripDto(sampleDto);
    expect(trip.startTime).toBe('2026-01-01T10:00:00Z');
  });

  it('maps null finished to empty string', () => {
    const trip = mapTripDto({ ...sampleDto, finished: null });
    expect(trip.endTime).toBe('');
  });

  it('maps null fromStopId to empty string', () => {
    const trip = mapTripDto({ ...sampleDto, fromStopId: null });
    expect(trip.fromStop).toBe('');
  });

  it('maps null toStopId to empty string', () => {
    const trip = mapTripDto({ ...sampleDto, toStopId: null });
    expect(trip.toStop).toBe('');
  });
});

describe('getTripsUrl', () => {
  it('always includes page=1 and size=100', () => {
    const url = getTripsUrl('http://api', {});
    expect(url).toContain('page=1');
    expect(url).toContain('size=100');
  });

  it('includes page=1 and size=100 regardless of filters', () => {
    const url = getTripsUrl('http://api', { deviceId: 'device-42', datePreset: 'today' });
    expect(url).toContain('page=1');
    expect(url).toContain('size=100');
    expect(url).not.toContain('deviceId');
    expect(url).not.toContain('datePreset');
  });
});