import React, { useEffect, useState } from 'react';
import { useGamePolling } from './hooks/useGamePolling';
import { LobbyPage } from './pages/LobbyPage';
import { GamePage } from './pages/GamePage';
import { SurveyPage } from './pages/SurveyPage';
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

  if (gameState && gameState.status === 'RUNNING') {
    return (
      <GamePage
        gameState={gameState}
        onStateUpdated={setGameStateDirectly}
      />
    );
  }

  if (gameState && gameState.status === 'FINISHED') {
    if (!gameState.player.hasSubmittedSurvey) {
      return (
        <SurveyPage
          gameState={gameState}
          onStateUpdated={setGameStateDirectly}
        />
      );
    }
    return (
      <EndgamePage
        gameState={gameState}
        onResetSession={async () => {
          setGameStateDirectly(null);
        }}
      />
    );
  }

  return (
    <LobbyPage
      gameState={gameState}
      onJoined={refreshNow}
      onLeft={() => setGameStateDirectly(null)}
      onNavigateAdmin={navigateToAdmin}
    />
  );
};

export default App;
