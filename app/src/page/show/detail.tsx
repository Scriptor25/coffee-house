import { resource } from "@runtime/resource";
import { Image } from "../../component/image/image";
import {
  MediaListItem,
  type MediaListMode,
} from "../../component/media-item/media-item";
import { MediaListContainer } from "../../component/media-list/media-list";
import { Suspense } from "../../component/suspense/suspense";
import { getSeasonById } from "../../data/season";
import { getShowById } from "../../data/show";
import styles from "./detail.module.css";

export function ShowSeason(props: { id: string; mode: MediaListMode }) {
  const $item = resource(() => getSeasonById(props.id));

  return (
    <Suspense
      resource={$item}
      pending={<>Loading season...</>}
      error={<>Failed to load season.</>}
    >
      {(item) => (
        <MediaListItem
          href={`#/season/${item.id}`}
          title={item.title}
          thumbnail={
            item.poster.length
              ? (className, sizes) => (
                  <Image
                    className={className}
                    sizes={sizes}
                    src={item.poster}
                  />
                )
              : undefined
          }
          mode={props.mode}
        />
      )}
    </Suspense>
  );
}

export function ShowDetailPage(props: { id: string }) {
  const $item = resource(() => getShowById(props.id));

  return (
    <Suspense
      resource={$item}
      pending={<p>Loading show...</p>}
      error={<p>Failed to load show.</p>}
    >
      {(item) => (
        <>
          <div className={styles.banner}>
            <Image
              className={styles.backdrop}
              src={item.backdrop}
              sizes="100vw"
            />
            <Image className={styles.poster} src={item.poster} sizes="200px" />
          </div>
          <main className={styles.content}>
            <h1>{item.title}</h1>
            <p>{item.description}</p>
            <h2>Seasons</h2>
            <MediaListContainer
              items={item.seasons.map((id) => (mode) => (
                <ShowSeason id={id} mode={mode} />
              ))}
              mode="grid-poster"
            />
          </main>
        </>
      )}
    </Suspense>
  );
}
