FROM node:24.19.0-bookworm-slim
WORKDIR /app
COPY --chown=node:node package.json ./
COPY --chown=node:node src ./src
COPY --chown=node:node public ./public
RUN mkdir /app/data && chown node:node /app/data
USER node
ENV NODE_ENV=production HOST=0.0.0.0 PORT=3000 DB_PATH=/app/data/linkod.db
EXPOSE 3000
VOLUME ["/app/data"]
CMD ["node", "src/server.js"]
