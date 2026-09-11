import { clsx } from "clsx";
import type { ButtonHTMLAttributes } from "react";

const variants = {
  primary: clsx(
    "inline-flex items-center justify-center px-4 py-[calc(--spacing(2)-1px)]",
    "rounded-full border border-transparent bg-gray-950 shadow-md",
    "text-base font-medium whitespace-nowrap text-white",
    "disabled:bg-gray-950 disabled:opacity-40 hover:bg-gray-800",
  ),
  secondary: clsx(
    "relative inline-flex items-center justify-center px-4 py-[calc(--spacing(2)-1px)]",
    "rounded-full border border-transparent bg-white/15 shadow-md ring-1 ring-[#D15052]/15",
    "after:absolute after:inset-0 after:rounded-full after:shadow-[inset_0_0_2px_1px_#ffffff4d]",
    "text-base font-medium whitespace-nowrap text-gray-950",
    "disabled:bg-white/15 disabled:opacity-40 hover:bg-white/20",
  ),
  outline: clsx(
    "inline-flex items-center justify-center px-4 py-[calc(--spacing(2)-1px)]",
    "rounded-full border border-transparent shadow-sm ring-1 ring-black/10",
    "text-base font-medium whitespace-nowrap text-gray-950",
    "disabled:bg-transparent disabled:opacity-40 hover:bg-gray-50",
  ),
  danger: clsx(
    "inline-flex items-center justify-center px-4 py-[calc(--spacing(2)-1px)]",
    "rounded-full border border-transparent bg-red-600 shadow-md",
    "text-base font-medium whitespace-nowrap text-white",
    "disabled:bg-red-600 disabled:opacity-40 hover:bg-red-500",
  ),
};

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: keyof typeof variants;
};

export function Button({
  variant = "primary",
  className,
  ...props
}: ButtonProps) {
  return (
    <button {...props} className={clsx(variants[variant], className)} />
  );
}
