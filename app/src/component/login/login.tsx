import { computed, signal, type Signal } from "@runtime/state";
import { createSession } from "../../data/session";
import styles from "./login.module.css";

export function Login(props: { session: Signal<string | null> }) {
  document.title = "Login";

  const state = signal<"none" | "pending" | "success" | "error">("none");

  const handleSubmit = (event: SubmitEvent) => {
    event.preventDefault();

    const form = event.currentTarget as HTMLFormElement;

    const data = new FormData(form, event.submitter);

    const username = data.get("username") as string;
    const password = data.get("password") as string;

    state.set("pending");

    createSession(username, password).then((token) => {
      if (token) {
        state.set("success");
        props.session.set(token);
      } else {
        state.set("error");
      }

      setTimeout(() => {
        state.set("none");
      }, 1000);
    });
  };

  return computed(() => {
    const value = state.get();
    const disabled = value === "pending";

    const label = (() => {
      switch (value) {
        case "none":
          return "submit";
        case "pending":
          return "pending...";
        case "success":
          return "success";
        case "error":
          return "error";
      }
    })();

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
              disabled={disabled}
            />
          </label>
          <label>
            <span>Password</span>
            <input
              type="password"
              name="password"
              autocomplete="current-password"
              required
              disabled={disabled}
            />
          </label>
        </div>
        <button type="submit" disabled={disabled}>
          {label}
        </button>
      </form>
    );
  });
}
