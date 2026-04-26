import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { StatusBadge } from '../shared/StatusBadge';

describe('StatusBadge', () => {
  it('renders "Settled" label for COMPLETED status', () => {
    render(<StatusBadge status="COMPLETED" />);
    expect(screen.getByText('Settled')).toBeInTheDocument();
  });

  it('renders "In Complete" label for INCOMPLETE status', () => {
    render(<StatusBadge status="INCOMPLETE" />);
    expect(screen.getByText('In Complete')).toBeInTheDocument();
  });

  it('renders "Voided" label for CANCELLED status', () => {
    render(<StatusBadge status="CANCELLED" />);
    expect(screen.getByText('Voided')).toBeInTheDocument();
  });

  it('renders "Active" label for IN_PROGRESS status', () => {
    render(<StatusBadge status="IN_PROGRESS" />);
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('applies teal color classes for COMPLETED status', () => {
    const { container } = render(<StatusBadge status="COMPLETED" />);
    const badge = container.firstChild as HTMLElement;
    expect(badge).toHaveClass('bg-teal-50', 'text-teal-700', 'border-teal-200');
  });

  it('applies red color classes for INCOMPLETE status', () => {
    const { container } = render(<StatusBadge status="INCOMPLETE" />);
    const badge = container.firstChild as HTMLElement;
    expect(badge).toHaveClass('bg-red-50', 'text-red-700', 'border-red-200');
  });
});