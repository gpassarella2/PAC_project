import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MyTripsPage from './pages/MyTripsPage';
import ItineraryPage from './pages/ItineraryPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/my-trips" element={<MyTripsPage />} />
        <Route path="/itinerary/:tripId" element={<ItineraryPage />} />
      </Routes>
    </BrowserRouter>
  );
}