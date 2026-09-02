import { computed, signal } from "@runtime/state";
import { Dashboard } from "../dashboard/dashboard";
import { Login } from "../login/login";

export function App() {
  const session = signal(window.localStorage.getItem("session"));

  session.subscribe(() => {
    const token = session.get();

    if (token) {
      window.localStorage.setItem("session", token);
    } else {
      window.localStorage.removeItem("session");
    }
  });

  return computed(() => {
    const token = session.get();

    return token ? (
      <Dashboard session={session} />
    ) : (
      <Login session={session} />
    );
  });
}
