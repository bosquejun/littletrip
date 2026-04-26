import { describe, it, expect } from 'vitest';
import { maskPan, formatDuration, formatCurrency, formatDateTime } from '../utils.js';

describe('maskPan', () => {
  it('masks middle digits of a 16-digit PAN', () => {
    expect(maskPan('4532118899224455')).toBe('4532****4455');
  });

  it('returns **** for empty string', () => {
    expect(maskPan('')).toBe('****');
  });

  it('returns original string if less than 8 characters', () => {
    expect(maskPan('1234567')).toBe('1234567');
  });
});

describe('formatDuration', () => {
  it('formats seconds into human readable string', () => {
    expect(formatDuration(3661)).toBe('1h 1m 1s');
  });

  it('returns 0s for zero', () => {
    expect(formatDuration(0)).toBe('0s');
  });

  it('formats minutes only', () => {
    expect(formatDuration(120)).toBe('2m');
  });

  it('formats seconds only', () => {
    expect(formatDuration(45)).toBe('45s');
  });
});

describe('formatCurrency', () => {
  it('formats a number as USD', () => {
    expect(formatCurrency(12.5)).toBe('$12.50');
  });

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });
});

describe('formatDateTime', () => {
  it('formats ISO string to short date time', () => {
    const result = formatDateTime('2026-01-15T14:30:00Z');
    expect(result).toContain('Jan');
    expect(result).toContain('15');
    expect(result).toContain(':');
  });

  it('returns dash for empty string', () => {
    expect(formatDateTime('')).toBe('-');
  });
});