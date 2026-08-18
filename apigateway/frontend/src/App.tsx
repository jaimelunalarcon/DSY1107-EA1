import { useAuth } from "react-oidc-context";

function App() {
  const auth = useAuth();

  if (auth.isLoading) {
    return <div>Cargando...</div>;
  }

  if (auth.error) {
    return <div>Error: {auth.error.message}</div>;
  }

  if (auth.isAuthenticated) {
    return (
      <div>
        <h1>Sesión iniciada</h1>
        <p>{auth.user?.profile.email}</p>

        <button onClick={() => auth.removeUser()}>
          Cerrar sesión
        </button>
      </div>
    );
  }

  return (
    <button onClick={() => auth.signinRedirect()}>
      Iniciar sesión con Cognito
    </button>
  );
}

export default App;