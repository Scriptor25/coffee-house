import {
  MediaItem,
  type MediaItemMode,
  type MediaItemProps,
} from "../media-item/media-item";
import styles from "./media-list.module.css";

export function MediaList(props: {
  data: Omit<MediaItemProps, "mode">[];
  mode?: MediaItemMode;
}) {
  return (
    <ul className={styles.list} data-mode={props.mode ?? "grid"}>
      {props.data.map((item) => (
        <MediaItem mode={props.mode} {...item} />
      ))}
    </ul>
  );
}
