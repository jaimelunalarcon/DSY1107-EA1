import { useAuth } from "react-oidc-context";
import type { ReactNode } from "react";
import { Button } from "./components/button";
import { Container } from "./components/container";
import { Footer } from "./components/footer";
import { Gradient, GradientBackground } from "./components/gradient";
import { LogoCloud } from "./components/logo-cloud";
import { Mark } from "./components/logo";
import { Navbar } from "./components/navbar";
import { Screenshot } from "./components/screenshot";
import { Heading, Subheading } from "./components/text";

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
            <div className="flex items-start">
              <Mark className="h-9 fill-black" />
            </div>
            <h1 className="mt-8 text-base/6 font-medium">{title}</h1>
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
              <Button variant="outline" onClick={() => auth.removeUser()}>
                Cerrar sesión
              </Button>
            }
          />
          <div className="pt-16 pb-24 sm:pt-24 sm:pb-32 md:pt-32 md:pb-48">
            <h1 className="font-display text-6xl/[0.9] font-medium tracking-tight text-balance text-gray-950 sm:text-8xl/[0.8] md:text-9xl/[0.8]">
              Sesión iniciada.
            </h1>
            <p className="mt-8 max-w-lg text-xl/7 font-medium text-gray-950/75 sm:text-2xl/8">
              {email}
            </p>
            <div className="mt-12 flex flex-col gap-x-6 gap-y-4 sm:flex-row">
              <Button onClick={() => auth.removeUser()}>Cerrar sesión</Button>
              <Button variant="secondary" type="button" disabled>
                Authorization Code
              </Button>
            </div>
          </div>
        </Container>
      </div>

      <main>
        <Container className="mt-10">
          <LogoCloud />
        </Container>
        <div className="bg-linear-to-b from-white from-50% to-gray-100 py-32">
          <Container className="pb-24">
            <Heading as="h2" className="max-w-3xl">
              Un vistazo a tu identidad federada.
            </Heading>
            <Screenshot
              width={1216}
              height={768}
              src="/screenshots/app.png"
              className="mt-16 h-144 sm:h-auto sm:w-304"
            />
          </Container>

          <Container>
            <Subheading>Identidad</Subheading>
            <Heading as="h3" className="mt-2 max-w-3xl">
              Know more about your session than they do.
            </Heading>
            <div className="mt-10 grid grid-cols-1 gap-4 sm:mt-16 lg:grid-cols-3">
              <article className="rounded-lg bg-white p-10 shadow-xs ring-1 ring-black/5 max-lg:rounded-t-4xl lg:rounded-l-4xl">
                <Subheading as="h3">Cuenta</Subheading>
                <p className="mt-1 text-2xl/8 font-medium tracking-tight text-gray-950">
                  {email}
                </p>
                <p className="mt-2 text-sm/6 text-gray-600">
                  El perfil llega desde el ID token de Cognito después del
                  callback OIDC.
                </p>
              </article>
              <article className="rounded-lg bg-white p-10 shadow-xs ring-1 ring-black/5">
                <Subheading as="h3">Flujo</Subheading>
                <p className="mt-1 text-2xl/8 font-medium tracking-tight text-gray-950">
                  Authorization Code
                </p>
                <p className="mt-2 text-sm/6 text-gray-600">
                  react-oidc-context redirige a Hosted UI y recupera los tokens
                  en localhost:5173.
                </p>
              </article>
              <article className="rounded-lg bg-white p-10 shadow-xs ring-1 ring-black/5 max-lg:rounded-b-4xl lg:rounded-r-4xl">
                <Subheading as="h3">Scopes</Subheading>
                <p className="mt-1 text-2xl/8 font-medium tracking-tight text-gray-950">
                  openid email profile
                </p>
                <p className="mt-2 text-sm/6 text-gray-600">
                  Los mismos scopes configurados en el cliente SPA de Cognito.
                </p>
              </article>
            </div>
          </Container>
        </div>
        <div className="mx-2 mt-2 rounded-4xl bg-gray-900 bg-[url(/dot-texture.svg)] py-24">
          <Container>
            <Subheading dark>Cognito</Subheading>
            <Heading as="h3" dark className="mt-2 max-w-3xl">
              Tu identidad ya está verificada.
            </Heading>
            <p className="mt-6 max-w-2xl text-sm/6 text-gray-400">
              El Hosted UI emitió los tokens. Esta vista solo consume la sesión
              local de react-oidc-context, sin Next.js ni un backend propio.
            </p>
          </Container>
        </div>
      </main>

      <Footer onAction={() => auth.removeUser()} actionLabel="Cerrar sesión" />
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
