import type { ReactNode } from "react";
import { Logo } from "./logo";
import { PlusGrid, PlusGridItem, PlusGridRow } from "./plus-grid";

type NavbarProps = {
  banner?: ReactNode;
  actions?: ReactNode;
};

export function Navbar({ banner, actions }: NavbarProps) {
  return (
    <header className="pt-12 sm:pt-16">
      <PlusGrid>
        <PlusGridRow className="relative flex justify-between">
          <div className="relative flex gap-6">
            <PlusGridItem className="py-3">
              <Logo className="h-9" />
            </PlusGridItem>
            {banner ? (
              <div className="relative hidden items-center py-3 lg:flex">
                {banner}
              </div>
            ) : null}
          </div>
          {actions ? (
            <PlusGridItem className="relative flex items-center px-4">
              {actions}
            </PlusGridItem>
          ) : null}
        </PlusGridRow>
      </PlusGrid>
    </header>
  );
}
