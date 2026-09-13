import { computed } from "@runtime/computed";
import type { VNode } from "@runtime/jsx-runtime";
import type { Resource } from "@runtime/resource";

export function Suspense<T>(props: {
  resource: Resource<T>;
  pending?: VNode;
  error?: VNode;
  children?: (data: T) => VNode;
}) {
  const $res = props.resource;

  return computed(() => {
    const res = $res.get();

    switch (res.status) {
      case "none":
        $res.load();
      case "pending":
        return props.pending;
      case "error":
        return props.error;
      case "success":
        return props.children?.(res.data);
    }
  });
}
