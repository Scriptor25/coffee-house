import { computed } from "@runtime/computed";
import { signal } from "@runtime/signal";
import {
  createSession,
  getSessionToken,
  setSessionToken,
} from "../data/session";
import { setMetadata } from "../meta/meta";
import styles from "./login.module.css";

export function LoginPage() {
  if (getSessionToken()) {
    window.location.hash = "";
    return;
  }

  setMetadata({
    type: "website",
    title: "Login",
    description: "The Login Page",
  });

  const sPending = signal(false);

  const handleSubmit = (event: SubmitEvent) => {
    event.preventDefault();

    const form = event.currentTarget as HTMLFormElement;

    const data = new FormData(form, event.submitter);

    const username = data.get("username") as string;
    const password = data.get("password") as string;

    sPending.set(true);

    createSession(username, password).then((token) => {
      setSessionToken(token);

      if (token) {
        window.location.hash = "";
        return;
      }

      sPending.set(false);
    });
  };

  return computed(() => {
    const pending = sPending.get();

    return (
      <form onsubmit={handleSubmit} className={styles.form}>
        <div className={styles.set}>
          <label>
            <span>Username</span>
            <input
              type="text"
              name="username"
              autocomplete="username"
              required
              disabled={pending}
            />
          </label>
          <label>
            <span>Password</span>
            <input
              type="password"
              name="password"
              autocomplete="current-password"
              required
              disabled={pending}
            />
          </label>
        </div>
        <button type="submit" disabled={pending}>
          {pending ? "pending..." : "submit"}
        </button>
      </form>
    );
  });
}
