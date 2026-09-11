import type { FormEvent } from "react";
import { useCallback, useEffect, useState } from "react";
import { clsx } from "clsx";
import { getApiConfig } from "../config.ts";
import { Button } from "./button";
import { Container } from "./container";

type Estado = "PENDIENTE" | "APROBADA" | "RECHAZADA";

type Categoria = "VIATICOS" | "SOFTWARE" | "HERRAMIENTAS" | "MARKETING" | "OTRO";

const CATEGORIAS: { value: Categoria; label: string }[] = [
  { value: "VIATICOS", label: "Viáticos — gastos de viaje, transporte, alojamiento" },
  { value: "SOFTWARE", label: "Software — licencias, suscripciones, herramientas digitales" },
  { value: "HERRAMIENTAS", label: "Herramientas — equipos, materiales físicos" },
  { value: "MARKETING", label: "Marketing — publicidad, eventos, materiales" },
  { value: "OTRO", label: "Otro — cualquier cosa que no entre en las anteriores" },
];

function etiquetaCategoria(c: Categoria | string | undefined) {
  return CATEGORIAS.find((x) => x.value === c)?.label.split(" — ")[0] ?? c ?? "—";
}

type Solicitud = {
  id: number;
  titulo: string;
  descripcion: string;
  monto: number;
  categoria: Categoria;
  solicitante: string;
  estado: Estado;
  comentarioAdmin: string | null;
  creadoEn: string;
  decididoEn: string | null;
};

type PresupuestosPanelProps = {
  accessToken: string;
  email: string;
};

function scopesFromToken(accessToken: string): string[] {
  try {
    const payload = accessToken.split(".")[1];
    if (!payload) return [];
    const padded = payload.replace(/-/g, "+").replace(/_/g, "/");
    const b64 = padded + "=".repeat((4 - (padded.length % 4)) % 4);
    const json = JSON.parse(atob(b64)) as { scope?: string };
    return (json.scope ?? "").split(/\s+/).filter(Boolean);
  } catch {
    return [];
  }
}

function formatoMonto(n: number) {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0,
  }).format(n);
}

