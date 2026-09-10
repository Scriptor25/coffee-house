import type { Media } from "../../data/media";
import { MediaItem, type MediaItemMode } from "../media-item/media-item";
import styles from "./media-list.module.css";

export function MediaList(props: { data: Media[]; mode?: MediaItemMode }) {
  return (
    <ul className={styles.list} data-mode={props.mode ?? "grid"}>
      {props.data.map((data) => (
        <MediaItem data={data} mode={props.mode} />
      ))}
    </ul>
  );
}
