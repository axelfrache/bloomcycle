FROM node:20-alpine AS build
WORKDIR /angular-app
COPY package.json package-lock.json ./
RUN npm install && npm cache clean --force
COPY . .
EXPOSE 4200
CMD ["npm", "run", "start"]