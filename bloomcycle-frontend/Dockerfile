FROM node:20-alpine AS build
WORKDIR /angular-app
COPY package*.json ./
COPY tailwind.config.js ./
COPY postcss.config.js ./
RUN npm install --legacy-peer-deps && npm cache clean --force
COPY . .
EXPOSE 4200
CMD ["npm", "run", "start"]