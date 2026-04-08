import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MyTripsPage from './pages/MyTripsPage';
import ItineraryPage from './pages/ItineraryPage';
import HomePage from './pages/HomePage';
import MonumentsPage from './pages/MonumentsPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/my-trips" element={<MyTripsPage />} />
        <Route path="/itinerary/:tripId" element={<ItineraryPage />} />
        <Route path="/" element={<HomePage />} />
        <Route path="/monuments" element={<MonumentsPage />} />
      </Routes>
    </BrowserRouter>
  );
}


