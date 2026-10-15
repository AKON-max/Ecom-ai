FROM node:22-bookworm-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_AGENT_BASE_URL=http://localhost:8000
ARG VITE_ECOMMERCE_BASE_URL=http://localhost:8081
ENV VITE_AGENT_BASE_URL=$VITE_AGENT_BASE_URL
ENV VITE_ECOMMERCE_BASE_URL=$VITE_ECOMMERCE_BASE_URL
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80