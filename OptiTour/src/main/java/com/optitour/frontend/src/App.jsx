import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import MonumentsPage from './pages/MonumentsPage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/monuments" element={<MonumentsPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;