# Gradle-based build. Build stage uses Gradle to run Node/npm build.
FROM gradle:8.5-jdk21-alpine AS builder

WORKDIR /app

# Copy Gradle config and source (exclude node_modules, .gradle, dist via .dockerignore)
COPY . .

# Build frontend via Gradle (Node plugin runs npm install + npm run build)
RUN gradle build --no-daemon

# Serve stage
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
