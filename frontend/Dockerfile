# syntax=docker/dockerfile:1
# ビルドコンテキストは web/（BFF との契約 openapi/bff.yaml から型を生成するため）
FROM node:24-alpine AS deps
WORKDIR /app
COPY openapi /openapi
COPY frontend/package.json frontend/package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY frontend .

# 生成した型が契約と一致していることを確かめる。契約だけ直して生成し忘れた変更や、型を手で直した変更をここで落とす
FROM deps AS check-generated
RUN cp app/types/bff.d.ts /tmp/bff.d.ts \
    && npm run generate:bff \
    && diff /tmp/bff.d.ts app/types/bff.d.ts \
    || { echo '画面の型が契約と一致しません。frontend で npm run generate:bff（集約リポジトリでは ./scripts/generate.sh）を実行し、生成物もコミットしてください' >&2; exit 1; }

# docker build --target typecheck で型チェックを実行する
FROM check-generated AS typecheck
RUN npm run typecheck

FROM deps AS build
RUN npm run build

FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production HOST=0.0.0.0 PORT=3000
# Nuxt のビルド成果物（.output）は依存を同梱しているため node_modules は不要
COPY --from=build --chown=node:node /app/.output ./.output
USER node
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
