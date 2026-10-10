import React from 'react';
import { SmartRemindersWidget } from '../components/SmartRemindersWidget';

export const SmartRemindersPage = () => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <SmartRemindersWidget />
    </div>
  );
};
