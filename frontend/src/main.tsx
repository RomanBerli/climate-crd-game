import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import { setStoredPlayerToken } from './api/client';
import './styles/tokens.css';
import './styles/main.css';

// Notausgang für Geräte, die in einer alten Sitzung hängen: Die Adresse «/?reset» löscht die
// gespeicherte Spieler-Sitzung dieses Browsers und zeigt wieder die Code-Eingabe.
if (new URLSearchParams(window.location.search).has('reset')) {
  setStoredPlayerToken(null);
  window.history.replaceState({}, '', '/');
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
