import { clsx } from "clsx";
import type { HTMLAttributes } from "react";

type HeadingProps = HTMLAttributes<HTMLHeadingElement> & {
  as?: "h1" | "h2" | "h3" | "h4";
  dark?: boolean;
};

export function Heading({
  className,
  as: Element = "h2",
  dark = false,
  ...props
}: HeadingProps) {
  return (
    <Element
      {...props}
      data-dark={dark ? "true" : undefined}
      className={clsx(
        className,
        "text-4xl font-medium tracking-tighter text-pretty text-gray-950 data-dark:text-white sm:text-6xl",
      )}
    />
  );
}

export function Subheading({
  className,
  as: Element = "h2",
  dark = false,
  ...props
}: HeadingProps) {
  return (
    <Element
      {...props}
      data-dark={dark ? "true" : undefined}
      className={clsx(
        className,
        "font-mono text-xs/5 font-semibold tracking-widest text-gray-500 uppercase data-dark:text-gray-400",
      )}
    />
  );
}

export function Lead({
  className,
  ...props
}: HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p
      className={clsx(className, "text-2xl font-medium text-gray-500")}
      {...props}
    />
  );
}
