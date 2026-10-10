import React, { createContext, useContext, useState } from 'react';

const NotificationContext = createContext();

export const NotificationProvider = ({ children }) => {
  const [activeAlert, setActiveAlert] = useState({
    show: true,
    message: "Time to take Medicine A (Doxycycline 100 mg) after breakfast.",
    medicineName: "Doxycycline Hyclate",
    reminderTime: "08:00 AM",
    foodRelation: "After breakfast"
  });

  const [notifications, setNotifications] = useState([
    { id: 1, title: "Medication Alert", message: "Time to take Medicine A after breakfast.", time: "8:00 AM", unread: true },
    { id: 2, title: "Doctor Note Added", message: "Dr. Anitha added a clinical guidance note to CASE-7F92K.", time: "Yesterday", unread: false },
    { id: 3, title: "Weekly Check-in Due", message: "Week 4 Recovery Check-in is now open.", time: "2 days ago", unread: false }
  ]);

  const triggerReminderAlert = (medicineName, foodRelation) => {
    setActiveAlert({
      show: true,
      message: `Time to take ${medicineName} ${foodRelation}.`,
      medicineName,
      foodRelation
    });
  };

  const dismissAlert = () => {
    setActiveAlert(prev => ({ ...prev, show: false }));
  };

  return (
    <NotificationContext.Provider value={{
      activeAlert,
      notifications,
      triggerReminderAlert,
      dismissAlert
    }}>
      {children}
    </NotificationContext.Provider>
  );
};

export const useNotification = () => useContext(NotificationContext);
