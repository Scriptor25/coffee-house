import type { VNode } from "@runtime/jsx-runtime";
import {
  MediaListItem,
  type MediaListItemProps,
  type MediaListMode,
} from "../media-item/media-item";
import styles from "./media-list.module.css";

export function MediaList(props: {
  items: Omit<MediaListItemProps, "mode">[];
  end?: Omit<MediaListItemProps, "mode">;
  mode?: MediaListMode;
}) {
  const mode = props.mode ?? "grid";

  return (
    <ul className={styles.list} data-mode={mode}>
      {props.items.map((item) => (
        <MediaListItem mode={mode} {...item} />
      ))}
      {props.end && <MediaListItem mode={mode} {...props.end} />}
    </ul>
  );
}

export function MediaListContainer(props: {
  items: ((mode: MediaListMode) => VNode)[];
  end?: Omit<MediaListItemProps, "mode">;
  mode?: MediaListMode;
}) {
  const mode = props.mode ?? "grid";

  return (
    <ul className={styles.list} data-mode={mode}>
      {props.items.map((item) => item(mode))}
      {props.end && <MediaListItem mode={mode} {...props.end} />}
    </ul>
  );
}
