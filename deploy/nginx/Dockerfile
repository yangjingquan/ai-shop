# Build the admin SPA in the same image that serves it.  This keeps the
# deployed static files and the Nginx configuration in one immutable artifact.
FROM node:22-alpine AS admin-builder

WORKDIR /workspace/admin

RUN corepack enable && corepack prepare pnpm@9.15.5 --activate

COPY admin/package.json admin/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY admin/ ./
RUN pnpm build

FROM nginx:1.27-alpine

COPY deploy/nginx/conf.d/ /etc/nginx/conf.d/
COPY --from=admin-builder /workspace/admin/dist/ /usr/share/nginx/html/console/

# Fail the image build if the SPA build did not produce an entry page.
RUN test -s /usr/share/nginx/html/console/index.html
