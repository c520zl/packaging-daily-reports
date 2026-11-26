FROM node:20-alpine AS build
WORKDIR /app

# pnpm 用 corepack 管理（node:20 自带 corepack，无需 npm i -g）
RUN corepack enable

COPY package.json ./
RUN pnpm install --no-frozen-lockfile

COPY . .
RUN pnpm build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
