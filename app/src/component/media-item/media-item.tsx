import type { VNode } from "@runtime/jsx-runtime";
import styles from "./media-item.module.css";

export type MediaListMode = "grid" | "grid-poster" | "list";

export interface MediaListItemProps {
  mode: MediaListMode;
  href: string;
  title: VNode;
  thumbnail?: (className?: string, sizes?: string) => VNode;
}

export function MediaListItem(props: MediaListItemProps) {
  return (
    <li className={styles.item} data-mode={props.mode}>
      {props.thumbnail ? (
        props.thumbnail(
          styles.thumbnail,
          props.mode === "list" ? "30vw" : "(max-width: 600px) 50vw, 300px",
        )
      ) : (
        <div className={styles.thumbnail} />
      )}
      <a className={styles.title} href={props.href}>
        {props.title}
      </a>
    </li>
  );
}
