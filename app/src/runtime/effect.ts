import { ReactiveObserver } from "./internal";

export function effect(fn: () => void): () => void {
  const observer = new ReactiveObserver(() => {
    observer.run(fn);
  });

  observer.run(fn);

  return () => {
    observer.dispose();
  };
}
