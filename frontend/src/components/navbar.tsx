import type { ReactNode } from "react";
import { PlusGrid, PlusGridItem, PlusGridRow } from "./plus-grid";

type NavbarProps = {
  banner?: ReactNode;
  actions?: ReactNode;
};

export function Navbar({ banner, actions }: NavbarProps) {
  return (
    <header className="py-6 sm:py-8">
      <PlusGrid>
        <PlusGridRow className="relative flex justify-between">
          <div className="relative flex flex-1 gap-6">
            {banner ? (
              <div className="relative flex items-center py-3">
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
