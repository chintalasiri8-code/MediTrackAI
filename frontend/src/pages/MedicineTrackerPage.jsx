import React from 'react';
import { MedicineTrackerWidget } from '../components/MedicineTrackerWidget';

export const MedicineTrackerPage = () => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <MedicineTrackerWidget />
    </div>
  );
};
