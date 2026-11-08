# PERWORK Builder

Cloud build backend for Java Studio's **Создание** section. User projects are stored temporarily in a private Cloudflare Workers KV namespace and are never committed to this public repository.

## Flow
1. Java Studio packages the selected `main/` folder into a ZIP.
2. The PERWORK Worker verifies Turnstile, validates the selected profile and stores the ZIP in private, temporary Workers KV storage.
3. The Worker dispatches `build-generated-project.yml` with a random job ID.
4. A fresh GitHub Actions runner authenticates using GitHub Actions OIDC, retrieves only that job's source/configuration, validates the archive, and generates a Gradle project.
5. Gradle runs inside a pinned Java 17 container with only the generated project and an empty Gradle cache mounted; no OIDC or GitHub credentials enter the container.
6. The runner uploads only the compiled JAR(s) and `build.log` as a short-lived Actions artifact.
7. The runner reports its result through an OIDC-authenticated Worker callback. Java Studio polls and downloads the artifact through the Worker.

## Current verified profile
Only profiles marked `supported` in `builder/profiles.json` are enabled for compilation.

| Minecraft | Loader | Type | Java | Status |
|---|---|---|---:|---|
| 1.20.1 | Forge | Mod | 17 | Verified — isolated Java 17 container build and JAR upload passed in PR workflow run #18 on 2026-11-08 |

Other combinations in the Java Studio selectors are not buildable until their template passes CI.

## Input contract and limits
The ZIP must contain one `main/` folder. Java files may be in `main/java/**` or directly under `main/**`; optional resources in `main/resources/**` are copied into the Gradle project.
- ZIP upload: 20 MiB
- Entries: maximum 1,000
- Total uncompressed size: maximum 100 MiB
- Individual file: maximum 20 MiB
- Path traversal, symlinks and encrypted ZIP entries are rejected.

## Cloudflare configuration
The Workers KV namespace has already been created: `PERWORK_BUILDER` (ID `3d0d1f2d8b1d400aba7857e25afa1334`). The Worker binds it as `BUILDER_KV` in `worker/wrangler.toml`; do not create another namespace or replace this ID. Workers KV is available on Cloudflare's Free plan, and the current 20 MiB upload limit is below its 25 MiB per-value limit. Source ZIPs expire after two hours; job metadata expires after 24 hours.

Set Worker secret `GITHUB_TOKEN` to a fine-grained token with access to both `Ketsuneban/perwork` (existing Worker features) and `Ketsuneban/perwork-builder`, with:
- `Actions: read and write` on `Ketsuneban/perwork-builder` (workflow dispatch and artifact download);
- `Metadata: read` on `Ketsuneban/perwork-builder`;
- existing permissions for the `Ketsuneban/perwork` commit monitor and revert feature.

Create a Cloudflare Turnstile widget for the site hostnames. Put its public site key in the empty `BUILDER_TURNSTILE_SITE_KEY` constant in the Java Studio cloud-build section of `Ketsuneban/perwork/src/app.js`. Keep the corresponding secret only in Cloudflare Worker Secret `TURNSTILE_SECRET_KEY`. The Worker rejects new builds until Turnstile is verified.

## OIDC trust
Internal Worker endpoints accept only a valid GitHub Actions OIDC token with issuer `https://token.actions.githubusercontent.com`, audience `perwork-builder`, repository `Ketsuneban/perwork-builder`, workflow `.github/workflows/build-generated-project.yml`, branch `refs/heads/main`, and event `workflow_dispatch`.

The Gradle container receives no GitHub token, Cloudflare secret, OIDC token or Actions runtime credentials; it mounts only the generated project and an empty Gradle cache. The workflow does not create an intermediate artifact containing user source code. The source ZIP is deleted after the result callback and expires automatically from Workers KV after two hours if a runner never completes the callback. Job metadata expires after 24 hours, and the final JAR/log artifact expires after one day. The workflow retries temporary KV reads to accommodate cross-region propagation.

**Public artifact warning:** this Builder repository is public. GitHub documents that users with read access to a repository can download its workflow artifacts; therefore the final JAR and build log are publicly downloadable while retained. Do not use the current artifact path for proprietary outputs unless the repository is made private or the result transport is moved to private Workers KV storage.

## Local test
```bash
python -m py_compile builder/generate.py builder/safe_extract.py
python builder/generate.py --source examples/main --output generated-project --minecraft 1.20.1 --loader forge --kind mod --mod-id examplemod --mod-name "Example Mod"
cd generated-project
gradle --no-daemon build
```
