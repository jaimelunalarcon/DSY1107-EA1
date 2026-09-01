import { clsx } from "clsx";
import type { ReactNode } from "react";

type PlusGridProps = {
  className?: string;
  children: ReactNode;
};

export function PlusGrid({ className = "", children }: PlusGridProps) {
  return <div className={className}>{children}</div>;
}

export function PlusGridRow({ className = "", children }: PlusGridProps) {
  return (
    <div
      className={clsx(
        className,
        "group/row relative isolate pt-[calc(--spacing(2)+1px)] last:pb-[calc(--spacing(2)+1px)]",
      )}
    >
      <div
        aria-hidden="true"
        className="absolute inset-y-0 left-1/2 -z-10 w-screen -translate-x-1/2"
      >
        <div className="absolute inset-x-0 top-0 border-t border-black/5" />
        <div className="absolute inset-x-0 top-2 border-t border-black/5" />
        <div className="absolute inset-x-0 bottom-0 hidden border-b border-black/5 group-last/row:block" />
        <div className="absolute inset-x-0 bottom-2 hidden border-b border-black/5 group-last/row:block" />
      </div>
      {children}
    </div>
  );
}

export function PlusGridItem({ className = "", children }: PlusGridProps) {
  return (
    <div className={clsx(className, "group/item relative")}>
      <PlusGridIcon
        placement="top left"
        className="hidden group-first/item:block"
      />
      <PlusGridIcon placement="top right" />
      <PlusGridIcon
        placement="bottom left"
        className="hidden group-first/item:group-last/row:block"
      />
      <PlusGridIcon
        placement="bottom right"
        className="hidden group-last/row:block"
      />
      {children}
    </div>
  );
}

type PlusGridIconProps = {
  className?: string;
  placement: "top left" | "top right" | "bottom left" | "bottom right";
};

export function PlusGridIcon({
  className = "",
  placement,
}: PlusGridIconProps) {
  const [yAxis, xAxis] = placement.split(" ");
  const yClass = yAxis === "top" ? "-top-2" : "-bottom-2";
  const xClass = xAxis === "left" ? "-left-2" : "-right-2";

  return (
    <svg
      viewBox="0 0 15 15"
      aria-hidden="true"
      className={clsx(
        className,
        "absolute size-[15px] fill-black/10",
        yClass,
        xClass,
      )}
    >
      <path d="M8 0H7V7H0V8H7V15H8V8H15V7H8V0Z" />
    </svg>
  );
}
