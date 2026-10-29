# fullstack-lab-web

画面（Nuxt）と BFF（Kotlin + Spring Boot）。[fullstack-lab](https://github.com/ht-0328/fullstack-lab) の一部です。画面と BFF は同じチームが同じリポジトリで開発する想定なので、1 つにまとめています。全体の構成と動かし方は fullstack-lab の README を参照してください。

```
web/
├── openapi/bff.yaml   # 画面と BFF の契約（正）
├── frontend/          # Nuxt 4（TypeScript）
├── bff/               # Spring Boot
└── e2e/               # Playwright（実ブラウザの E2E テスト）
```

## 契約が正

[openapi/bff.yaml](openapi/bff.yaml) から、画面と BFF の両方にコードを生成します。生成コードは git で管理します（手で直さず、契約を直して生成し直します）。

| 生成物 | 生成元 | 使う側 |
|---|---|---|
| BFF のコントローラーのインターフェースと DTO（`bff/src/generated`） | `openapi-generator`（`kotlin-spring`） | [BffController](bff/src/main/kotlin/com/example/fullstacklab/bff/task/BffController.kt) が実装する |
| 画面の型 `frontend/app/types/bff.d.ts` | `openapi-typescript` | [useBff.ts](frontend/app/composables/useBff.ts) が `openapi-fetch` の型付きクライアントを作る |

BFF のコントローラーは [BffController](bff/src/main/kotlin/com/example/fullstacklab/bff/task/BffController.kt)（`me` / `tasks`）と [SignupController](bff/src/main/kotlin/com/example/fullstacklab/bff/account/SignupController.kt)（`signup`。唯一、未認証で呼べる）です。

契約を変えたら、BFF は `bff` で `./gradlew generate`、画面は `frontend` で `npm run generate:bff` を実行して生成し直し、生成物も同じコミットに含めます（ホストに JDK / Node が無ければ fullstack-lab の `./scripts/generate.sh`）。下の Docker コマンドはテストの前に生成し直して差分がないことを確かめるので（`check-generated` ステージ）、生成し忘れや手で直した生成コードは CI で落ちます。

画面側は「型だけ生成し、HTTP の層は生成しない」方針です。理由は fullstack-lab の docs/architecture.md にあります。

## 他リポジトリへの依存

BFF は API・認可 API・認証 API を、それぞれのリポジトリが publish したクライアント（Maven 成果物）で呼びます。バージョンは [bff/gradle.properties](bff/gradle.properties) で指定します。API 側の契約が変わったら、ここを上げて追従します。

| 環境変数 | 意味 | 既定 |
|---|---|---|
| `MAVEN_REPO_URL` | クライアントの取得先 | `http://localhost:8081/releases`（ローカルの Reposilite） |
| `MAVEN_REPO_USER` / `MAVEN_REPO_PASSWORD` | 資格情報（GitHub Packages、CodeArtifact で必要） | なし |

## コマンド

```bash
docker build --target typecheck -f frontend/Dockerfile .   # 型が契約と一致することを確かめて型チェック
docker build --target test --network host -f bff/Dockerfile .   # クライアントを取得して単体テスト
```

## E2E テスト（Playwright）

[e2e/](e2e/) に、実ブラウザで画面を操作するテストがあります（ログイン、登録、詳細、認可による表示の違い、ログアウト、会員登録とパスワード変更）。全体構成（Nginx、IdP、BFF、API、DB）が起動している必要があるので、fullstack-lab の `scripts/e2e-test.sh` から実行します。ホストに Node.js やブラウザは不要です（Playwright の公式イメージを使います）。

```bash
# fullstack-lab で
./scripts/e2e-test.sh                     # 全部
./scripts/e2e-test.sh tests/auth.spec.ts  # 一部
```

接続先は環境変数 `E2E_BASE_URL`（既定 `http://localhost:8080`）、`E2E_STAFF_BASE_URL`、`E2E_USER_PASSWORD` で変えられます。

Dockerfile のビルドコンテキストは、契約ファイルを含めるためリポジトリのルート（`web/`）です。
