import type { Media } from "../../data/media";
import styles from "./media-item.module.css";

export type MediaItemMode = "grid" | "grid-poster" | "list";

export function MediaItem(props: { data: Media; mode?: MediaItemMode }) {
  return (
    <li className={styles.item} data-mode={props.mode ?? "grid"}>
      {props.data.thumbnail ? (
        <img className={styles.thumbnail} src={props.data.thumbnail} />
      ) : (
        <div className={styles.thumbnail} />
      )}
      <a className={styles.title} href={`#/media/${props.data.id}`}>
        {props.data.title}
      </a>
    </li>
  );
}
