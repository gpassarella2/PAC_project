import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MyTripsPage from './pages/MyTripsPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/my-trips" element={<MyTripsPage />} />
      </Routes>
    </BrowserRouter>
  );
}