function formatoFecha(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return new Intl.DateTimeFormat("es-CL", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(d);
}

export function PresupuestosPanel({ accessToken, email }: PresupuestosPanelProps) {
  const api = getApiConfig().baseUrl;
  const scopes = scopesFromToken(accessToken);
  const puedeEscribir = scopes.includes("presupuestos/write");
  const puedeDecidir = scopes.includes("presupuestos/decidir");
  const puedeLeer = scopes.includes("presupuestos/read") || puedeEscribir || puedeDecidir;

  const [lista, setLista] = useState<Solicitud[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [cargando, setCargando] = useState(false);
  const [titulo, setTitulo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [monto, setMonto] = useState("");
  const [categoria, setCategoria] = useState<Categoria>("VIATICOS");
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [comentario, setComentario] = useState<Record<number, string>>({});
  const [aEliminar, setAEliminar] = useState<Solicitud | null>(null);
  const [eliminando, setEliminando] = useState(false);

  const headers = useCallback(
    () => ({
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    }),
    [accessToken],
  );

  const cargar = useCallback(async () => {
    if (!puedeLeer) {
      setLista([]);
      return;
    }
    setCargando(true);
    setError(null);
    try {
      const res = await fetch(`${api}/presupuestos`, { headers: headers() });
      const text = await res.text();
      if (!res.ok) {
        setError(`GET /presupuestos → HTTP ${res.status}: ${text}`);
        setLista([]);
        return;
      }
      const data: unknown = JSON.parse(text);
      if (!Array.isArray(data)) {
        setError(`GET /presupuestos no devolvió un array: ${text}`);
        setLista([]);
        return;
      }
      setLista(data as Solicitud[]);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Error al listar");
      setLista([]);
    } finally {
      setCargando(false);
    }
  }, [api, headers, puedeLeer]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  function empezarEdicion(s: Solicitud) {
    setEditandoId(s.id);
    setTitulo(s.titulo);
    setDescripcion(s.descripcion);
    setMonto(String(s.monto));
    setCategoria(s.categoria ?? "OTRO");
  }

  function cancelarForm() {
    setEditandoId(null);
    setTitulo("");
    setDescripcion("");
    setMonto("");
    setCategoria("VIATICOS");
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const bodyEdicion = {
      titulo: titulo.trim(),
      descripcion: descripcion.trim(),
      monto: Number(monto),
      categoria,
    };

    const res =
      editandoId === null
        ? await fetch(`${api}/presupuestos`, {
            method: "POST",
            headers: headers(),
            body: JSON.stringify({ ...bodyEdicion, solicitante: email }),
          })
        : await fetch(`${api}/presupuestos/${editandoId}`, {
            method: "PUT",
            headers: headers(),
            body: JSON.stringify(bodyEdicion),
          });

    const text = await res.text();
    if (!res.ok) {
      if (res.status === 403) {
        setError("403 - Sin permisos para generar solicitudes");
        return;
      }
      setError(
        `${editandoId === null ? "POST" : "PUT"} /presupuestos → HTTP ${res.status}: ${text}`,
      );
      return;
    }
    cancelarForm();
    await cargar();
  }

  async function confirmarEliminar() {
    if (!aEliminar) return;
    const id = aEliminar.id;
    setEliminando(true);
    setError(null);
    try {
      const res = await fetch(`${api}/presupuestos/${id}`, {
        method: "DELETE",
        headers: headers(),
      });
      if (!res.ok) {
        const text = await res.text();
        setError(`DELETE /presupuestos/${id} → HTTP ${res.status}: ${text}`);
        return;
      }
      if (editandoId === id) cancelarForm();
      setAEliminar(null);
      await cargar();
    } finally {
      setEliminando(false);
    }
  }

  async function decidir(id: number, estado: "APROBADA" | "RECHAZADA") {
    setError(null);
    const res = await fetch(`${api}/presupuestos/${id}/decision`, {
      method: "POST",
      headers: headers(),
      body: JSON.stringify({
        estado,
        comentario: comentario[id]?.trim() || null,
      }),
    });
    const text = await res.text();
    if (!res.ok) {
      setError(`POST decision → HTTP ${res.status}: ${text}`);
      return;
    }
    await cargar();
  }

  const listado = (
    <div className={puedeEscribir ? "" : "mt-12"}>
      <div className="flex items-center justify-between gap-4">
        <h3 className="text-base font-medium text-gray-950">Listado de Solicitudes</h3>
        <Button
          variant="outline"
          type="button"
          onClick={() => void cargar()}
          disabled={cargando || !puedeLeer}
        >
          {cargando ? "Cargando…" : "Actualizar"}
        </Button>
      </div>

      {!puedeLeer ? (
        <p className="mt-4 text-sm text-gray-600">
          Sin <code>presupuestos/read</code> no se puede listar.
        </p>
      ) : lista.length === 0 ? (
        <p className="mt-4 text-sm text-gray-500">No hay solicitudes aún.</p>
      ) : (
        <ul className="mt-6 space-y-4">
          {lista.map((s) => (
            <li
              key={s.id}
              className={clsx(
                "relative rounded-r-xl rounded-l-none border-l-4 p-5 ring-1 ring-black/5",
                puedeEscribir ? "bg-gray-50" : "bg-white shadow-sm",
                s.estado === "PENDIENTE" && "border-l-amber-500",
                s.estado === "APROBADA" && "border-l-emerald-500",
                s.estado === "RECHAZADA" && "border-l-red-500",
              )}
            >
              <span
                className={clsx(
                  "absolute top-5 right-5 rounded-full px-3 py-1 text-xs font-semibold",
                  s.estado === "PENDIENTE" && "bg-amber-100 text-amber-900",
                  s.estado === "APROBADA" && "bg-emerald-100 text-emerald-900",
                  s.estado === "RECHAZADA" && "bg-red-100 text-red-900",
                )}
              >
                {s.estado}
              </span>
              <div className="pr-24">
                  <p className="text-xs font-medium text-gray-500">{s.solicitante}</p>
                  <p className="mt-1 font-medium text-gray-950">
                    #{s.id} · {s.titulo}
                  </p>
                  <p className="mt-0.5 text-sm text-gray-500">{formatoFecha(s.creadoEn)}</p>
                  <p className="mt-1 text-xs font-medium text-gray-500">
                    {etiquetaCategoria(s.categoria)}
                  </p>
                  <p className="mt-1 text-sm text-gray-600">{s.descripcion}</p>
                  <p className="mt-2 text-sm text-gray-800">{formatoMonto(s.monto)}</p>
              </div>

              {s.comentarioAdmin ? (
                puedeEscribir ? (
                  <div className="mt-4 flex w-full min-w-0 items-start gap-3 rounded-xl bg-white p-4 ring-1 ring-black/5">
                    <svg
                      viewBox="0 0 20 20"
                      fill="currentColor"
                      aria-hidden="true"
                      className="mt-0.5 size-5 shrink-0 text-gray-400"
                    >
                      <path
                        fillRule="evenodd"
                        d="M10 2c-2.236 0-4.43.18-6.583.514C1.613 2.83.5 4.232.5 5.877v2.246c0 1.645 1.113 3.047 2.917 3.363.54.095 1.09.174 1.649.236.373.041.634.414.52.768a12.5 12.5 0 0 1-.923 2.146c-.234.41.22.87.65.678a17.4 17.4 0 0 0 3.53-2.013c.204-.128.44-.194.68-.194.76 0 1.51-.04 2.25-.118 1.804-.316 2.917-1.718 2.917-3.363V5.877c0-1.645-1.113-3.047-2.917-3.363A49.4 49.4 0 0 0 10 2Z"
                        clipRule="evenodd"
                      />
                    </svg>
                    <p className="min-w-0 flex-1 text-sm text-gray-700">{s.comentarioAdmin}</p>
                  </div>
                ) : (
                  <p className="mt-3 text-sm text-gray-600">Admin: {s.comentarioAdmin}</p>
                )
              ) : null}

              {puedeEscribir && s.estado === "PENDIENTE" ? (
                <div className="mt-4 flex flex-wrap gap-3">
                  <Button type="button" variant="outline" onClick={() => empezarEdicion(s)}>
                    Editar
                  </Button>
                  <Button type="button" variant="danger" onClick={() => setAEliminar(s)}>
                    Eliminar
                  </Button>
                </div>
              ) : null}

              {puedeDecidir && s.estado === "PENDIENTE" ? (
                <div className="mt-4 flex flex-wrap items-end gap-3">
                  <input
                    className="min-w-[12rem] flex-1 rounded-lg border border-black/10 px-3 py-2 text-sm"
                    placeholder="Comentario (opcional)"
                    value={comentario[s.id] ?? ""}
                    onChange={(e) =>
                      setComentario((prev) => ({ ...prev, [s.id]: e.target.value }))
                    }
                  />
                  <Button type="button" onClick={() => void decidir(s.id, "APROBADA")}>
                    Aprobar
                  </Button>
                  <Button
                    type="button"
                    variant="danger"
                    onClick={() => void decidir(s.id, "RECHAZADA")}
                  >
                    Rechazar
                  </Button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </div>
  );

  return (
    <section className="border-t border-black/5 bg-gray-50 py-16 sm:py-24">
      <Container>
        <h2 className="text-sm font-semibold tracking-widest text-gray-500 uppercase">
          Solicitudes de presupuesto
        </h2>

        {error ? (
          <pre className="mt-6 overflow-auto rounded-lg bg-red-50 p-4 text-xs text-red-800 ring-1 ring-red-200">
            {error}
          </pre>
        ) : null}

        {puedeEscribir ? (
          <div className="mt-8 grid gap-12 lg:grid-cols-12 lg:items-start lg:gap-10">
            <form onSubmit={onSubmit} className="space-y-4 lg:col-span-5">
              <h3 className="text-sm font-medium text-gray-950">
                {editandoId === null ? "Nueva solicitud" : `Editando #${editandoId}`}
              </h3>
              <div>
                <label className="block text-sm font-medium text-gray-950">
                  Actividad / proyecto
                </label>
                <input
                  className="mt-1 w-full rounded-lg border border-black/10 px-3 py-2 text-sm"
                  value={titulo}
                  onChange={(e) => setTitulo(e.target.value)}
                  required
                  maxLength={160}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-950">Categoría</label>
                <select
                  className="mt-1 w-full rounded-lg border border-black/10 px-3 py-2 text-sm"
                  value={categoria}
                  onChange={(e) => setCategoria(e.target.value as Categoria)}
                  required
                >
                  {CATEGORIAS.map((c) => (
                    <option key={c.value} value={c.value}>
                      {c.label}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-950">Descripción</label>
                <textarea
                  className="mt-1 w-full rounded-lg border border-black/10 px-3 py-2 text-sm"
                  rows={3}
                  value={descripcion}
                  onChange={(e) => setDescripcion(e.target.value)}
                  required
                  maxLength={1000}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-950">Monto (CLP)</label>
                <input
                  type="number"
                  min={1}
                  className="mt-1 w-full rounded-lg border border-black/10 px-3 py-2 text-sm"
                  value={monto}
                  onChange={(e) => setMonto(e.target.value)}
                  required
                />
              </div>
              {editandoId === null ? (
                <p className="text-xs text-gray-500">Solicitante: {email}</p>
              ) : null}
              <div className="flex flex-wrap gap-3">
                <Button type="submit">
                  {editandoId === null ? "Enviar solicitud" : "Guardar cambios"}
                </Button>
                {editandoId !== null ? (
                  <Button type="button" variant="outline" onClick={cancelarForm}>
                    Cancelar
                  </Button>
                ) : null}
              </div>
            </form>
            <div className="rounded-2xl bg-white p-6 ring-1 ring-black/5 sm:p-8 lg:col-span-7 lg:min-h-full">
              {listado}
            </div>
          </div>
        ) : (
          <>
            {!puedeDecidir ? (
              <pre className="mt-6 overflow-auto rounded-lg bg-red-50 p-4 text-xs text-red-800 ring-1 ring-red-200">
                403 - Sin permisos para generar solicitudes
              </pre>
            ) : null}
            {listado}
          </>
        )}
      </Container>

      {aEliminar ? (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="presentation"
          onClick={() => {
            if (!eliminando) setAEliminar(null);
          }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="eliminar-titulo"
            className="w-full max-w-md rounded-xl bg-white p-6 shadow-lg ring-1 ring-black/5"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 id="eliminar-titulo" className="text-base font-medium text-gray-950">
              ¿Eliminar solicitud?
            </h3>
            <p className="mt-2 text-sm text-gray-600">
              Se eliminará #{aEliminar.id} · {aEliminar.titulo}. Esta acción no se puede
              deshacer.
            </p>
            <div className="mt-6 flex flex-wrap justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                disabled={eliminando}
                onClick={() => setAEliminar(null)}
              >
                Cancelar
              </Button>
              <Button
                type="button"
                variant="danger"
                disabled={eliminando}
                onClick={() => void confirmarEliminar()}
              >
                {eliminando ? "Eliminando…" : "Eliminar"}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}
