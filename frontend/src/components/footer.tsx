import { Container } from "./container";
import { Gradient } from "./gradient";
import { PlusGrid, PlusGridItem, PlusGridRow } from "./plus-grid";

export function Footer() {
  return (
    <footer>
      <Gradient className="relative">
        <div className="absolute inset-2 rounded-4xl bg-white/80" />
        <Container>
          <PlusGrid className="py-6">
            <PlusGridRow>
              <PlusGridItem className="py-3">
                <div className="text-sm/6 text-gray-950">
                  &copy; {new Date().getFullYear()} DSY1107
                </div>
              </PlusGridItem>
            </PlusGridRow>
          </PlusGrid>
        </Container>
      </Gradient>
    </footer>
  );
}
