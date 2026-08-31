# Pre-2.1.0 (2.0.x) mobile comment compatibility fixtures

These fixtures are frozen **Spring MVC HTTP response bodies**, not direct `ObjectMapper.valueToTree` output. Dates therefore use the ISO-8601 strings emitted by the real controller route.

## Source revisions

The mobile contracts and iOS `MARKETING_VERSION` values were read directly from the SKURI mobile repository. The iOS version is used here because `package.json` remained at `2.0.0` during the `2.0.1` release line.

| Mobile revision | Board comment contract | School notice comment contract | App notice comment contract |
| --- | --- | --- | --- |
| `fa36efb3` (`MARKETING_VERSION=2.0.0`) | `BoardCommentDto`; current HTTP adds only optional `isAuthorAdmin` | `NoticeCommentDto`; current HTTP adds optional `authorProfileImage` and `isAuthorAdmin` | No app-notice comment API or DTO existed, so no compatibility claim is made for this route |
| `c0942681` (`MARKETING_VERSION=2.0.1`) | `BoardCommentDto` already includes optional `isAuthorAdmin`; current field set matches | `NoticeCommentDto` includes optional `authorProfileImage` and `isAuthorAdmin`; current field set matches | App-notice comments reuse this same `NoticeCommentDto`; current field set matches |

Both revisions declare `createdAt` and `updatedAt` as strings. Nullable identity fields remain nullable in the placeholder, while required values such as `content`, `isDeleted`, counters, and timestamps retain their expected JSON types.

TypeScript interfaces do not reject extra properties in a JSON object at runtime, and the inspected mobile mappers read only named properties. The Java test therefore makes the narrower, reproducible claim that:

1. every frozen client-known field exists with a compatible value/type;
2. the only additional HTTP fields are those listed above; and
3. subtracting the frozen client-known fields leaves exactly those documented additions.

It does not claim to execute the historical React Native bundle or a runtime schema validator.
