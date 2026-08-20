import { useAuth } from "react-oidc-context";
import type { ReactNode } from "react";
import { Button } from "./components/button";
import { Container } from "./components/container";
import { Footer } from "./components/footer";
import { Gradient, GradientBackground } from "./components/gradient";
import { Navbar } from "./components/navbar";
import { ApiTestPanel } from "./components/api-test-panel";

function AuthScreen({
  title,
  description,
  children,
}: {
  title: string;
  description: string;
  children: ReactNode;
}) {
  return (
    <main className="overflow-hidden bg-gray-50">
      <GradientBackground />
      <div className="isolate flex min-h-dvh items-center justify-center p-6 lg:p-8">
        <div className="w-full max-w-md rounded-xl bg-white shadow-md ring-1 ring-black/5">
          <div className="p-7 sm:p-11">
            <h1 className="text-base/6 font-medium">{title}</h1>
            <p className="mt-1 text-sm/5 text-gray-600">{description}</p>
            <div className="mt-8">{children}</div>
          </div>
          <div className="m-1.5 rounded-lg bg-gray-50 py-4 text-center text-sm/5 ring-1 ring-black/5">
            Acceso con Amazon Cognito · OIDC
          </div>
        </div>
      </div>
    </main>
  );
}

function SignedInApp() {
  const auth = useAuth();
  const email = auth.user?.profile.email ?? "cuenta autenticada";

  return (
    <div className="overflow-hidden">
      <div className="relative">
        <Gradient className="absolute inset-2 bottom-0 rounded-4xl ring-1 ring-black/5 ring-inset" />
        <Container className="relative">
          <Navbar
            banner={
              <span className="flex items-center gap-1 rounded-full bg-fuchsia-950/35 px-3 py-0.5 text-sm/6 font-medium text-white">
                Sesión protegida con Cognito
              </span>
            }
            actions={
              <Button onClick={() => auth.removeUser()}>
                Cerrar sesión
              </Button>
            }
          />
          <div className="pt-8 pb-12 sm:pt-12 sm:pb-16 md:pt-16 md:pb-24">
            <h1 className="text-xl font-medium text-gray-950">
              Sesión iniciada.
            </h1>
            <p className="mt-4 max-w-lg text-base font-medium text-gray-950/75">
              {email}
            </p>
          </div>
        </Container>
      </div>

      <ApiTestPanel user={auth.user} />

      <Footer />
    </div>
  );
}

function App() {
  const auth = useAuth();

  if (auth.isLoading) {
    return (
      <AuthScreen
        title="Cargando..."
        description="Validando la sesión con Amazon Cognito."
      >
        <div className="h-2 overflow-hidden rounded-full bg-gray-100">
          <div className="h-full w-1/2 animate-pulse rounded-full bg-gray-950" />
        </div>
      </AuthScreen>
    );
  }

  if (auth.error) {
    return (
      <AuthScreen
        title="No se pudo iniciar sesión"
        description={auth.error.message}
      >
        <Button className="w-full" onClick={() => auth.signinRedirect()}>
          Reintentar con Cognito
        </Button>
      </AuthScreen>
    );
  }

  if (auth.isAuthenticated) {
    return <SignedInApp />;
  }

  return (
    <AuthScreen
      title="Welcome back!"
      description="Inicia sesión con Amazon Cognito para continuar."
    >
      <Button className="w-full" onClick={() => auth.signinRedirect()}>
        Iniciar sesión con Cognito
      </Button>
    </AuthScreen>
  );
}

export default App;
