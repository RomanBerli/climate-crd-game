import React, { useEffect, useState } from 'react';
import { useGamePolling } from './hooks/useGamePolling';
import { LobbyPage } from './pages/LobbyPage';
import { GamePage } from './pages/GamePage';
import { EndgamePage } from './pages/EndgamePage';
import { AdminPage } from './pages/AdminPage';

export const App: React.FC = () => {
  const [isAdminRoute, setIsAdminRoute] = useState<boolean>(() => {
    return window.location.pathname.startsWith('/admin') || window.location.hash === '#admin';
  });

  useEffect(() => {
    const handlePopState = () => {
      setIsAdminRoute(
        window.location.pathname.startsWith('/admin') || window.location.hash === '#admin'
      );
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigateToAdmin = () => {
    window.history.pushState({}, '', '/admin');
    setIsAdminRoute(true);
  };

  const navigateToPlayer = () => {
    window.history.pushState({}, '', '/');
    setIsAdminRoute(false);
  };

  const {
    gameState,
    displaySeconds,
    loading,
    refreshNow,
    setGameStateDirectly,
  } = useGamePolling(!isAdminRoute);

  if (isAdminRoute) {
    return <AdminPage onBackToPlayerView={navigateToPlayer} />;
  }

  if (loading && !gameState) {
    return (
      <main className="page-container page-container--centered">
        <div className="loading-card">
          <span className="pulse-indicator">Verbinde mit Klima-Labor Server...</span>
        </div>
      </main>
    );
  }

  // Reconnect & State Routing (Spec 4.2 & Plan 3.5)
  if (gameState && (gameState.status === 'RUNNING' || gameState.status === 'PAUSED')) {
    return (
      <GamePage
        gameState={gameState}
        displaySeconds={displaySeconds}
        onStateUpdated={setGameStateDirectly}
      />
    );
  }

  if (gameState && gameState.status === 'FINISHED') {
    return <EndgamePage gameState={gameState} onResetSession={refreshNow} />;
  }

  return (
    <LobbyPage
      gameState={gameState}
      onJoined={refreshNow}
      onNavigateAdmin={navigateToAdmin}
    />
  );
};

export default App;
