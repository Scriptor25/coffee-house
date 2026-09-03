import { computed } from "@runtime/computed";
import { signal } from "@runtime/signal";
import { getSessionToken, setSessionToken } from "../../data/session";
import { Dashboard } from "../dashboard/dashboard";
import { Login } from "../login/login";

export function App() {
  const sToken = signal(getSessionToken());

  sToken.subscribe(() => {
    setSessionToken(sToken.get());
  });

  return computed(() => {
    const token = sToken.get();

    return token ? <Dashboard /> : <Login sToken={sToken} />;
  });
}
